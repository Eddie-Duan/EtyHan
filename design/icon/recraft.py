#!/usr/bin/env python3
"""Recraft 出图客户端（只用标准库）。

为什么要 Python 而不是 PowerShell：
  这台机器的 Schannel 拿不到凭据（curl/.NET 都报 SEC_E_NO_CREDENTIALS），
  走本地代理时 TLS 握手会失败；CPython 用 OpenSSL，走同一个代理正常。

令牌来源（优先级）：--token > 环境变量 RECRAFT_API_TOKEN > design/icon/.env > 仓库根 .env
代理来源：--proxy > 环境变量 HTTPS_PROXY > .env 里的 RECRAFT_PROXY > 不走代理

用法：
  python design/icon/recraft.py whoami
  python design/icon/recraft.py generate --set concepts --id C1,C3,C8 --dry-run
  python design/icon/recraft.py generate --set concepts --id C1,C3,C8
  python design/icon/recraft.py generate --set lockup
  python design/icon/recraft.py i2i --image design/icon/reference/mark-1024.png --prompt "..." --strength 0.45
"""
from __future__ import annotations

import argparse
import base64
import json
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
BASE = "https://external.api.recraft.ai/v1"


def load_dotenv(*paths: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    for p in paths:
        if not p.is_file():
            continue
        for raw in p.read_text(encoding="utf-8", errors="replace").splitlines():
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            k = k.strip()
            v = v.strip().strip('"').strip("'")
            if k and k not in out:
                out[k] = v
    return out


ENV = load_dotenv(HERE / ".env", ROOT / ".env")


def pick(name: str, default: str = "") -> str:
    return os.environ.get(name) or ENV.get(name) or default


def make_opener(proxy: str) -> urllib.request.OpenerDirector:
    handlers: list = []
    if proxy:
        handlers.append(urllib.request.ProxyHandler({"http": proxy, "https": proxy}))
    else:
        handlers.append(urllib.request.ProxyHandler({}))
    return urllib.request.build_opener(*handlers)


def request_json(opener, path: str, token: str, body: dict | None = None) -> dict:
    url = f"{BASE}/{path}"
    data = None
    headers = {"Authorization": f"Bearer {token}", "User-Agent": "etyhan-icon/1.0"}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"
    req = urllib.request.Request(url, data=data, headers=headers, method="POST" if body else "GET")
    try:
        with opener.open(req, timeout=180) as r:
            return json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "replace")[:800]
        raise SystemExit(f"{e.code} {path}: {detail}") from None


def download(opener, url: str, dest: Path) -> int:
    with opener.open(url, timeout=180) as r:
        data = r.read()
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(data)
    return len(data)


def main() -> int:
    ap = argparse.ArgumentParser(description="Recraft client for EtyHan icon work")
    ap.add_argument("action", choices=["whoami", "generate", "i2i"])
    ap.add_argument("--set", default="concepts")
    ap.add_argument("--id", default="")
    ap.add_argument("--token", default="")
    ap.add_argument("--proxy", default="")
    ap.add_argument("--out", default="")
    ap.add_argument("--image", default="")
    ap.add_argument("--prompt", default="")
    ap.add_argument("--strength", type=float, default=0.5)
    ap.add_argument("--model", default="")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()

    proxy = a.proxy or pick("HTTPS_PROXY") or pick("RECRAFT_PROXY")
    token = a.token or pick("RECRAFT_API_TOKEN")
    if not token and not a.dry_run:
        raise SystemExit(f"missing token: fill RECRAFT_API_TOKEN in {HERE / '.env'}")

    opener = make_opener(proxy)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    out_root = Path(a.out or pick("RECRAFT_OUT", str(HERE / "out")))

    if a.action == "whoami":
        me = request_json(opener, "users/me", token)
        credits = me.get("credits")
        print(json.dumps(me, ensure_ascii=False, indent=2))
        if isinstance(credits, int):
            print(f"balance: {credits} units = ${credits / 1000:.2f}")
        return 0

    if a.action == "i2i":
        if not a.image or not a.prompt:
            raise SystemExit("i2i needs --image and --prompt")
        img = Path(a.image)
        payload = {
            "image_url": "data:image/png;base64," + base64.b64encode(img.read_bytes()).decode("ascii"),
            "prompt": a.prompt,
            "strength": a.strength,
            "model": a.model or pick("RECRAFT_MODEL", "recraftv4_1"),
            "n": 1,
            "image_format": "png",
        }
        if a.dry_run:
            print(f"[dry-run] imageToImage strength={a.strength} image={img} prompt={a.prompt}")
            return 0
        resp = request_json(opener, "images/imageToImage", token, payload)
        d = out_root / stamp
        files = []
        for i, item in enumerate(resp.get("data", []), 1):
            name = f"i2i-{i}.png"
            size = download(opener, item["url"], d / name)
            files.append({"file": str(d / name), "bytes": size, "image_id": item.get("image_id")})
        (d / "manifest.json").write_text(
            json.dumps({"kind": "i2i", "prompt": a.prompt, "strength": a.strength,
                        "credits": resp.get("credits"), "files": files},
                       ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"credits={resp.get('credits')} dir={d}")
        return 0

    spec = json.loads((HERE / "prompts.json").read_text(encoding="utf-8"))
    items = spec["sets"].get(a.set)
    if not items:
        raise SystemExit(f"unknown set '{a.set}'; have: {list(spec['sets'])}")
    if a.id:
        wanted = {s.strip() for s in a.id.split(",") if s.strip()}
        items = [it for it in items if it["id"] in wanted]
        if not items:
            raise SystemExit(f"no items match --id {a.id}")

    out_dir = out_root / stamp
    if not a.dry_run:
        out_dir.mkdir(parents=True, exist_ok=True)
    manifest, total = [], 0

    for it in items:
        model = it.get("model") or pick("RECRAFT_MODEL", "recraftv4_1")
        body = {
            "prompt": it["prompt"],
            "model": model,
            "size": it.get("size", "1:1"),
            "n": int(it.get("n", 1)),
            "image_format": it.get("format", "png"),
        }
        if it.get("seed"):
            body["random_seed"] = int(it["seed"])
        if it.get("colors"):
            controls = {"colors": [{"rgb": c} for c in it["colors"]]}
            if it.get("background"):
                controls["background_color"] = {"rgb": it["background"]}
            body["controls"] = controls

        if a.dry_run:
            print(f"[{it['id']}] {it['title']} model={model} size={body['size']} n={body['n']}")
            continue

        print(f"-> {it['id']} {it['title']} ({model} {body['size']})", flush=True)
        try:
            resp = request_json(opener, "images/generations", token, body)
        except SystemExit as e:
            print(f"   FAILED {e}", file=sys.stderr)
            manifest.append({"id": it["id"], "title": it["title"], "ok": False, "error": str(e)})
            continue

        ext = ".svg" if "vector" in model else f".{body['image_format']}"
        n = len(resp.get("data", []))
        files = []
        for i, item in enumerate(resp.get("data", []), 1):
            name = f"{it['id']}{ext}" if n == 1 else f"{it['id']}-{i}{ext}"
            size = download(opener, item["url"], out_dir / name)
            files.append({"file": str(out_dir / name), "bytes": size, "image_id": item.get("image_id")})
        total += int(resp.get("credits") or 0)
        manifest.append({**{k: it[k] for k in ("id", "title", "prompt") if k in it},
                         "model": model, "size": body["size"], "credits": resp.get("credits"),
                         "ok": True, "files": files})
        time.sleep(0.25)

    if a.dry_run:
        return 0
    (out_dir / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\n{len(manifest)} items, credits={total}\ndir: {out_dir}")
    for m in manifest:
        for f in m.get("files", []):
            print(f["file"])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

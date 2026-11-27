"""把首页图的底色归一化成纸色，去掉拼贴残留。

原图（用户提供）左上 EtyHan 底下和两个 한 字形后面各贴了一块底色略有差异的矩形
（242,238,229 vs 纸色 245,241,230），放大看有接缝；JPEG 噪声也让大片平色发花。

做法：把"浅色且低饱和"的像素统一刷成纸色，字形（深色 / 高饱和朱砂）与描边保留。
不碰几何、不改颜色，只让背景变干净。

  python design/banner/flatten-bg.py \
      --src design/banner/source/etyhan-hero-original.jpg \
      --out docs/assets/readme-hero.png
"""
import argparse
from pathlib import Path

import numpy as np
from PIL import Image

PAPER = (245, 241, 230)


def flatten(src: Path, out: Path, paper=PAPER,
            lum_min: float = 232.0, sat_max: int = 16) -> tuple[int, int]:
    im = Image.open(src).convert("RGB")
    a = np.asarray(im).astype(np.int16)
    lum = 0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]
    sat = a.max(axis=2) - a.min(axis=2)
    mask = (lum > lum_min) & (sat < sat_max)
    a[mask] = paper
    out.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(a.astype(np.uint8), "RGB").save(out)
    return int(mask.sum()), mask.size


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", default="design/banner/source/etyhan-hero-original.jpg")
    ap.add_argument("--out", default="docs/assets/readme-hero.png")
    a = ap.parse_args()
    hit, total = flatten(Path(a.src), Path(a.out))
    print(f"{a.out}  {hit}/{total} px flattened")

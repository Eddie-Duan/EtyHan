"""从 mark.py 的同一套几何生成 Android VectorDrawable + 多尺寸 PNG。

产物（写到 design/icon/android/）：
  ic_brand_mark.xml        72dp 视口，字形撑满——界面左上角 28-32dp 用
  ic_brand_seal.xml        108dp 视口，完整印章——启动页 / 商店页 / 关于页
  ic_launcher_foreground.xml  108dp 视口，字形缩到自适应图标安全区
  png/<name>-<size>.png    48..512 的位图，给旧版 mipmap 与预览用
"""
from pathlib import Path

import mark as M

OUT = Path(__file__).parent / "android"


def fmt(v: float) -> str:
    return f"{v:.2f}".rstrip("0").rstrip(".")


def path_d(points, t) -> str:
    xs = [t(p) for p in points]
    d = f"M{fmt(xs[0][0])},{fmt(xs[0][1])}"
    for p in xs[1:]:
        d += f" L{fmt(p[0])},{fmt(p[1])}"
    return d


def ring_d(t) -> str:
    cx, cy, r = M.RING_CX, M.RING_CY, M.RING_R
    cx, cy = t((cx, cy))
    return (f"M{fmt(cx - r)},{fmt(cy)} a{fmt(r)},{fmt(r)} 0 1,0 {fmt(2 * r)},0 "
            f"a{fmt(r)},{fmt(r)} 0 1,0 {fmt(-2 * r)},0")


def build(name: str, viewport: float, zoom: float, shift, frame: bool) -> str:
    t = M.transform(zoom, shift)
    parts = []
    if frame:
        edges, corners = M.frame_parts()
        for a, b in edges:
            parts.append((path_d([a, b], t), M.FRAME_SW * zoom))
        for poly in corners:
            parts.append((path_d(poly, t), M.CORNER_SW * zoom))
    for a, b in M.bar_lines():
        parts.append((path_d([a, b], t), M.BAR_SW * zoom))
    parts.append((ring_d(t), M.RING_SW * zoom))

    paths = "\n".join(
        f'    <path android:pathData="{d}"\n'
        f'        android:strokeColor="{M.SEAL_HEX}" android:strokeWidth="{fmt(sw)}"\n'
        f'        android:strokeLineCap="round" android:strokeLineJoin="round"\n'
        f'        android:fillColor="#00000000" />'
        for d, sw in parts)
    return (f'<?xml version="1.0" encoding="utf-8"?>\n'
            f'<!-- {name} -->\n'
            f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{fmt(viewport)}dp" android:height="{fmt(viewport)}dp"\n'
            f'    android:viewportWidth="{fmt(viewport)}" android:viewportHeight="{fmt(viewport)}">\n'
            f'{paths}\n</vector>\n')


SPECS = [
    # name, viewport, zoom, shift, frame
    ("ic_brand_mark", 72, 1.0, (-18.0, -23.0), False),
    ("ic_brand_seal", 108, 1.0, (0.0, 0.0), True),
    ("ic_launcher_foreground", 108, 1.0, (0.0, 0.0), False),
]

if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    for name, vp, zoom, shift, frame in SPECS:
        xml = build(name, vp, zoom, shift, frame)
        (OUT / f"{name}.xml").write_text(xml, encoding="utf-8")
        for size in (48, 72, 96, 144, 192, 512):
            png = OUT / "png" / f"{name}-{size}.png"
            png.parent.mkdir(parents=True, exist_ok=True)
            # 位图版：mark.py 只认 108 网格，这里按 zoom/shift 反推
            img = M.render_for(vp, zoom, shift, frame, size) if hasattr(M, "render_for") else None
            if img is not None:
                img.save(png)
        print("wrote", name)

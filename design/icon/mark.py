"""EtyHan 品牌标记的几何母版。

108x108 网格；所有形状都在 grid 坐标里定义，再由 transform() 缩放/平移，
Pillow 渲染和 Android VectorDrawable 共用同一套数字，避免两边不一致。

  python design/icon/mark.py --out design/icon/out/mark-v3.png
"""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw

SEAL = (140, 29, 24, 255)
PAPER = (246, 241, 231, 255)
SEAL_HEX = "#8C1D18"

# 字形：保持原版 108 网格数值不变（横线 7、圆环 7、圆心 y=72）
BAR_X0, BAR_X1 = 38.0, 70.0
BAR_Y1, BAR_Y2 = 33.0, 47.0
BAR_SW = 7.0
RING_CX, RING_CY, RING_R = 54.0, 72.0, 13.0
RING_SW = 7.0

FRAME_INSET = 5.5
FRAME_SW = 1.7
CORNER_W = 14.0
CORNER_SW = 1.7


def transform(zoom=1.0, shift=(0.0, 0.0)):
    def t(p):
        return ((p[0] - 54.0) * zoom + 54.0 + shift[0],
                (p[1] - 54.0) * zoom + 54.0 + shift[1])
    return t


def bar_lines():
    return [((BAR_X0, BAR_Y1), (BAR_X1, BAR_Y1)), ((BAR_X0, BAR_Y2), (BAR_X1, BAR_Y2))]


def ring():
    return (RING_CX, RING_CY, RING_R)


def kf_points(x, y, sx, sy, w):
    """回纹角：沿两条边进入、向内折三步的方形螺旋（拐子纹）。"""
    t = w / 3.4
    return [
        (x + sx * w, y),
        (x, y),
        (x, y + sy * w),
        (x + sx * t, y + sy * w),
        (x + sx * t, y + sy * t),
        (x + sx * (w - t), y + sy * t),
        (x + sx * (w - t), y + sy * (w - t)),
        (x + sx * 2 * t, y + sy * (w - t)),
        (x + sx * 2 * t, y + sy * 2 * t),
    ]


def frame_parts():
    """返回 (四段边框线, 四角点列)，均为 grid 坐标。"""
    a, b = FRAME_INSET, 108 - FRAME_INSET
    w = CORNER_W
    edges = [((a + w, a), (b - w, a)), ((a + w, b), (b - w, b)),
             ((a, a + w), (a, b - w)), ((b, a + w), (b, b - w))]
    corners = [kf_points(a, a, 1, 1, w), kf_points(b, a, -1, 1, w),
               kf_points(a, b, 1, -1, w), kf_points(b, b, -1, -1, w)]
    return edges, corners


def render_canvas(size: int, viewport: float = 108.0, zoom: float = 1.0,
                  shift=(0.0, 0.0), frame: bool = True, ss: int = 4) -> Image.Image:
    S = size * ss
    img = Image.new("RGBA", (S, S), PAPER)
    d = ImageDraw.Draw(img)
    k = S / viewport
    t = transform(zoom, shift)

    def pts(points):
        return [tuple(c * k for c in t(p)) for p in points]

    if frame:
        edges, corners = frame_parts()
        for a, b in edges:
            d.line(pts([a, b]), fill=SEAL, width=max(1, int(round(FRAME_SW * zoom * k))))
        for poly in corners:
            d.line(pts(poly), fill=SEAL, width=max(1, int(round(CORNER_SW * zoom * k))), joint="curve")

    sw = BAR_SW * zoom * k
    for a, b in bar_lines():
        (x0, y0), (x1, _) = pts([a, b])
        d.line([(x0, y0), (x1, y0)], fill=SEAL, width=int(round(sw)))
        for cx in (x0, x1):
            d.ellipse([cx - sw / 2, y0 - sw / 2, cx + sw / 2, y0 + sw / 2], fill=SEAL)

    cx, cy, r = ring()
    rs = RING_SW * zoom * k
    cx, cy = t((cx, cy))
    cx, cy, rr = cx * k, cy * k, r * zoom * k
    d.ellipse([cx - rr - rs / 2, cy - rr - rs / 2, cx + rr + rs / 2, cy + rr + rs / 2],
              outline=SEAL, width=int(round(rs)))
    return img.resize((size, size), Image.LANCZOS)


def render(size: int, frame: bool = True, zoom: float = 1.0, ss: int = 4) -> Image.Image:
    return render_canvas(size, 108.0, zoom, (0.0, 0.0), frame, ss)


def render_for(viewport, zoom, shift, frame, size, ss=4):
    return render_canvas(size, viewport, zoom, shift, frame, ss)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="design/icon/out/mark-v3.png")
    ap.add_argument("--size", type=int, default=1024)
    ap.add_argument("--plain", action="store_true")
    a = ap.parse_args()
    out = Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    render(a.size, frame=not a.plain).save(out)
    print(out)

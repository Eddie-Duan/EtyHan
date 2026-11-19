import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from mark import PAPER, render  # noqa: E402

OUT = Path("design/icon/out/preview-mark.png")
big = 620
left = render(big, True)
right = render(big, False)
sizes = [192, 96, 48]
W = 40 + big * 2 + 40
H = 40 + big + 40 + 210 + 40
canvas = Image.new("RGBA", (W, H), PAPER)
canvas.paste(left, (40, 40), left)
canvas.paste(right, (40 + big + 40, 40), right)

d = ImageDraw.Draw(canvas)
try:
    f = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 18)
except Exception:
    f = ImageFont.load_default()

x = 40
y = 40 + big + 30
for s in sizes:
    im = render(s, True)
    canvas.paste(im, (x, y), im)
    d.text((x, y + s + 6), f"{s}px seal", fill=(27, 26, 23, 255), font=f)
    x += s + 30
x += 40
for s in sizes:
    im = render(s, False)
    canvas.paste(im, (x, y), im)
    d.text((x, y + s + 6), f"{s}px plain", fill=(27, 26, 23, 255), font=f)
    x += s + 30

OUT.parent.mkdir(parents=True, exist_ok=True)
canvas.save(OUT)
print(OUT, canvas.size)

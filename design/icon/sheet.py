import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

args = sys.argv[1:]
out = Path(args[0])
files = [Path(p) for p in args[1:]]

COLS = int(sys.argv[sys.argv.index("--cols") + 1]) if "--cols" in args else 3
files = [f for f in files if f.is_file()]
if not files:
    raise SystemExit("no input images")

THUMB_W = 460
GAP = 18
PAD = 18
BG = (246, 241, 231)
INK = (27, 26, 23)

try:
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 20)
except Exception:
    font = ImageFont.load_default()

thumbs = []
for f in files:
    im = Image.open(f).convert("RGBA")
    ratio = THUMB_W / im.width
    im = im.resize((THUMB_W, max(1, int(im.height * ratio))), Image.LANCZOS)
    thumbs.append((f, im))

rows = (len(thumbs) + COLS - 1) // COLS
cell_h = max(im.height for _, im in thumbs) + 30
W = PAD * 2 + COLS * THUMB_W + (COLS - 1) * GAP
H = PAD * 2 + rows * cell_h + (rows - 1) * GAP

sheet = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(sheet)
for i, (f, im) in enumerate(thumbs):
    r, c = divmod(i, COLS)
    x = PAD + c * (THUMB_W + GAP)
    y = PAD + r * (cell_h + GAP)
    sheet.paste(im, (x, y), im)
    d.text((x, y + im.height + 5), f.stem, fill=INK, font=font)

out.parent.mkdir(parents=True, exist_ok=True)
sheet.save(out)
print(f"{out} {sheet.width}x{sheet.height}")

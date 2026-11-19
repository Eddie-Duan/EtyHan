"""交付预览：launcher 三档尺寸（含圆形遮罩）、印章版、界面左上角实际大小。"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
import mark as M  # noqa: E402

OUT = Path("design/icon/out/preview-final.png")
W, H = 1240, 620
PAPER = (246, 241, 231, 255)
INK = (27, 26, 23, 255)
MUTED = (108, 102, 92, 255)

canvas = Image.new("RGBA", (W, H), PAPER)
d = ImageDraw.Draw(canvas)
try:
    f = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 17)
    fb = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 24)
except Exception:
    f = fb = ImageFont.load_default()


def label(x, y, text, font=f, fill=MUTED):
    d.text((x, y), text, font=font, fill=fill)


def circle_mask(im):
    size = im.width
    mask = Image.new("L", (size * 3, size * 3), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size * 3 - 1, size * 3 - 1], fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    out = im.copy()
    out.putalpha(mask)
    return out


# 1) launcher 三档，底部对齐
label(40, 26, "launcher（纸底 + 原样字形，落在自适应图标安全区内）")
base = 250
x = 40
for s in (192, 96, 48):
    im = M.render(s, frame=False)
    canvas.paste(im, (x, base - s), im)
    label(x, base + 8, f"{s}px")
    x += s + 40

# 2) 圆形遮罩检验（96px 一档足够）
cm = circle_mask(M.render(96, frame=False))
cx = 520
label(cx, 26, "圆形遮罩下不会切到字形")
canvas.paste(cm, (cx, 60), cm)
label(cx, 164, "96px / round mask")

# 3) 界面左上角实际大小
label(40, 320, "界面左上角（28 / 32 / 40dp，与标题同排）")
x = 40
for size in (28, 32, 40):
    im = M.render_for(72, 1.0, (-18.0, -23.0), False, size * 3).resize((size, size), Image.LANCZOS)
    canvas.paste(im, (x, 396 - size), im)
    label(x, 404, f"{size}dp")
    x += 78
label(x + 6, 350, "EtyHan", font=fb, fill=INK)

# 4) 印章版
sx, sy = 760, 26
label(sx, sy, "印章版（启动页 / 商店页 / 关于页）")
seal = M.render(400, frame=True)
canvas.paste(seal, (sx, 60), seal)
label(sx, 470, "字形原样，只在外圈加四角回纹与边框")

OUT.parent.mkdir(parents=True, exist_ok=True)
canvas.save(OUT)
print(OUT, canvas.size)

"""主页左上角头部模拟（3x 像素密度），用于在真机之前核对视觉。"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
import mark as M  # noqa: E402

S = 3  # 3x
W, H = 1240, 230
PAPER = (246, 241, 231, 255)
SEAL = (140, 29, 24, 255)
CREAM = (255, 248, 241, 255)
INK = (27, 26, 23, 255)
MUTED = (91, 84, 72, 255)

canvas = Image.new("RGBA", (W, H), PAPER)
d = ImageDraw.Draw(canvas)

# 42dp 圆角正方形，半径 14dp
tile = 42 * S
x0, y0 = 56, (H - tile) // 2
d.rounded_rectangle([x0, y0, x0 + tile, y0 + tile], radius=14 * S, fill=SEAL)

# 图标：0.62 * 42dp，奶白色
mark = M.render_for(72, 1.0, (-18.0, -23.0), False, int(tile * 0.68), bg=(0, 0, 0, 0), fg=CREAM)
canvas.paste(mark, (x0 + (tile - mark.width) // 2, y0 + (tile - mark.height) // 2), mark)

# EtyHan：headlineMedium = Serif SemiBold 25sp
try:
    font = ImageFont.truetype("C:/Windows/Fonts/timesbd.ttf", 25 * S)
except Exception:
    font = ImageFont.load_default()
tx = x0 + tile + 12 * S
bbox = d.textbbox((0, 0), "EtyHan", font=font)
d.text((tx, (H - (bbox[3] - bbox[1])) // 2 - bbox[1]), "EtyHan", font=font, fill=INK)

# 右侧 ProviderPill 占位
try:
    small = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 12 * S)
except Exception:
    small = ImageFont.load_default()
pill = "deepseek · deepseek-chat"
pb = d.textbbox((0, 0), pill, font=small)
pw, ph = pb[2] - pb[0] + 24 * S, pb[3] - pb[1] + 12 * S
px, py = W - pw - 40, (H - ph) // 2
d.rounded_rectangle([px, py, px + pw, py + ph], radius=ph // 2, outline=(201, 191, 174, 255), width=2 * S)
d.text((px + 12 * S, py + 6 * S - pb[1]), pill, font=small, fill=MUTED)

out = Path("design/icon/out/preview-header.png")
canvas.save(out)
print(out, canvas.size)

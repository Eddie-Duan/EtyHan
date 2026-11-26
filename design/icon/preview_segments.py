"""结果页「分段」卡片的配色模拟（与 Compose 里的取值一致）。"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
import mark as M  # noqa: E402
from mark import PAPER  # noqa: E402

# 与 Source.kt / theme 一致的取色
SINO = ((0xF3, 0xDE, 0xD7), (0x7E, 0x1B, 0x16))
NATIVE = ((0xDD, 0xEB, 0xDE), (0x2C, 0x5A, 0x41))
ROLE = ((0xED, 0xE3, 0xF7), (0x57, 0x3A, 0x7C))
BASE = ((0xDC, 0xEA, 0xE9), (0x1F, 0x55, 0x54))
NOTE = ((0xEC, 0xE9, 0xE1), (0x6B, 0x62, 0x52))
NEUTRAL = ((0xED, 0xE6, 0xD8), (0x5B, 0x54, 0x48))
INK = (0x1B, 0x1A, 0x17)
MUTED = (0x5B, 0x54, 0x48)
RED = (0x8C, 0x1D, 0x18)

S = 2  # 2x
W, H = 1180, 980
img = Image.new("RGB", (W, H), PAPER[:3])
d = ImageDraw.Draw(img)


def font(name, size):
    try:
        return ImageFont.truetype(f"C:/Windows/Fonts/{name}", size)
    except Exception:
        return ImageFont.load_default()


f_kr = font("malgun.ttf", 30 * S)
f_rom = font("segoeui.ttf", 15 * S)
f_pill = font("msyh.ttc", 13 * S)
f_zh = font("msyh.ttc", 19 * S)
f_hanja = font("simsun.ttc", 19 * S)


def pill(x, y, text, colors, f=None, pad=(9, 3), radius=6):
    f = f or f_pill
    tw = d.textlength(text, font=f)
    th = f.size
    box = [x, y, x + tw + pad[0] * 2 * S, y + th + pad[1] * 2 * S]
    d.rounded_rectangle(box, radius=radius * S, fill=colors[0], outline=colors[1], width=max(1, S // 2))
    d.text((x + pad[0] * S, y + pad[1] * S - 2), text, font=f, fill=colors[1])
    return box[2]


def row(y, ko, rom, pos, origin, hanja, zh, role, base=None, note=None, origin_colors=SINO):
    x = 60
    d.text((x, y), ko, font=f_kr, fill=INK)
    x += d.textlength(ko, font=f_kr) + 14 * S
    d.text((x, y + 6 * S), rom, font=f_rom, fill=MUTED)
    x += d.textlength(rom, font=f_rom) + 12 * S
    x = pill(x, y + 2 * S, pos, NEUTRAL) + 6 * S
    pill(x, y + 2 * S, origin, origin_colors)

    y2 = y + 46 * S
    d.rounded_rectangle([60, y2, 60 + d.textlength(hanja, font=f_hanja) + 18 * S, y2 + 30 * S],
                        radius=8 * S, fill=origin_colors[0], outline=origin_colors[1], width=max(1, S // 2))
    d.text((69, y2 + 4 * S), hanja, font=f_hanja, fill=origin_colors[1])

    y3 = y2 + 38 * S
    d.text((60, y3), zh, font=f_zh, fill=INK)

    y4 = y3 + 34 * S
    x = 60
    x = pill(x, y4, role[0], ROLE) + 8 * S
    if base:
        x = pill(x, y4, base, BASE) + 8 * S
    if note:
        pill(x, y4, note, NOTE)
    return y4 + 40 * S


y = 40
y = row(y, "한국어", "han-gugeo", "名词", "漢", "韓國語", "韩语", ("词干",), None, None)
d.line([60, y, W - 60, y], fill=(0xE0, 0xD7, 0xC7), width=S)
y = row(y + 16, "를", "reul", "助词", "", "賓格助詞", "宾格助词", None) if False else y
# 를：无汉字，单独画一行
y += 16
x = 60
d.text((x, y), "를", font=f_kr, fill=INK)
x += d.textlength("를", font=f_kr) + 14 * S
d.text((x, y + 6 * S), "reul", font=f_rom, fill=MUTED)
x += d.textlength("reul", font=f_rom) + 12 * S
pill(x, y + 2 * S, "助词", NEUTRAL)
y += 46 * S
d.text((60, y), "宾格助词", font=f_zh, fill=INK)
y += 34 * S
pill(60, y, "宾格助词", ROLE)
y += 46 * S
d.line([60, y, W - 60, y], fill=(0xE0, 0xD7, 0xC7), width=S)

y += 16
x = 60
d.text((x, y), "할", font=f_kr, fill=INK)
x += d.textlength("할", font=f_kr) + 14 * S
d.text((x, y + 6 * S), "hal", font=f_rom, fill=MUTED)
x += d.textlength("hal", font=f_rom) + 12 * S
x = pill(x, y + 2 * S, "动词", NEUTRAL) + 6 * S
pill(x, y + 2 * S, "本土", NATIVE)
y += 46 * S
d.text((60, y), "会做", font=f_zh, fill=INK)
y += 34 * S
x = 60
x = pill(x, y, "冠形语尾", ROLE) + 8 * S
x = pill(x, y, "原形 하다", BASE) + 8 * S
y += 42 * S
pill(60, y, "ㄴ 冠形", NOTE)

out = Path("design/icon/out/preview-segments.png")
img.save(out)
print(out, img.size)

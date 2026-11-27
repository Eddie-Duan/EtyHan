# EtyHan 首页图（README hero）

**最终采用**：用户在 2026-11-26 提供的 3072×1024 手绘稿（`source/etyhan-hero-original.jpg`），
`flatten-bg.py` 归一化底色后输出 `docs/assets/readme-hero.png`，README 引用的就是它。
它把两个手写的 `한`、`漢 / 韓` 和右侧放大的 `ㅎ` 印章放在同一张图里，比下面三个备选更活泼。

下面 A/B/C 是同一主题的另外三个方向（1600×800，2:1），源文件是 HTML，出图走浏览器截图。

## 文件

| 文件 | 作用 |
|---|---|
| `source/etyhan-hero-original.jpg` | 用户提供的原图，3072×1024 JPEG |
| `flatten-bg.py` | 把底色刷成纸色、去掉拼贴接缝，输出 `docs/assets/readme-hero.png` |
| `banner.css` | A/B/C 的全部样式；纸 / 墨 / 朱砂 取自 App 主题 `ui/theme/Theme.kt` |
| `render.html` | A/B/C 的 markup；`?opt=a\|b\|c` 只留一个，便于逐张截图 |
| `out/` | 出图：`a-editorial.png` / `b-dictionary.png` / `c-converge.png` |

### 底色归一化

原图左上 `EtyHan` 底下、两个 `한` 字形后面各贴了一块底色不同的矩形
（242,238,229 vs 纸色 245,241,230），放大有接缝。`flatten-bg.py` 把"浅色且低饱和"的
像素统一刷成纸色，字形和朱砂不动：

```powershell
& $py design/banner/flatten-bg.py `
    --src design/banner/source/etyhan-hero-original.jpg `
    --out docs/assets/readme-hero.png
```

约 255 万 / 314 万像素被刷平，输出 384 KB。**没有**重绘任何字形。

## 出图（A/B/C 备选）

这台机器上 `chrome --headless` 起不来（沙箱禁止创建命名管道，mojo 初始化即失败），
所以用 ego 浏览器的 CDP 截图：设 `Emulation.setDeviceMetricsOverride` 为 1600×800，
隐藏 `#ego-agent-cursor-overlay`，等 `document.fonts.ready` 后 `page.screenshot()`。
`deviceScaleFactor: 2` 在当前 ego 版本下不放大输出，最终就是 1600×800。

想一次看三张：直接打开 `render.html`（不带 `?opt`），三张竖直排开。

## 三个备选方向

**A · 一字两面** —— `한 = 漢 = 韓` 一行大字，上面挂 `ㅎ ㅏ ㄴ` 的拆字标注，
下方中韩双栏文案。最克制，缩到 880px 宽仍可读。

**B · 拆字** —— 字典页：朱砂回纹边框 + `ㅎ / ㅏ / ㄴ`（标 초성·중성·종성）合成 `한`，
再用树线分到 `漢`（漢字·汉字）和 `韓`（韓國·韩国）。信息量最大，小字在 880px 下会偏细，
适合放仓库社交预览图或发布页。

**C · 归字** —— `漢 → 한 ← 韓`，中间的 `한` 套一个朱砂细圆（呼应标记里的 ○）。

## 文案

主句（两条都能独立用）：

- 中文：**「汉」与「韩」，在韩语里都写作 `한`。**
- 韩文：**漢도 韓도 ‘한’ — ㅎ에서 시작하는 같은 글자.**

备选：

| # | 中文 | 韩文 |
|---|---|---|
| 1 | 一个 `ㅎ`，写出「汉」，也写出「韩」。 | 같은 ㅎ, 다른 한자, 같은 소리 ‘한’. |
| 2 | 汉字词是韩语里的中文底片，一个字一个字显影。 | 漢字語는 한국어 속의 한자 필름, 한 글자씩. |
| 3 | 从 `ㅎ` 起笔，读回汉字的来路。 | ㅎ에서 시작해 漢의 뿌리까지. |

功能句（放在主句下面，中韩各一条）：

- 中文：译文 / 语法分段 / 词源，一次给全。
- 韩文：번역 · 문법 분석 · 어원까지, 단 한 번에.

## 事实核对

`漢` 和 `韓` 在韩语里都读 **한**（漢字 한자、漢江 한강；韓國 한국、大韓 대한），
所以 `한 = 漢 = 韓` 是成立的，不是文字游戏。`한` 的构成也确实是人称的
초성 `ㅎ` + 중성 `ㅏ` + 종성 `ㄴ`。

## 字体

这台机器没有韩文衬线（无 Batang / Nanum Myeongjo），所以：

- 汉字：`Noto Serif SC`（宋体感）
- 韩文：`Malgun Gothic`（맑은 고딕）
- 拉丁：`Georgia`

`Noto Serif SC` 的 cmap 里没有 Hangul，浏览器会按字符回退到 Malgun，
所以同一行里汉字是衬线、韩文是黑体——这是被动结果，也是刻意的分层。
换到有 `Source Han Serif K` 的机器上，可以把 `.kr` 改成它，`한` 会更接近衬线调性。

# EtyHan 图标 / 品牌标识

现有字形：把 `ㅎ` 拆成「两条等长横线 + 一个圆环」，同时读作 `h`、`=` 和 `○`。
这一轮做两件事：用 Recraft 找纹样语言，再把字形和纹样重新画成可交付的矢量。

## 文件

| 文件 | 作用 |
|---|---|
| `mark.py` | **几何母版**。108 网格里定义字形与回纹角，Pillow 渲染多尺寸 PNG |
| `emit.py` | 从同一套数字生成 Android VectorDrawable + 多尺寸 PNG |
| `preview.py` / `preview2.py` | 预览拼图（大图、小尺寸、左上角实际大小、与原版对照） |
| `recraft.py` | Recraft API 客户端（概念图 / 纹样参考 / 图生图） |
| `prompts.json` | 变体清单：`concepts` / `lockup` / `vector` / `pattern` |
| `.env` | 令牌与代理（已被 `.gitignore` 忽略） |
| `out/` | 出图结果、预览图、`manifest.json` |
| `android/` | 可直接拷进 `res/drawable/` 的三个 VectorDrawable |

## 出图（Recraft）

```powershell
$py = "C:\Users\DUANboyang\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
& $py design/icon/recraft.py whoami                       # 查余额，不花 credits
& $py design/icon/recraft.py generate --set pattern        # 四种纹样参考
& $py design/icon/recraft.py generate --set concepts --id C1,C3,C8
& $py design/icon/recraft.py generate --set lockup         # 2:1 横版
& $py design/icon/recraft.py generate --set vector --id V2 # SVG 终稿
```

`-DryRun` 只打印请求体，不发请求。

> 这台机器上 PowerShell 的 Schannel 拿不到凭据（`SEC_E_NO_CREDENTIALS`），curl 和 .NET 走本地代理都会 TLS 失败；CPython 用 OpenSSL，所以客户端是 Python。`.env` 里的 `RECRAFT_PROXY=http://127.0.0.1:7897` 指向本机代理。

## 生成资源

```powershell
& $py design/icon/emit.py     # 写 design/icon/android/*.xml 与 png/
& $py design/icon/preview2.py # 拼预览图
```

三个产物：

| 文件 | 视口 | 用途 |
|---|---|---|
| `ic_brand_mark.xml` | 72dp，字形撑满 | 界面左上角 28–32dp |
| `ic_brand_seal.xml` | 108dp，完整印章 + 回纹角 | 启动页 / 商店页 / 关于页 |
| `ic_launcher_foreground.xml` | 108dp，字形缩到 0.92 | 自适应图标前景 |

launcher 不用印章边框，是因为自适应图标会把 108dp 画布外圈 18dp 裁掉，方框边和回纹角必被圆/方角遮罩切掉；所以 launcher 只放字形，印章留给不裁切的场景。

## 与旧版相比改了什么

1. **重心**：旧版字形包围盒中心在 y≈59，画布中心 54，整体偏下；新版按 54 居中并留 1.2 单位光学上移。
2. **笔画**：圆环与横线同为 7 时，圆环因封闭双曲率显重；新版圆环 5.9、横线 7。
3. **留白**：新版横线到圆环的间隙与两横线之间一致（各 ≈7）。
4. **纹样**：四角回纹角（拐子纹）与边框首尾相接，在 96dp 以上可读。

## 成本

`recraftv4_1` $0.035/张，`recraftv4_1_vector` $0.08/张（1000 units = $1）。
本轮已用：概念 3 张 + 横版 2 张 + 纹样 4 张 = 315 units。

# EtyHan

![EtyHan 首页图：汉字「漢」与「韓」在韩语里都写作「한」，同一个字母 ㅎ 起笔](docs/assets/readme-hero.png)

> **「汉」与「韩」，在韩语里都写作 `한`。**
> **漢도 韓도 ‘한’ — ㅎ에서 시작하는 같은 글자.**

给中文母语者的韩语精读工具：一次查询，拿到**译文 + 逐词语法分段 + 语法讲解 + 词汇词源（汉字词逐字对照）+ 例句**，并自动沉淀成可离线的个人词库。

Android 原生（Kotlin + Jetpack Compose），模型 API 由用户自己填：DeepSeek、智谱 GLM、硅基流动、阿里百炼、Kimi、OpenRouter、OpenAI、Gemini、本机 Ollama，或任何 OpenAI 兼容服务。

> 为什么做词源：韩语词汇约六成是汉字词（학생=學生、도서관=圖書館），中文母语者真正需要的不是"再背一遍单词"，而是**汉字音对应规律**和**同形异义陷阱**（집 是"房子"不是"集"）。这是现有韩语词典 App 普遍缺的一层。
>
> 判定只看**词源**，不看中文今义：친구 是 親舊（今义"朋友"，字面"亲戚旧友"），사과 是 沙果（中文指海棠果，韩语泛指苹果），신문 是 新聞（韩语"报纸"）——这些都会标成"汉"，并同时给出字面义与今义。

---

## 界面与流程

```
查询页                 结果页（四个页签）
┌──────────────┐       ┌──────────────────────────┐
│ EtyHan       │       │ ←        ★   ↗   ⋮      │
│ [输入韩语…]   │       │ 학생들은 학교에 갑니다     │
│ [分析]        │  →    │ 译文 / 语感               │
│ 最近查询…     │       │ 给中文母语者的提示         │
└──────────────┘       │ 分段│语法│词源│例句        │
                       └──────────────────────────┘
```

- **分段**：按"语法最小可解释单位"切开（词干 / 助词 / 语尾），标词性、语法角色、原形、汉字。罗马音按**实际读音**写，音变 / 特殊读音标红（ㅂ→p、紧音化 kk、의→e）。**点任一成分可单独查它的词源**。
- **语法**：句子成分公式、句型、时制与敬语层级、逐条语法点 + 中文母语者的对照提示。
- **词源**：来源标签统一为 `汉 / 本土 / 英 / 日 / 法 / 德…`（同样的形状、同样的位置）。标签后按来源接内容：汉→繁体汉字，英→原词，日/法/德等→原词 + 拉丁转写（因为看不懂），本土→不写（朝鲜自造词）。另有词源构成比例条、汉字词**逐字对照**（학→學→学）、韩汉同形词对照表。
- **例句**：带译文和语法点，可朗读。
- 首页/词库/设置三个底部页签；支持系统深色模式；支持从其他 App 分享文本或用"处理文本"直接查词。

## 快速开始

### 1. 装 APK

已构建的调试包：`app/build/outputs/apk/debug/app-debug.apk`（用 `adb install` 或直接传到手机安装）。

自己构建：

```bash
# 需要 JDK 17、Android SDK 35
./gradlew assembleDebug
# 单元测试
./gradlew testDebugUnitTest
```

用 Android Studio 打开本目录也可以直接运行（AGP 8.7.3 / Kotlin 2.0.21 / compileSdk 35 / minSdk 26）。

### 2. 填一个模型 Key

打开 App → 设置 → 选服务 → 填 Key → **测试连接**。推荐顺序：

| 服务 | 模型 | 说明 |
|---|---|---|
| **DeepSeek**（默认） | `deepseek-chat` | 便宜、JSON 稳定，韩语够用 |
| 智谱 GLM | `glm-4-flash` | 有免费额度，适合先试 |
| 硅基流动 | `deepseek-ai/DeepSeek-V3` | 聚合多家模型，有赠送额度 |
| 阿里百炼 | `qwen-plus` | 语法解释细 |
| Kimi | `moonshot-v1-8k` | 中文表达自然 |
| OpenRouter | `deepseek/deepseek-chat` 等 | 一个 Key 用多家（需外币卡） |
| OpenAI | `gpt-4o-mini` | 需能访问 api.openai.com |
| Gemini | `gemini-2.0-flash` | Google 的 OpenAI 兼容端点，有免费额度 |
| 本地 Ollama | `qwen2.5:7b` | 完全离线；模拟器用 `10.0.2.2`，真机填电脑局域网 IP |

> Key 存在应用私有目录（优先 Android Keystore 加密的 `EncryptedSharedPreferences`，失败则退回普通私有文件）。除了你选的模型服务，没有任何第三方服务器参与。

## 省钱 / 省 token 的设计

这是本项目的核心取舍，写在 `PromptBuilder.kt` 与分析模板里：

1. **只让模型填空**：固定 JSON schema + 短键名（`t` / `segs` / `ety`），模型不自由发挥格式，输出短、好解析。
2. **明确禁止思考过程**：系统提示词要求"不要输出推理过程、不要复述任务、只输出 JSON"，并给出**字数预算**（350 / 700 / 1100 汉字三档）。
3. **稳定前缀**：系统消息逐字节固定，输入放在 user 消息末尾 → 命中 DeepSeek / OpenAI 的自动 prefix cache，重复查询的输入费大幅下降。
4. **同词不重查**：查询结果落库，同一文本直接读缓存（结果页可"重新分析"强制刷新）。
5. **三档详细度**：精简 / 标准 / 详细，直接控制例句数、语法点数和词源条目数。
6. **表单可调**：`JSON 模式`开关（不支持的服务会自动去掉 `response_format` 重试一次）；`max_tokens`、温度可在代码里改默认值。

一次典型句子分析：输入约 1k tokens、输出约 600 tokens。按 DeepSeek 现价约 **0.001~0.003 元/次**。

### 为什么是“单次结构化调用”而不是多轮 Agent

“让模型自己规划、调工具、反复核验”听起来更像 Agent，但对这个任务不划算：

- 用户要的四件事（译文 / 分段 / 语法 / 词源）在一份 schema 里可以一次问全，多轮只会重复付输入费；
- 多轮会让延迟从 3~8 秒变成 20 秒以上，查词场景无法接受；
- 结构化单次调用可以被缓存、被重放、被导出，也便于把 Prompt 当成可移植的 “skill” 维护。

如果要做核验型 Agent，正确的位置是**词源二次校验**（拿 hanja 去查汉字音表 / 词典再回填），已列在 Roadmap，而不是让模型自由发挥。

## 分析模板（可移植的 "skill"）

模板不止在 App 里：`docs/etyhan-analysis-skill.md` 是同一份提示词的独立 Markdown 版，可以直接贴到 ChatGPT / Claude / 任何聊天窗口里用，也可以改成你自己的版本。

修改入口：设置 → 附加要求（追加到系统提示词末尾，仍要求返回同一份 JSON）。设置 → **预览 Prompt** 可以看到实际发出的完整内容。

## 项目结构

```
app/src/main/java/com/etyhan/app/
├── ai/
│   ├── PromptBuilder.kt      # 分析模板：schema + 纪律 + 预算（核心资产）
│   ├── LlmClient.kt          # OpenAI 兼容客户端，错误本地化、JSON 模式自动降级
│   ├── AnalysisParser.kt     # 围栏/尾逗号/夹带解释的容错解析 + 归一化
│   └── AnalysisFormatter.kt  # 导出纯文本、词源比例、摘要
├── data/
│   ├── Analysis.kt           # Analysis/Segment/Grammar/Etymology…（@Serializable）
│   ├── AnalysisRepository.kt # 分析主流程 + 缓存 + 词库
│   ├── EtyHanDatabase.kt     # Room：payload 存整份 JSON，字段演进不需要迁移
│   ├── Settings.kt           # DataStore + Keystore 加密的 Key 存储
│   └── Providers.kt          # 各家服务的快捷方式
├── ui/
│   ├── lookup/  result/  bank/  settings/   # 四个页面 + ViewModel
│   └── components/ theme/                   # 纸·墨·朱砂配色与卡片
└── tts/Speaker.kt            # 系统韩语 TTS（离线）
```

## 与 Soju Platform 的关系

结构与功能组织参考了 [Soju Platform](https://github.com/orbingol/soju-platform)（MIT）：词汇/语法/例句/朗读的分区方式。EtyHan 把"词表浏览 + 练习 + 聊天"收敛为**单次分析 + 卡片呈现**，并新增了词源层（汉字逐字对照、同形异义提示）——这是面向中文母语者的差异点。

## 已验证

在本机（Windows / JDK 17 / Android SDK 35 / Gradle 8.9 / AGP 8.7.3）实际跑过：

- `assembleDebug` → BUILD SUCCESSFUL，产物 `app/build/outputs/apk/debug/app-debug.apk`（约 19 MB）
- `testDebugUnitTest` → **31 个测试全部通过**：
  - JSON 容错（代码围栏、夹带解释、字符串里的花括号、尾逗号、空字段归一化）
  - 分析模板稳定性（前缀不变、预算随档位变化、`romHi`/`lang` 规则存在）
  - 罗马音标红（多字母片段、单字母片段、片段不存在时的降级）
  - 来源标签与内容（汉/英/日/本土 同屏渲染，日文带拉丁转写）
  - 汉字词按词源标注（親舊 显示「字面：亲戚旧友 / 今义：朋友」）
  - LLM 客户端（MockWebServer：请求体、鉴权头、401 文案、`response_format` 被拒后自动重试、只有思考内容时的报错）
  - Robolectric 启动冒烟：Activity 起得来、首页渲染、底部导航能到「词库 / 设置」（Room + DataStore + 导航都是真跑）
- `lintDebug` → **0 errors**

**没做**：真机/模拟器上的手工点按与视觉走查（构建环境没有连接设备、也没有装模拟器）。第一次在你的手机上跑，如果有布局或 TTS 的机型问题，请把 logcat 发我。

## 已知限制

- 分析质量取决于你选的模型；小模型可能在汉字词 `hanja` 上出错，结果页的"查看 JSON"可以看到模型原始结构。
- 只做韩→中。汉→韩、TOPIK 题库、间隔重复复习尚未实现（见 Roadmap）。
- 无内置词典数据：词库靠你自己的查询积累。冷启动时 AI 是唯一来源。

## Roadmap

- [ ] 汉字音对应表（학↔學 的规则库）+ 汉字词家族树
- [ ] 生词本间隔重复（SRS）与 TOPIK 分级筛选
- [ ] 拍照/OCR 取词
- [ ] 汉→韩反向分析（中文词映射到韩语汉字词）
- [ ] 导出 Anki CSV / Markdown

## 许可

MIT。见 [LICENSE](LICENSE)。


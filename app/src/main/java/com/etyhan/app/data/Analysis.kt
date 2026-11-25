package com.etyhan.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 一次分析的完整内容。
 *
 * 字段名刻意用 1~5 个字母的短键（`t` / `segs` / `ety` …），因为这份 JSON 完全由大模型生成：
 * 键名越短，输出越省 token，模型也越不容易写歪。展示层再翻译成中文标签。
 *
 * 这是「AI 填空」的表格：PromptBuilder 把 schema 交给模型，模型只负责填值。
 */
@Serializable
data class Analysis(
    val v: Int = 1,
    /** word / phrase / sentence */
    val kind: String = "word",
    /** 中文译文（单词则为释义）。按中文语序重写，不是逐段拼接 */
    @SerialName("t") val translation: String = "",
    /** 同形词 / 多义项候选；只有一个义项时为空 */
    @SerialName("ambig") val ambiguity: List<Ambig> = emptyList(),
    /** 整条输入的罗马音（按实际读音写） */
    @SerialName("rom") val romanization: String = "",
    /** 罗马音里要标红的子串（音变 / 特殊读音），多个用 | 分隔 */
    @SerialName("romHi") val romHighlight: String = "",
    /** TOPIK 1~6 / 初·中·高级 */
    @SerialName("level") val level: String = "",
    @SerialName("segs") val segments: List<Segment> = emptyList(),
    @SerialName("grammar") val grammar: Grammar? = null,
    @SerialName("ety") val etymology: Etymology? = null,
    @SerialName("ex") val examples: List<Example> = emptyList(),
    @SerialName("rel") val related: List<Related> = emptyList(),
    /** 语感、场合、忌讳 */
    @SerialName("nuance") val nuance: String = "",
    /** 给中文母语者的专属提示：同形异义、汉字音对应、语序差异 */
    @SerialName("tips") val tips: List<String> = emptyList(),
) {
    val isSentence: Boolean get() = kind == "sentence"
    val kindLabel: String
        get() = when (kind) {
            "sentence" -> "句子"
            "phrase" -> "词组"
            else -> "单词"
        }
    val isEmpty: Boolean
        get() = translation.isBlank() && segments.isEmpty() && etymology == null
}

/**
 * 同一写法有多个常用汉字词或多个易混义项时的一条记录。
 * 例：경주 → 慶州（地名）/ 競走（赛跑）。
 */
@Serializable
data class Ambig(
    /** 原文写法 */
    @SerialName("ko") val ko: String = "",
    /** 候选义项，按可能性从高到低 */
    @SerialName("cands") val candidates: List<String> = emptyList(),
    /** 本次采用的义项，应与 candidates 之一对应 */
    @SerialName("pick") val pick: String = "",
    /** 取舍理由（语境、搭配、常用度） */
    @SerialName("why") val why: String = "",
)

@Serializable
data class Segment(
    /** 原文片段 */
    @SerialName("ko") val ko: String = "",
    /** 实际读音的罗马音（不是逐字转写） */
    @SerialName("rom") val rom: String = "",
    /** rom 里要标红的子串，多个用 | 分隔 */
    @SerialName("romHi") val romHi: String = "",
    /** 韩文实际发音，如 [학꾜] */
    @SerialName("pron") val pron: String = "",
    /** 音变原因，如“紧音化”“词首 ㅂ 读 p” */
    @SerialName("why") val why: String = "",
    /** 韩语词性：명사 / 조사 / 어미 … */
    @SerialName("pos") val pos: String = "",
    /** 语法角色：词干 / 主格助词 / 过去时制 / 尊敬语尾 … */
    @SerialName("tag") val tag: String = "",
    /** 中文意思 */
    @SerialName("zh") val zh: String = "",
    @SerialName("hanja") val hanja: String = "",
    /** 来源语言原词（英/日/法等），如 computer、弁当 */
    @SerialName("src") val src: String = "",
    /** 外来词原词的拉丁转写（英语不需要，日语/法语等需要） */
    @SerialName("srcRom") val srcRom: String = "",
    /** 来源语言代码：zh/en/ja/fr/de/… */
    @SerialName("lang") val lang: String = "",
    /** sino / native / loan / hybrid / unknown */
    @SerialName("origin") val origin: String = "",
    /** 动词·形容词的原形 */
    @SerialName("base") val base: String = "",
    @SerialName("note") val note: String = "",
)

@Serializable
data class Grammar(
    /** 陈述句 / 疑问句 / 命令句 / 共动句 */
    @SerialName("type") val type: String = "",
    /** 现在时 / 过去时 / 将来时 … */
    @SerialName("tense") val tense: String = "",
    /** 해요体 / 합니다体 / 해体 … */
    @SerialName("speech") val speech: String = "",
    /** 成分切分：主语 + 状语 + 谓语 */
    @SerialName("structure") val structure: String = "",
    /** 句型公式 */
    @SerialName("pattern") val pattern: String = "",
    @SerialName("points") val points: List<GrammarPoint> = emptyList(),
)

@Serializable
data class GrammarPoint(
    @SerialName("t") val title: String = "",
    @SerialName("d") val desc: String = "",
    @SerialName("tip") val tip: String = "",
)

@Serializable
data class Etymology(
    @SerialName("summary") val summary: String = "",
    @SerialName("items") val items: List<EtyItem> = emptyList(),
    /** 韩汉同源/同形对照 */
    @SerialName("cognates") val cognates: List<Cognate> = emptyList(),
)

@Serializable
data class EtyItem(
    @SerialName("w") val word: String = "",
    /** sino / native / loan / hybrid / unknown */
    @SerialName("origin") val origin: String = "",
    @SerialName("hanja") val hanja: String = "",
    @SerialName("rom") val rom: String = "",
    /** rom 里要标红的子串，多个用 | 分隔 */
    @SerialName("romHi") val romHi: String = "",
    /** 韩文实际发音，如 [학꾜] */
    @SerialName("pron") val pron: String = "",
    /** 音变原因 */
    @SerialName("why") val why: String = "",
    @SerialName("meaning") val meaning: String = "",
    /** 汉字词的字面义（逐字直译）；只在字面义与今义不同时填，如 親舊→亲戚旧友 */
    @SerialName("lit") val literal: String = "",
    /** 来源语言原词，如 computer / 弁当 / boutique */
    @SerialName("src") val src: String = "",
    /** 外来词原词的拉丁转写（英语不需要，日语/法语等需要） */
    @SerialName("srcRom") val srcRom: String = "",
    /** 来源语言代码：zh/en/ja/fr/de/… */
    @SerialName("lang") val lang: String = "",
    /** 旧字段：外来词原词（与 src 等价，保留兼容） */
    @SerialName("english") val english: String = "",
    /** 汉字词逐字对照 */
    @SerialName("chars") val chars: List<HanjaChar> = emptyList(),
    @SerialName("tip") val tip: String = "",
) {
    /** 来源写法：汉字词用繁体汉字，外来词用原语言词。 */
    val sourceForm: String get() = hanja.ifBlank { src.ifBlank { english } }
}

@Serializable
data class HanjaChar(
    /** 韩语字 */
    @SerialName("ko") val ko: String = "",
    /** 对应繁体汉字 */
    @SerialName("hanja") val hanja: String = "",
    /** 现代中文用字/义 */
    @SerialName("zh") val zh: String = "",
)

@Serializable
data class Cognate(
    @SerialName("ko") val ko: String = "",
    @SerialName("hanja") val hanja: String = "",
    @SerialName("zh") val zh: String = "",
    @SerialName("note") val note: String = "",
)

@Serializable
data class Example(
    @SerialName("ko") val ko: String = "",
    @SerialName("zh") val zh: String = "",
    @SerialName("point") val point: String = "",
)

@Serializable
data class Related(
    @SerialName("w") val word: String = "",
    @SerialName("zh") val zh: String = "",
    @SerialName("note") val note: String = "",
)

/** 词源类型。模型可能返回英文键或中文词，这里做一次归一化。 */
enum class Origin(val id: String, val label: String, val short: String) {
    SINO("sino", "汉字词", "漢"),
    NATIVE("native", "固有词", "固"),
    LOAN("loan", "外来词", "外"),
    HYBRID("hybrid", "混合词", "混"),
    UNKNOWN("unknown", "词源待考", "?");

    companion object {
        fun of(raw: String?): Origin {
            val s = raw?.trim().orEmpty()
            if (s.isEmpty()) return UNKNOWN
            val lower = s.lowercase()
            return when {
                lower.contains("sino") || s.contains("汉字") || s.contains("漢字") || s.contains("한자") -> SINO
                lower.contains("hybrid") || s.contains("混合") -> HYBRID
                lower.contains("native") || s.contains("固有") || s.contains("고유") -> NATIVE
                lower.contains("loan") || lower.contains("foreign") || s.contains("外来") || s.contains("외래") -> LOAN
                else -> UNKNOWN
            }
        }
    }
}

/**
 * 来源标签：汉 / 固 / 英 / 日 / 法 / 德… 全部同一个样式、同一个位置，
 * 不再让“汉字词”比其他来源更特殊；具体内容（汉字、原词）另起一个元素展示。
 */
object SourceLabels {

    fun langKey(raw: String?): String {
        val s = raw?.trim()?.lowercase().orEmpty()
        return when {
            s.startsWith("zh") || s.contains("chinese") || s.contains("汉语") || s.contains("中文") -> "zh"
            s.startsWith("en") || s.contains("english") || s.contains("英语") -> "en"
            s.startsWith("ja") || s.startsWith("jp") || s.contains("japanese") || s.contains("日语") || s.contains("日本語") -> "ja"
            s.startsWith("fr") || s.contains("french") || s.contains("法语") -> "fr"
            s.startsWith("de") || s.contains("german") || s.contains("德语") -> "de"
            s.startsWith("es") || s.contains("spanish") || s.contains("西班牙") -> "es"
            s.startsWith("ru") || s.contains("russian") || s.contains("俄语") -> "ru"
            s.startsWith("it") || s.contains("italian") || s.contains("意大利") -> "it"
            s.startsWith("la") || s.contains("latin") || s.contains("拉丁") -> "la"
            else -> s
        }
    }

    /** 标签上的一个字。 */
    fun short(origin: Origin, lang: String? = null): String = when (origin) {
        Origin.SINO -> "汉"
        Origin.NATIVE -> "本土"
        Origin.HYBRID -> "混"
        Origin.UNKNOWN -> "?"
        Origin.LOAN -> when (langKey(lang)) {
            "en" -> "英"
            "ja" -> "日"
            "fr" -> "法"
            "de" -> "德"
            "es" -> "西"
            "ru" -> "俄"
            "it" -> "意"
            "la" -> "拉"
            "zh" -> "汉"
            else -> "外"
        }
    }

    /** 读屏 / 长按用的完整名称。 */
    fun label(origin: Origin, lang: String? = null): String = when (origin) {
        Origin.SINO -> "汉字词"
        Origin.NATIVE -> "固有词（本土词）"
        Origin.HYBRID -> "混合词"
        Origin.UNKNOWN -> "词源待考"
        Origin.LOAN -> when (langKey(lang)) {
            "en" -> "英语来源"
            "ja" -> "日语来源"
            "fr" -> "法语来源"
            "de" -> "德语来源"
            "es" -> "西班牙语来源"
            "ru" -> "俄语来源"
            "it" -> "意大利语来源"
            "la" -> "拉丁语来源"
            "zh" -> "汉语来源"
            else -> "外来词"
        }
    }
}

/** 韩语语法术语 → 中文。模型按要求只输出韩语术语，展示时补中文。 */
object PosLabels {
    private val map = mapOf(
        "명사" to "名词",
        "대명사" to "代词",
        "수사" to "数词",
        "동사" to "动词",
        "형용사" to "形容词",
        "관형사" to "冠形词",
        "부사" to "副词",
        "조사" to "助词",
        "어미" to "语尾",
        "접사" to "词缀",
        "감탄사" to "感叹词",
        "구" to "短语",
        "절" to "分句",
    )

    fun zh(pos: String): String {
        val key = pos.trim()
        map[key]?.let { return it }
        return map.entries.firstOrNull { key.contains(it.key) }?.value ?: ""
    }

    fun label(pos: String): String {
        val zh = zh(pos)
        return if (zh.isEmpty() || zh == pos) pos else "$pos · $zh"
    }
}

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
    /** word / sentence */
    val kind: String = "word",
    /** 中文译文（单词则为释义） */
    @SerialName("t") val translation: String = "",
    /** 整条输入的罗马音 */
    @SerialName("rom") val romanization: String = "",
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
    val isEmpty: Boolean
        get() = translation.isBlank() && segments.isEmpty() && etymology == null
}

@Serializable
data class Segment(
    /** 原文片段 */
    @SerialName("ko") val ko: String = "",
    @SerialName("rom") val rom: String = "",
    /** 韩语词性：명사 / 조사 / 어미 … */
    @SerialName("pos") val pos: String = "",
    /** 语法角色：词干 / 主格助词 / 过去时制 / 尊敬语尾 … */
    @SerialName("tag") val tag: String = "",
    /** 中文意思 */
    @SerialName("zh") val zh: String = "",
    @SerialName("hanja") val hanja: String = "",
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
    @SerialName("meaning") val meaning: String = "",
    /** 外来词的原词，如 computer / パン */
    @SerialName("english") val english: String = "",
    /** 汉字词逐字对照 */
    @SerialName("chars") val chars: List<HanjaChar> = emptyList(),
    @SerialName("tip") val tip: String = "",
)

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

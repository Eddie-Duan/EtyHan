package com.etyhan.app.ai

import com.etyhan.app.data.Analysis
import kotlinx.serialization.json.Json

sealed interface ParseResult {
    data class Ok(val analysis: Analysis) : ParseResult
    data class Fail(val message: String, val raw: String) : ParseResult
}

/**
 * 模型输出的清洗与解析。
 *
 * 真实世界里模型会：包 ```json 围栏、前后加一句“好的，这是分析”、写尾逗号、
 * 或者干脆返回一段带思考过程的文本。这里全部兜住，并且只信任第一个能解析成功的平衡 JSON 块。
 */
object AnalysisParser {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
        allowStructuredMapKeys = true
    }

    fun parse(raw: String): ParseResult {
        val text = stripFence(raw)
        val candidates = balancedJsonCandidates(text)
        if (candidates.isEmpty()) {
            return ParseResult.Fail("没有找到 JSON 对象", raw)
        }
        var lastError: String? = null
        for (candidate in candidates) {
            val attempts = listOf(candidate, repairTrailingComma(candidate))
            for (attempt in attempts) {
                val analysis = runCatching { json.decodeFromString(Analysis.serializer(), attempt) }.getOrNull()
                if (analysis != null) {
                    val cleaned = normalize(analysis)
                    if (cleaned.isEmpty) {
                        lastError = "JSON 能解析，但译文和分段都是空的"
                        continue
                    }
                    return ParseResult.Ok(cleaned)
                }
            }
        }
        return ParseResult.Fail(lastError ?: "JSON 解析失败（字段类型可能不对）", raw)
    }

    /** 去掉 ```json … ``` 围栏与首尾噪声。 */
    fun stripFence(raw: String): String {
        var s = raw.trim().removePrefix("\uFEFF")
        if (!s.contains("```")) return s
        val start = s.indexOf("```")
        val afterStart = s.indexOf('\n', start).let { if (it < 0) start + 3 else it + 1 }
        val end = s.indexOf("```", afterStart)
        s = if (end > afterStart) s.substring(afterStart, end) else s.substring(afterStart)
        return s.trim()
    }

    /** 扫描所有平衡的 {...} 片段（跳过字符串内的花括号）。 */
    fun balancedJsonCandidates(text: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            if (text[i] != '{') {
                i++
                continue
            }
            var depth = 0
            var inString = false
            var escaped = false
            var j = i
            var closedAt = -1
            while (j < text.length) {
                val c = text[j]
                if (inString) {
                    when {
                        escaped -> escaped = false
                        c == '\\' -> escaped = true
                        c == '"' -> inString = false
                    }
                } else {
                    when (c) {
                        '"' -> inString = true
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) {
                                closedAt = j
                                break
                            }
                        }
                    }
                }
                j++
            }
            if (closedAt > i) {
                out += text.substring(i, closedAt + 1)
                if (out.size >= 3) break
                i = closedAt + 1
            } else {
                i++
            }
        }
        return out
    }

    fun repairTrailingComma(text: String): String =
        Regex(",\\s*([}\\]])").replace(text, "$1")

    /** 修剪空白、丢弃空分段、限制数组长度，避免模型偶尔刷屏把 UI 撑爆。 */
    fun normalize(a: Analysis): Analysis = a.copy(
        kind = a.kind.trim().ifBlank { if (a.segments.size > 1) "sentence" else "word" },
        translation = a.translation.trim(),
        romanization = a.romanization.trim(),
        level = a.level.trim(),
        nuance = a.nuance.trim(),
        romHighlight = a.romHighlight.trim(),
        segments = a.segments
            .filter { it.ko.isNotBlank() || it.zh.isNotBlank() }
            .take(40)
            .map {
                it.copy(
                    ko = it.ko.trim(),
                    rom = it.rom.trim(),
                    romHi = it.romHi.trim(),
                    pron = it.pron.trim(),
                    why = it.why.trim(),
                    pos = it.pos.trim(),
                    tag = it.tag.trim(),
                    zh = it.zh.trim(),
                    hanja = it.hanja.trim(),
                    src = it.src.trim(),
                    srcRom = it.srcRom.trim(),
                    lang = it.lang.trim(),
                    base = it.base.trim(),
                    note = it.note.trim(),
                )
            },
        grammar = a.grammar?.let { g ->
            g.copy(
                type = g.type.trim(),
                tense = g.tense.trim(),
                speech = g.speech.trim(),
                structure = g.structure.trim(),
                pattern = g.pattern.trim(),
                points = g.points.filter { it.title.isNotBlank() || it.desc.isNotBlank() }.take(8)
                    .map { it.copy(title = it.title.trim(), desc = it.desc.trim(), tip = it.tip.trim()) },
            )
        },
        etymology = a.etymology?.let { e ->
            e.copy(
                summary = e.summary.trim(),
                items = e.items.filter { it.word.isNotBlank() }.take(10).map { item ->
                    item.copy(
                        word = item.word.trim(),
                        origin = item.origin.trim(),
                        hanja = item.hanja.trim(),
                        rom = item.rom.trim(),
                        meaning = item.meaning.trim(),
                        literal = item.literal.trim(),
                        english = item.english.trim(),
                        tip = item.tip.trim(),
                        chars = item.chars.filter { it.ko.isNotBlank() || it.hanja.isNotBlank() }.take(6),
                        src = item.src.trim(),
                        srcRom = item.srcRom.trim(),
                        lang = item.lang.trim(),
                        romHi = item.romHi.trim(),
                        pron = item.pron.trim(),
                        why = item.why.trim(),
                    )
                },
                cognates = e.cognates.filter { it.ko.isNotBlank() || it.zh.isNotBlank() }.take(8),
            )
        },
        examples = a.examples.filter { it.ko.isNotBlank() }.take(5)
            .map { it.copy(ko = it.ko.trim(), zh = it.zh.trim(), point = it.point.trim()) },
        related = a.related.filter { it.word.isNotBlank() }.take(10)
            .map { it.copy(word = it.word.trim(), zh = it.zh.trim(), note = it.note.trim()) },
        tips = a.tips.filter { it.isNotBlank() }.take(6).map { it.trim() },
    )

    fun templateVersion(): String = PromptBuilder.TEMPLATE_VERSION
}

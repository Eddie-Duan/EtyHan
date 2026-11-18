package com.etyhan.app.ai

import com.etyhan.app.data.Analysis
import com.etyhan.app.data.Origin
import com.etyhan.app.data.PosLabels
import com.etyhan.app.data.StoredAnalysis

/** 把一份分析导出成可复制/分享的纯文本。 */
object AnalysisFormatter {

    fun toPlainText(stored: StoredAnalysis): String {
        val a = stored.analysis
        return buildString {
            append("【").append(stored.query).append("】\n")
            if (a.romanization.isNotBlank()) append("读音 ").append(a.romanization).append('\n')
            append("译文：").append(a.translation.ifBlank { "—" }).append('\n')
            if (a.nuance.isNotBlank()) append("语感：").append(a.nuance).append('\n')

            if (a.segments.isNotEmpty()) {
                append("\n■ 分段\n")
                a.segments.forEach { s ->
                    append("· ").append(s.ko)
                    if (s.rom.isNotBlank()) append(" [").append(s.rom).append(']')
                    val meta = listOfNotNull(
                        s.pos.ifBlank { null },
                        s.tag.ifBlank { null },
                        s.hanja.ifBlank { null },
                    ).joinToString(" / ")
                    if (meta.isNotBlank()) append("  ").append(meta)
                    if (s.zh.isNotBlank()) append("  → ").append(s.zh)
                    if (s.base.isNotBlank()) append("（原形 ").append(s.base).append("）")
                    append('\n')
                    if (s.note.isNotBlank()) append("   ").append(s.note).append('\n')
                }
            }

            a.grammar?.let { g ->
                append("\n■ 语法\n")
                val head = listOf(g.type, g.tense, g.speech).filter { it.isNotBlank() }
                if (head.isNotEmpty()) append(head.joinToString(" · ")).append('\n')
                if (g.structure.isNotBlank()) append("结构：").append(g.structure).append('\n')
                if (g.pattern.isNotBlank()) append("句型：").append(g.pattern).append('\n')
                g.points.forEach { p ->
                    append("· ").append(p.title)
                    if (p.desc.isNotBlank()) append("：").append(p.desc)
                    append('\n')
                    if (p.tip.isNotBlank()) append("   提示：").append(p.tip).append('\n')
                }
            }

            a.etymology?.let { e ->
                append("\n■ 词源\n")
                if (e.summary.isNotBlank()) append(e.summary).append('\n')
                e.items.forEach { item ->
                    append("· ").append(item.word)
                    if (item.hanja.isNotBlank()) append("（").append(item.hanja).append("）")
                    append(" ").append(Origin.of(item.origin).label)
                    if (item.meaning.isNotBlank()) append("：").append(item.meaning)
                    append('\n')
                    if (item.chars.isNotEmpty()) {
                        append("   ")
                        append(item.chars.joinToString("  ") { "${it.ko}→${it.hanja}（${it.zh}）" })
                        append('\n')
                    }
                    if (item.tip.isNotBlank()) append("   ").append(item.tip).append('\n')
                }
            }

            if (a.examples.isNotEmpty()) {
                append("\n■ 例句\n")
                a.examples.forEach {
                    append("· ").append(it.ko).append('\n')
                    if (it.zh.isNotBlank()) append("  ").append(it.zh).append('\n')
                    if (it.point.isNotBlank()) append("  ").append(it.point).append('\n')
                }
            }

            if (a.tips.isNotEmpty()) {
                append("\n■ 给中文母语者\n")
                a.tips.forEach { append("· ").append(it).append('\n') }
            }

            append("\n— EtyHan · ").append(stored.model.ifBlank { "AI" })
            if (stored.tokensIn + stored.tokensOut > 0) {
                append(" · ").append(stored.tokensIn).append("+").append(stored.tokensOut).append(" tokens")
            }
        }.trim()
    }

    /** 首页历史列表的一行摘要。 */
    fun snippet(a: Analysis): String = a.translation
        .ifBlank { a.segments.joinToString(" ") { it.zh }.trim() }
        .ifBlank { a.etymology?.summary.orEmpty() }
        .take(60)

    /** 统计词源构成，给结果页画比例条。优先用词源条目；没有条目时退回逐段统计。 */
    fun originMix(a: Analysis): List<Pair<Origin, Int>> {
        val etyWords = a.etymology?.items?.map { Origin.of(it.origin) }.orEmpty()
        val words = (if (etyWords.isNotEmpty()) etyWords else a.segments.map { Origin.of(it.origin) })
            .filter { it != Origin.UNKNOWN }
        if (words.isEmpty()) return emptyList()
        return words.groupingBy { it }.eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    fun posLabel(pos: String): String = PosLabels.label(pos)
}

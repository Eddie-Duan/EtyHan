package com.etyhan.app.ai

import com.etyhan.app.data.Analysis
import com.etyhan.app.data.Cognate
import com.etyhan.app.data.Etymology
import com.etyhan.app.data.EtyItem
import com.etyhan.app.data.Example
import com.etyhan.app.data.Grammar
import com.etyhan.app.data.GrammarPoint
import com.etyhan.app.data.HanjaChar
import com.etyhan.app.data.Origin
import com.etyhan.app.data.Segment
import com.etyhan.app.data.StoredAnalysis
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisFormatterTest {

    private val analysis = Analysis(
        kind = "sentence",
        translation = "学生们去学校。",
        romanization = "haksaeng-deul-eun hakgyo-e gamnida",
        level = "TOPIK 2",
        segments = listOf(
            Segment(ko = "학생", rom = "haksaeng", pos = "명사", tag = "词干", zh = "学生", hanja = "學生", origin = "sino"),
            Segment(ko = "들은", rom = "deul-eun", pos = "조사", tag = "复数+主题", zh = "（复数主语）", origin = ""),
            Segment(ko = "학교", rom = "hakgyo", pos = "명사", tag = "词干", zh = "学校", hanja = "學校", origin = "sino"),
            Segment(ko = "에", rom = "e", pos = "조사", tag = "方向助词", zh = "到", origin = ""),
            Segment(ko = "갑니다", rom = "gamnida", pos = "동사", tag = "终结语尾", zh = "去", base = "가다", origin = "native"),
        ),
        grammar = Grammar(
            type = "陈述句",
            tense = "现在时",
            speech = "합니다体",
            structure = "主语 + 目的地 + 谓语",
            pattern = "N은/는 N에 가다",
            points = listOf(GrammarPoint("은/는", "主题助词", "对比时说“至于…”")),
        ),
        etymology = Etymology(
            summary = "两个汉字词，其余为固有词与助词。",
            items = listOf(
                EtyItem(
                    word = "학교",
                    origin = "sino",
                    hanja = "學校",
                    rom = "hakgyo",
                    meaning = "学校",
                    chars = listOf(HanjaChar("학", "學", "学"), HanjaChar("교", "校", "校")),
                    tip = "학 对应“學”的入声",
                ),
                EtyItem(word = "가다", origin = "native", meaning = "去"),
            ),
            cognates = listOf(Cognate("학교", "學校", "学校", "词序相同")),
        ),
        examples = listOf(Example("학생들은 학교에 갑니다.", "学生们去学校。", "은/는 主题助词")),
        tips = listOf("汉字词约占韩语六成"),
    )

    @Test
    fun plainTextHasAllSections() {
        val stored = StoredAnalysis(
            id = 1,
            query = "학생들은 학교에 갑니다",
            analysis = analysis,
            provider = "deepseek",
            model = "deepseek-chat",
            tokensIn = 800,
            tokensOut = 600,
            elapsedMs = 4200,
            favorite = false,
            rawJson = "{}",
            createdAt = 0,
            updatedAt = 0,
        )
        val text = AnalysisFormatter.toPlainText(stored)
        assertTrue(text.contains("【학생들은 학교에 갑니다】"))
        assertTrue(text.contains("译文：学生们去学校。"))
        assertTrue(text.contains("■ 分段"))
        assertTrue(text.contains("학→學（学）"))
        assertTrue(text.contains("■ 语法"))
        assertTrue(text.contains("■ 词源"))
        assertTrue(text.contains("■ 例句"))
        assertTrue(text.contains("■ 给中文母语者"))
        assertTrue(text.contains("deepseek-chat"))
    }

    @Test
    fun originMixCountsEtymologyItems() {
        val mix = AnalysisFormatter.originMix(analysis)
        val sino = mix.first { it.first == Origin.SINO }.second
        val native = mix.first { it.first == Origin.NATIVE }.second
        // 两个词源条目：학교(sino)、가다(native)
        assertTrue(sino == 1)
        assertTrue(native == 1)
    }

    @Test
    fun snippetFallsBackToTranslation() {
        assertTrue(AnalysisFormatter.snippet(analysis).startsWith("学生们"))
    }
}

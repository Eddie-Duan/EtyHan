package com.etyhan.app.ai

import com.etyhan.app.data.Depth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    @Test
    fun systemPromptIsStableForCaching() {
        val a = PromptBuilder.system()
        val b = PromptBuilder.system()
        assertEquals(a, b)
        // 稳定前缀：schema 与纪律必须在系统消息里，输入在 user 消息末尾
        assertTrue(a.contains("\"segs\""))
        assertTrue(a.contains("ety"))
        assertTrue(a.contains("chars"))
        assertTrue(a.contains("romHi"))
        assertTrue(a.contains("\"lang\"") || a.contains("lang："))
        assertTrue(a.contains("只输出一个 JSON 对象"))
    }

    @Test
    fun extraInstructionAppendedOnlyWhenPresent() {
        val base = PromptBuilder.system()
        val withExtra = PromptBuilder.system("每个词补 TOPIK 等级")
        assertTrue(withExtra.startsWith(base))
        assertTrue(withExtra.contains("每个词补 TOPIK 等级"))
    }

    @Test
    fun userPromptCarriesQueryAndDepth() {
        val q = "학생들은 학교에 갑니다."
        val u = PromptBuilder.user(q, Depth.LITE, true)
        assertTrue(u.contains(q))
        assertTrue(u.contains("depth=lite"))
        assertTrue(u.contains("showRomanization=true"))
        assertTrue(u.contains("≤ 350"))
        assertFalse(PromptBuilder.user(q, Depth.FULL, false).contains("≤ 350"))
    }

    @Test
    fun budgetsGrowWithDepth() {
        val lite = PromptBuilder.user("x", Depth.LITE, true)
        val full = PromptBuilder.user("x", Depth.FULL, true)
        assertTrue(lite.contains("ex 最多 1 条"))
        assertTrue(full.contains("ex 最多 3 条"))
    }
}

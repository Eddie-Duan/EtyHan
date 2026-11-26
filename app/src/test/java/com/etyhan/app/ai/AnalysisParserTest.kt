package com.etyhan.app.ai

import com.etyhan.app.data.Origin
import com.etyhan.app.data.PosLabels
import com.etyhan.app.data.SourceLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisParserTest {

    private val sample = """
        {
          "v": 1,
          "kind": "sentence",
          "t": "朋友去学校。",
          "rom": "chingu-ga hakgyo-e gayo",
          "level": "TOPIK 1",
          "segs": [
            {"ko":"친구","rom":"chingu","pos":"명사","tag":"词干","zh":"朋友","origin":"native"},
            {"ko":"학교","rom":"hakgyo","pos":"명사","tag":"词干","zh":"学校","hanja":"學校","origin":"sino"},
            {"ko":"에","rom":"e","pos":"조사","tag":"方向助词","zh":"到/往","origin":""}
          ],
          "grammar": {
            "type":"陈述句","tense":"现在时","speech":"해요体",
            "structure":"主语 + 目的地 + 谓语",
            "pattern":"N이/가 N에 가다",
            "points":[{"t":"이/가","d":"主格助词","tip":"中文靠语序"}]
          },
          "ety": {
            "summary":"一个汉字词：학교(學校)",
            "items":[{"w":"학교","origin":"sino","hanja":"學校","rom":"hakgyo","meaning":"学校",
              "chars":[{"ko":"학","hanja":"學","zh":"学"},{"ko":"교","hanja":"校","zh":"校"}],
              "tip":"학↔學"}],
            "cognates":[{"ko":"학교","hanja":"學校","zh":"学校","note":"词序相同"}]
          },
          "ex":[{"ko":"친구가 학교에 갔어요.","zh":"朋友去了学校。","point":"过去时"}],
          "rel":[{"w":"학생","zh":"学生","note":"同用 학"}],
          "nuance":"日常口语。",
          "tips":["에 表方向，에서 表动作发生地"]
        }
    """.trimIndent()

    @Test
    fun parsesPlainJson() {
        val r = AnalysisParser.parse(sample)
        assertTrue(r is ParseResult.Ok)
        val a = (r as ParseResult.Ok).analysis
        assertEquals("朋友去学校。", a.translation)
        assertEquals(3, a.segments.size)
        assertEquals("學校", a.segments[1].hanja)
        assertEquals(2, a.etymology!!.items.first().chars.size)
        assertEquals("TOPIK 1", a.level)
        assertEquals(1, a.tips.size)
    }

    @Test
    fun stripsMarkdownFenceAndChatter() {
        val raw = "好的，这是分析结果：\n```json\n$sample\n```\n希望有帮助。"
        val r = AnalysisParser.parse(raw)
        assertTrue(r is ParseResult.Ok)
    }

    @Test
    fun handlesBracesInsideStrings() {
        val tricky = sample.replace("朋友去学校。", "他说“{别动}”。")
        val r = AnalysisParser.parse("前言 {\"not\": \"json\"} 然后 $tricky")
        assertTrue(r is ParseResult.Ok)
        assertEquals("他说“{别动}”。", (r as ParseResult.Ok).analysis.translation)
    }

    @Test
    fun repairsTrailingComma() {
        val broken = "{\"t\":\"学校\",\"kind\":\"word\",\"segs\":[{\"ko\":\"학교\",\"zh\":\"学校\",},],}"
        val r = AnalysisParser.parse(broken)
        assertTrue(r is ParseResult.Ok)
        assertEquals("学校", (r as ParseResult.Ok).analysis.translation)
    }

    @Test
    fun failsOnNonJson() {
        val r = AnalysisParser.parse("抱歉，我无法完成这个请求。")
        assertTrue(r is ParseResult.Fail)
    }

    @Test
    fun normalizesBlankSegments() {
        val raw = """{"t":"学校","segs":[{"ko":"","zh":""},{"ko":"학교","zh":"学校"}],"tips":["","有效提示"]}"""
        val a = (AnalysisParser.parse(raw) as ParseResult.Ok).analysis
        assertEquals(1, a.segments.size)
        assertEquals(1, a.tips.size)
    }


    @Test
    fun parsesPronunciationAndSourceFields() {
        val raw = """
            {
              "t": "学校", "rom": "hakkyo", "romHi": "kk",
              "segs": [
                {"ko":"학교","rom":"hakkyo","romHi":"kk","pron":"[학꾜]","why":"紧音化","pos":"명사","zh":"学校","hanja":"學校","origin":"sino"},
                {"ko":"버스","rom":"beoseu","zh":"公交车","origin":"loan","lang":"en","src":"bus","srcRom":""}
              ],
              "ety": {"items":[{"w":"버스","origin":"loan","lang":"en","src":"bus","meaning":"公交车"}]}
            }
        """.trimIndent()
        val a = (AnalysisParser.parse(raw) as ParseResult.Ok).analysis
        assertEquals("kk", a.romHighlight)
        assertEquals("kk", a.segments[0].romHi)
        assertEquals("[학꾜]", a.segments[0].pron)
        assertEquals("紧音化", a.segments[0].why)
        assertEquals("bus", a.segments[1].src)
        assertEquals("en", a.segments[1].lang)
        assertEquals("", a.segments[1].srcRom)
        assertEquals("bus", a.etymology!!.items[0].sourceForm)
    }

    @Test
    fun sourceLabelsAreEqualStatus() {
        assertEquals("漢", SourceLabels.short(Origin.SINO, ""))
        assertEquals("本土", SourceLabels.short(Origin.NATIVE, ""))
        assertEquals("英", SourceLabels.short(Origin.LOAN, "en"))
        assertEquals("日", SourceLabels.short(Origin.LOAN, "Japanese"))
        assertEquals("法", SourceLabels.short(Origin.LOAN, "fr"))
        assertEquals("德", SourceLabels.short(Origin.LOAN, "德语"))
        assertEquals("外", SourceLabels.short(Origin.LOAN, ""))
        assertEquals("英语来源", SourceLabels.label(Origin.LOAN, "en"))
        assertEquals("日语来源", SourceLabels.label(Origin.LOAN, "ja"))
    }

    @Test
    fun trimsNewFields() {
        val raw = """{"t":"x","romHi":" kk ","segs":[{"ko":"학교","rom":" hakkyo ","romHi":" kk ","src":" bus ","lang":" en "}]}"""
        val a = (AnalysisParser.parse(raw) as ParseResult.Ok).analysis
        assertEquals("kk", a.romHighlight)
        assertEquals("hakkyo", a.segments[0].rom)
        assertEquals("kk", a.segments[0].romHi)
        assertEquals("bus", a.segments[0].src)
        assertEquals("en", a.segments[0].lang)
    }

    @Test
    fun originNormalization() {
        assertEquals(Origin.SINO, Origin.of("sino"))
        assertEquals(Origin.SINO, Origin.of("汉字词"))
        assertEquals(Origin.SINO, Origin.of("漢字語"))
        assertEquals(Origin.NATIVE, Origin.of("native"))
        assertEquals(Origin.LOAN, Origin.of("外来词"))
        assertEquals(Origin.HYBRID, Origin.of("hybrid"))
        assertEquals(Origin.UNKNOWN, Origin.of(""))
    }

    @Test
    fun posLabels() {
        assertEquals("名词", PosLabels.zh("명사"))
        assertEquals("语尾", PosLabels.zh("어미"))
        assertEquals("명사 · 名词", PosLabels.label("명사"))
        assertEquals("", PosLabels.zh("unknown-term"))
    }
}

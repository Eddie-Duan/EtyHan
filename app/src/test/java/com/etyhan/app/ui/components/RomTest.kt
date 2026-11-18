package com.etyhan.app.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class RomTest {

    @Test
    fun splitsHighlightTokens() {
        assertEquals(listOf("kk", "p"), romTokens("kk|p"))
        assertEquals(listOf("kk", "e"), romTokens(" kk , e "))
        assertEquals(emptyList<String>(), romTokens(""))
    }

    @Test
    fun highlightsMultiLetterTokenAtItsRange() {
        val annotated = highlightRom("hakkyo-e beoseu-ro gayo", "kk", Color.Red)
        val ranges = annotated.spanStyles.map { it.start to it.end }
        assertEquals(listOf(2 to 4), ranges)
    }

    @Test
    fun highlightsFirstOccurrenceOfSingleLetter() {
        val annotated = highlightRom("pusan", "p", Color.Red)
        val ranges = annotated.spanStyles.map { it.start to it.end }
        assertEquals(listOf(0 to 1), ranges)
    }

    @Test
    fun ignoresTokenMissingFromRom() {
        val annotated = highlightRom("hakkyo", "zz", Color.Red)
        assertEquals(0, annotated.spanStyles.size)
    }
}

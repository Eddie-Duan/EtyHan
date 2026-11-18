package com.etyhan.app.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * 罗马音标注：把音变 / 特殊读音的片段标红。
 *
 * 模型给的 `romHi` 用 | 分隔多个片段；单字母片段只标第一处，避免把整个词的同类字母都染红。
 */
@Composable
fun pronunciationRed(): Color =
    if (isSystemInDarkTheme()) Color(0xFFFF8A80) else Color(0xFFC62828)

fun romTokens(highlight: String): List<String> =
    highlight
        .split('|', ',', ';', '，', '、', '/', ' ')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

fun highlightRom(rom: String, highlight: String, color: Color): AnnotatedString =
    buildAnnotatedString {
        append(rom)
        romTokens(highlight).forEach { token ->
            if (token.length == 1) {
                val index = rom.indexOf(token, ignoreCase = true)
                if (index >= 0) {
                    addStyle(
                        SpanStyle(color = color, fontWeight = FontWeight.SemiBold),
                        index,
                        index + token.length,
                    )
                }
            } else {
                var index = rom.indexOf(token, 0, ignoreCase = true)
                while (index >= 0) {
                    addStyle(
                        SpanStyle(color = color, fontWeight = FontWeight.SemiBold),
                        index,
                        index + token.length,
                    )
                    index = rom.indexOf(token, index + token.length, ignoreCase = true)
                }
            }
        }
    }

@Composable
fun RomText(
    rom: String,
    highlight: String = "",
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    baseColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = Int.MAX_VALUE,
) {
    if (rom.isBlank()) return
    val red = pronunciationRed()
    val annotated = remember(rom, highlight, red) { highlightRom(rom, highlight, red) }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = baseColor,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

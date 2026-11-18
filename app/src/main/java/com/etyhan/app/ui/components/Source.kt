package com.etyhan.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.etyhan.app.data.Origin
import com.etyhan.app.data.SourceLabels

/**
 * 来源标签：汉 / 固 / 英 / 日 / 法 / 德…
 *
 * 所有来源一个样式、一个位置、同样大小——“汉字词”不再比外来词特殊。
 * 标签只放语言，具体内容（汉字、原词）由旁边的 [SourceForm] 单独承载。
 */
@Composable
fun SourceBadge(
    origin: Origin,
    lang: String = "",
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = sourceColors(origin, lang)
    Surface(
        modifier = modifier,
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.18f)),
    ) {
        Text(
            text = SourceLabels.short(origin, lang),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}

/**
 * 来源内容：汉字词的汉字、外来词的原词。与标签分开的独立元素。
 *
 * 英语/汉语来源只显示原词；日语、法语等看不懂的语言再附一行拉丁转写（[rom]）。
 */
@Composable
fun SourceForm(
    text: String,
    rom: String = "",
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    if (text.isBlank()) return
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 9.dp, vertical = if (large) 5.dp else 3.dp),
        ) {
            Text(
                text = text,
                style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Serif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (rom.isNotBlank()) {
                Text(
                    text = rom,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun sourceColors(origin: Origin, lang: String = ""): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    fun pick(lightBg: Long, lightFg: Long, darkBg: Long, darkFg: Long): Pair<Color, Color> =
        if (dark) Color(darkBg) to Color(darkFg) else Color(lightBg) to Color(lightFg)

    return when (origin) {
        Origin.SINO -> pick(0xFFF3DED7, 0xFF7E1B16, 0xFF4A2320, 0xFFF3C8C0)
        Origin.NATIVE -> pick(0xFFDDEBDE, 0xFF2C5A41, 0xFF1F3A2C, 0xFFB9DCC6)
        Origin.HYBRID -> pick(0xFFE8DFF4, 0xFF4A2E70, 0xFF332845, 0xFFD6C6F0)
        Origin.UNKNOWN -> pick(0xFFEDE7DB, 0xFF6B6252, 0xFF33302B, 0xFFCFC7B9)
        Origin.LOAN -> when (SourceLabels.langKey(lang)) {
            "en" -> pick(0xFFDCE7F3, 0xFF1F4870, 0xFF1E3348, 0xFFBBD5EE)
            "ja" -> pick(0xFFEADCEC, 0xFF5B2A5E, 0xFF3A2340, 0xFFE4C9E8)
            "fr" -> pick(0xFFF3E7D2, 0xFF6B4A16, 0xFF3B2E17, 0xFFF0D9AE)
            "de" -> pick(0xFFE1E4E8, 0xFF33414F, 0xFF28313A, 0xFFC9D6E2)
            "es" -> pick(0xFFF6E2D5, 0xFF7A3A16, 0xFF3D2617, 0xFFF5CBAF)
            "ru" -> pick(0xFFD9EAEA, 0xFF1F4F4C, 0xFF1B3A38, 0xFFBDE3DF)
            "it" -> pick(0xFFE4F0DC, 0xFF3C5822, 0xFF293B1C, 0xFFD2E8BF)
            "la" -> pick(0xFFEFE3D8, 0xFF5C3B22, 0xFF38261A, 0xFFEAD3BE)
            "zh" -> pick(0xFFF3DED7, 0xFF7E1B16, 0xFF4A2320, 0xFFF3C8C0)
            else -> pick(0xFFE3E6EA, 0xFF46505C, 0xFF2A3037, 0xFFCFD6DE)
        }
    }
}

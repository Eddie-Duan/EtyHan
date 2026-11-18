package com.etyhan.app.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun relativeTime(millis: Long, now: Long = System.currentTimeMillis()): String {
    if (millis <= 0) return ""
    val diff = now - millis
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000} 天前"
        else -> {
            val cal = Calendar.getInstance()
            val target = Calendar.getInstance().apply { timeInMillis = millis }
            val pattern = if (cal.get(Calendar.YEAR) == target.get(Calendar.YEAR)) "M月d日" else "yyyy年M月d日"
            SimpleDateFormat(pattern, Locale.CHINA).format(Date(millis))
        }
    }
}

fun formatTokens(n: Int): String =
    if (n >= 1000) String.format(Locale.CHINA, "%.1fk", n / 1000f) else n.toString()

fun formatDuration(ms: Long): String =
    if (ms < 1000) "${ms}ms" else String.format(Locale.CHINA, "%.1fs", ms / 1000f)

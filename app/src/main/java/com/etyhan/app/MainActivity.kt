package com.etyhan.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.etyhan.app.ui.EtyHanRoot
import com.etyhan.app.ui.theme.EtyHanTheme

class MainActivity : ComponentActivity() {

    private val incomingText = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingText.value = extractText(intent)
        setContent {
            EtyHanTheme {
                EtyHanRoot(initialQuery = incomingText.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingText.value = extractText(intent)
    }

    /** 支持从别的 App 分享文本、或选中文本后“处理文本”直接查词。 */
    private fun extractText(intent: Intent?): String {
        if (intent == null) return ""
        val raw: String? = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT ->
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }
        return raw?.trim()?.take(500).orEmpty()
    }
}

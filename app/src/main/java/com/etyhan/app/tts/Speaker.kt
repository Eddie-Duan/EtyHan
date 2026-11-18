package com.etyhan.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * 韩语朗读。系统 TTS 初始化是异步的，所以先排队、就绪后再念；
 * 没装韩语语音包时通过 [message] 提示用户去系统设置安装。
 */
class Speaker(private val context: Context) {

    private var engine: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    private var initializing = false

    @Volatile
    private var enabled = true

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) engine?.stop()
    }

    fun speak(text: String) {
        if (!enabled) return
        val clean = text.trim()
        if (clean.isEmpty()) return

        if (engine == null) {
            pending = clean
            if (initializing) return
            initializing = true
            engine = TextToSpeech(context) { status ->
                initializing = false
                if (status == TextToSpeech.SUCCESS) {
                    val tts = engine ?: return@TextToSpeech
                    val result = tts.setLanguage(Locale.KOREAN)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        _message.value = "系统缺少韩语语音包，可在「设置 → 语言与输入 → 文字转语音」中安装"
                        ready = false
                    } else {
                        tts.setSpeechRate(0.92f)
                        ready = true
                        pending?.let { speak(it) }
                    }
                } else {
                    _message.value = "文字转语音初始化失败"
                }
                pending = null
            }
            return
        }

        if (!ready) {
            pending = clean
            return
        }
        engine?.stop()
        engine?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "etyhan-${clean.hashCode()}")
    }

    fun dismissMessage() {
        _message.value = null
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
    }
}

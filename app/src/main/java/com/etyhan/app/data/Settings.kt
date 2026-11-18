package com.etyhan.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Depth(val id: String, val label: String, val hint: String) {
    LITE("lite", "精简", "译文 + 分段 + 词源要点，最省 token"),
    STANDARD("standard", "标准", "默认：语法点 + 词源 + 2 条例句"),
    FULL("full", "详细", "更多语法点、对照与提示，token 约多 50%");

    companion object {
        fun of(id: String?): Depth = entries.firstOrNull { it.id == id } ?: STANDARD
    }
}

data class AppSettings(
    val providerId: String = "deepseek",
    val baseUrl: String = "https://api.deepseek.com/v1",
    val model: String = "deepseek-chat",
    val depth: Depth = Depth.STANDARD,
    val jsonMode: Boolean = true,
    val showRomanization: Boolean = true,
    val maxTokens: Int = 1200,
    val temperature: Float = 0.3f,
    val extraInstruction: String = "",
    val ttsEnabled: Boolean = true,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "etyhan_settings")

class SettingsRepository(private val context: Context) {

    val flow: Flow<AppSettings> = context.settingsStore.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsStore.edit { prefs -> transform(prefs.toSettings()).writeTo(prefs) }
    }

    private fun Preferences.toSettings() = AppSettings(
        providerId = this[K_PROVIDER] ?: "deepseek",
        baseUrl = this[K_BASE_URL] ?: "https://api.deepseek.com/v1",
        model = this[K_MODEL] ?: "deepseek-chat",
        depth = Depth.of(this[K_DEPTH]),
        jsonMode = this[K_JSON_MODE] ?: true,
        showRomanization = this[K_SHOW_ROM] ?: true,
        maxTokens = this[K_MAX_TOKENS] ?: 1200,
        temperature = this[K_TEMPERATURE] ?: 0.3f,
        extraInstruction = this[K_EXTRA] ?: "",
        ttsEnabled = this[K_TTS] ?: true,
    )

    private fun AppSettings.writeTo(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs[K_PROVIDER] = providerId
        prefs[K_BASE_URL] = baseUrl
        prefs[K_MODEL] = model
        prefs[K_DEPTH] = depth.id
        prefs[K_JSON_MODE] = jsonMode
        prefs[K_SHOW_ROM] = showRomanization
        prefs[K_MAX_TOKENS] = maxTokens
        prefs[K_TEMPERATURE] = temperature
        prefs[K_EXTRA] = extraInstruction
        prefs[K_TTS] = ttsEnabled
    }

    private companion object {
        val K_PROVIDER = stringPreferencesKey("provider_id")
        val K_BASE_URL = stringPreferencesKey("base_url")
        val K_MODEL = stringPreferencesKey("model")
        val K_DEPTH = stringPreferencesKey("depth")
        val K_JSON_MODE = booleanPreferencesKey("json_mode")
        val K_SHOW_ROM = booleanPreferencesKey("show_romanization")
        val K_MAX_TOKENS = intPreferencesKey("max_tokens")
        val K_TEMPERATURE = floatPreferencesKey("temperature")
        val K_EXTRA = stringPreferencesKey("extra_instruction")
        val K_TTS = booleanPreferencesKey("tts_enabled")
    }
}

/**
 * API Key 单独存：先试 Android Keystore 加密的 SharedPreferences，
 * 某些 ROM 上 Keystore 会坏，就退回应用私有目录的普通文件（仍然只有本应用可读）。
 */
class SecretStore(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "etyhan_secrets",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            ) as SharedPreferences
        }.getOrElse {
            context.getSharedPreferences("etyhan_secrets_plain", Context.MODE_PRIVATE)
        }
    }

    fun get(providerId: String): String = prefs.getString(keyOf(providerId), "").orEmpty()

    fun put(providerId: String, value: String) {
        prefs.edit().putString(keyOf(providerId), value.trim()).apply()
    }

    fun has(providerId: String): Boolean = get(providerId).isNotBlank()

    private fun keyOf(providerId: String) = "key_$providerId"
}

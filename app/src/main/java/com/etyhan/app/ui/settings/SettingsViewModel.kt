package com.etyhan.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etyhan.app.AppContainer
import com.etyhan.app.ai.LlmConfig
import com.etyhan.app.ai.LlmException
import com.etyhan.app.data.AnalysisRepository
import com.etyhan.app.data.AppSettings
import com.etyhan.app.data.Depth
import com.etyhan.app.data.ProviderPreset
import com.etyhan.app.data.Providers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val provider: ProviderPreset = Providers.byId("deepseek"),
    val apiKey: String = "",
    val keyVisible: Boolean = false,
    val loaded: Boolean = false,
    val testing: Boolean = false,
    val testMessage: String? = null,
    val testOk: Boolean = false,
    val historyCount: Int = 0,
    val promptPreview: String? = null,
    val message: String? = null,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val repo: AnalysisRepository = container.repository
    private val state = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = state

    private var keyJob: Job? = null

    init {
        viewModelScope.launch {
            val s = container.settings.flow.first()
            state.update {
                it.copy(
                    settings = s,
                    provider = Providers.byId(s.providerId),
                    apiKey = container.secrets.get(s.providerId),
                    loaded = true,
                )
            }
            state.update { it.copy(historyCount = repo.count()) }
        }
    }

    fun selectProvider(id: String) {
        val preset = Providers.byId(id)
        state.update {
            it.copy(
                provider = preset,
                apiKey = container.secrets.get(id),
                testMessage = null,
                settings = it.settings.copy(
                    providerId = id,
                    baseUrl = preset.baseUrl,
                    model = preset.model,
                    jsonMode = preset.jsonMode,
                ),
            )
        }
        persist { it.copy(providerId = id, baseUrl = preset.baseUrl, model = preset.model, jsonMode = preset.jsonMode) }
    }

    fun setBaseUrl(value: String) = updateAndPersist { it.copy(baseUrl = value.trim()) }

    fun setModel(value: String) = updateAndPersist { it.copy(model = value.trim()) }

    fun setDepth(depth: Depth) = updateAndPersist { it.copy(depth = depth) }

    fun setJsonMode(enabled: Boolean) = updateAndPersist { it.copy(jsonMode = enabled) }

    fun setShowRomanization(enabled: Boolean) = updateAndPersist { it.copy(showRomanization = enabled) }

    fun setTtsEnabled(enabled: Boolean) = updateAndPersist { it.copy(ttsEnabled = enabled) }

    fun setExtraInstruction(value: String) = updateAndPersist { it.copy(extraInstruction = value) }

    fun setApiKey(value: String) {
        val providerId = state.value.settings.providerId
        state.update { it.copy(apiKey = value, testMessage = null) }
        keyJob?.cancel()
        keyJob = viewModelScope.launch {
            delay(600)
            container.secrets.put(providerId, value)
        }
    }

    fun toggleKeyVisible() = state.update { it.copy(keyVisible = !it.keyVisible) }

    fun testConnection() {
        if (state.value.testing) return
        val s = state.value.settings
        val key = state.value.apiKey
        state.update { it.copy(testing = true, testMessage = null) }
        viewModelScope.launch {
            // 用界面上的值而不是 DataStore 里的值，避免“刚填完就测”读到旧 Key
            container.secrets.put(s.providerId, key)
            val cfg = LlmConfig(
                baseUrl = s.baseUrl,
                apiKey = key,
                model = s.model,
                jsonMode = s.jsonMode,
                maxTokens = s.maxTokens,
                temperature = s.temperature,
            )
            val result = runCatching { container.llm.ping(cfg) }
            result.fold(
                onSuccess = { msg ->
                    state.update { it.copy(testing = false, testMessage = msg, testOk = true) }
                },
                onFailure = { e ->
                    val text = when (e) {
                        is LlmException -> listOfNotNull(e.message, e.hint).joinToString(" · ")
                        else -> e.message ?: e.javaClass.simpleName
                    }
                    state.update { it.copy(testing = false, testMessage = text, testOk = false) }
                },
            )
        }
    }

    fun previewPrompt() {
        val s = state.value.settings
        state.update { it.copy(promptPreview = repo.promptPreview(s)) }
    }

    fun closePreview() = state.update { it.copy(promptPreview = null) }

    fun clearHistory() {
        viewModelScope.launch {
            repo.clearAll()
            state.update { it.copy(historyCount = 0, message = "已清空历史与词库") }
        }
    }

    fun dismissMessage() = state.update { it.copy(message = null) }

    fun refreshCount() {
        viewModelScope.launch { state.update { it.copy(historyCount = repo.count()) } }
    }

    private fun updateAndPersist(transform: (AppSettings) -> AppSettings) {
        state.update { it.copy(settings = transform(it.settings), testMessage = null) }
        persist(transform)
    }

    private fun persist(transform: (AppSettings) -> AppSettings) {
        // DataStore 自己会串行化写入，这里不再防抖，避免“改完立刻退出”丢设置
        viewModelScope.launch { container.settings.update(transform) }
    }

    override fun onCleared() {
        // 离开页面时把防抖中的 Key 立刻写掉
        runCatching { container.secrets.put(state.value.settings.providerId, state.value.apiKey) }
        super.onCleared()
    }
}

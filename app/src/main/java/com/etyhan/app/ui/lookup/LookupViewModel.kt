package com.etyhan.app.ui.lookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etyhan.app.AppContainer
import com.etyhan.app.data.AnalysisRepository
import com.etyhan.app.data.AnalyzeOutcome
import com.etyhan.app.data.AppSettings
import com.etyhan.app.data.Providers
import com.etyhan.app.data.StoredAnalysis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LookupUiState(
    val input: String = "",
    val loading: Boolean = false,
    val elapsedSec: Int = 0,
    val error: AnalyzeOutcome.Failure? = null,
    val navigateTo: Long? = null,
    val settings: AppSettings = AppSettings(),
    val hasKey: Boolean = true,
    val recent: List<StoredAnalysis> = emptyList(),
) {
    val providerName: String get() = Providers.byId(settings.providerId).name
    val ready: Boolean get() = settings.baseUrl.isNotBlank() && settings.model.isNotBlank() && hasKey
}

class LookupViewModel(private val container: AppContainer) : ViewModel() {

    private val repo: AnalysisRepository = container.repository
    private val state = MutableStateFlow(LookupUiState())
    val uiState: StateFlow<LookupUiState> = state

    private var job: Job? = null
    private var ticker: Job? = null
    private var lastAutoQuery: String? = null

    init {
        viewModelScope.launch {
            repo.observeRecent(12).collect { list ->
                state.update { it.copy(recent = list) }
            }
        }
        viewModelScope.launch {
            container.settings.flow.collect { s ->
                val preset = Providers.byId(s.providerId)
                val needsKey = preset.needsKey && s.providerId != "custom"
                state.update {
                    it.copy(
                        settings = s,
                        hasKey = !needsKey || container.secrets.has(s.providerId),
                    )
                }
            }
        }
    }

    fun setInput(text: String) {
        state.update { it.copy(input = text, error = null) }
    }

    fun analyze(force: Boolean = false) {
        val query = state.value.input.trim()
        if (query.isEmpty() || state.value.loading) return
        job?.cancel()
        state.update { it.copy(loading = true, error = null, elapsedSec = 0, navigateTo = null) }
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(1000)
                state.update { it.copy(elapsedSec = it.elapsedSec + 1) }
            }
        }
        job = viewModelScope.launch {
            val outcome = repo.analyze(query, force)
            ticker?.cancel()
            when (outcome) {
                is AnalyzeOutcome.Success ->
                    state.update { it.copy(loading = false, navigateTo = outcome.id) }
                is AnalyzeOutcome.Failure ->
                    state.update { it.copy(loading = false, error = outcome) }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        ticker?.cancel()
        state.update { it.copy(loading = false, elapsedSec = 0) }
    }

    fun dismissError() = state.update { it.copy(error = null) }

    fun consumeNavigation() = state.update { it.copy(navigateTo = null) }

    fun prefill(text: String) {
        state.update { it.copy(input = text, error = null) }
    }

    /** 从分享/选中文本进来、或从结果页点词跳转时使用；同一个文本只自动分析一次。 */
    fun autoAnalyze(text: String) {
        val q = text.trim()
        if (q.isEmpty() || q == lastAutoQuery) return
        lastAutoQuery = q
        state.update { it.copy(input = q, error = null) }
        analyze()
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }
}

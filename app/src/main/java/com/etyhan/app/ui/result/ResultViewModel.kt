package com.etyhan.app.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etyhan.app.AppContainer
import com.etyhan.app.ai.AnalysisFormatter
import com.etyhan.app.data.AnalysisRepository
import com.etyhan.app.data.AnalyzeOutcome
import com.etyhan.app.data.StoredAnalysis
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultUiState(
    val id: Long = 0,
    val stored: StoredAnalysis? = null,
    val loading: Boolean = true,
    val analyzing: Boolean = false,
    val error: AnalyzeOutcome.Failure? = null,
    val notFound: Boolean = false,
    val deleted: Boolean = false,
)

class ResultViewModel(
    private val container: AppContainer,
    private val initialId: Long,
) : ViewModel() {

    private val repo: AnalysisRepository = container.repository
    private val state = MutableStateFlow(ResultUiState(id = initialId))
    val uiState: StateFlow<ResultUiState> = state

    private var job: Job? = null

    init {
        load(initialId)
    }

    fun load(id: Long) {
        state.update { it.copy(id = id, loading = true, notFound = false) }
        job?.cancel()
        job = viewModelScope.launch {
            val stored = repo.find(id)
            state.update {
                it.copy(
                    stored = stored,
                    loading = false,
                    notFound = stored == null,
                )
            }
        }
    }

    fun toggleFavorite() {
        val stored = state.value.stored ?: return
        val next = !stored.favorite
        state.update { it.copy(stored = stored.copy(favorite = next)) }
        viewModelScope.launch { repo.setFavorite(stored.id, next) }
    }

    fun reanalyze() {
        val stored = state.value.stored ?: return
        if (state.value.analyzing) return
        state.update { it.copy(analyzing = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repo.analyze(stored.query, force = true)) {
                is AnalyzeOutcome.Success -> {
                    state.update { it.copy(analyzing = false) }
                    load(outcome.id)
                }
                is AnalyzeOutcome.Failure ->
                    state.update { it.copy(analyzing = false, error = outcome) }
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val stored = state.value.stored ?: return
        viewModelScope.launch {
            repo.delete(stored.id)
            state.update { it.copy(deleted = true) }
            onDone()
        }
    }

    fun dismissError() = state.update { it.copy(error = null) }

    fun plainText(): String = state.value.stored?.let(AnalysisFormatter::toPlainText).orEmpty()
}

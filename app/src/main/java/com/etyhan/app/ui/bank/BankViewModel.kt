package com.etyhan.app.ui.bank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etyhan.app.AppContainer
import com.etyhan.app.data.AnalysisRepository
import com.etyhan.app.data.StoredAnalysis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BankUiState(
    val items: List<StoredAnalysis> = emptyList(),
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val total: Int = 0,
)

private data class Filter(val query: String, val favoritesOnly: Boolean)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class BankViewModel(private val container: AppContainer) : ViewModel() {

    private val repo: AnalysisRepository = container.repository
    private val filter = MutableStateFlow(Filter("", false))

    private val items = filter
        .debounce(180)
        .flatMapLatest { f ->
            when {
                f.query.isNotBlank() -> repo.search(f.query.trim())
                f.favoritesOnly -> repo.observeFavorites()
                else -> repo.observeAll()
            }
        }

    val uiState: StateFlow<BankUiState> = combine(items, filter) { list, f ->
        BankUiState(items = list, query = f.query, favoritesOnly = f.favoritesOnly, total = list.size)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BankUiState(),
    )

    fun setQuery(q: String) {
        filter.value = filter.value.copy(query = q)
    }

    fun setFavoritesOnly(only: Boolean) {
        filter.value = filter.value.copy(favoritesOnly = only)
    }

    fun toggleFavorite(item: StoredAnalysis) {
        viewModelScope.launch { repo.setFavorite(item.id, !item.favorite) }
    }

    fun delete(item: StoredAnalysis) {
        viewModelScope.launch { repo.delete(item.id) }
    }
}

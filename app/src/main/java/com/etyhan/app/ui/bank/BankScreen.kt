package com.etyhan.app.ui.bank

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.etyhan.app.AppContainer
import com.etyhan.app.ai.AnalysisFormatter
import com.etyhan.app.data.Origin
import com.etyhan.app.data.StoredAnalysis
import com.etyhan.app.ui.components.ConfirmDialog
import com.etyhan.app.ui.components.EmptyState
import com.etyhan.app.ui.components.EtyCard
import com.etyhan.app.ui.components.OriginBadge
import com.etyhan.app.ui.components.ScreenTitle
import com.etyhan.app.ui.relativeTime

@Composable
fun BankScreen(
    container: AppContainer,
    onOpenResult: (Long) -> Unit,
) {
    val vm: BankViewModel = viewModel(
        factory = viewModelFactory { initializer { BankViewModel(container) } },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<StoredAnalysis?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            title = "词库",
            subtitle = "查过的词与句子都会留在这里，可离线翻看",
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            placeholder = { Text("在词库里搜韩语、中文或汉字…") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "清空搜索")
                    }
                }
            },
        )

        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterPill("全部", !state.favoritesOnly) { vm.setFavoritesOnly(false) }
            FilterPill("收藏", state.favoritesOnly) { vm.setFavoritesOnly(true) }
            Spacer(Modifier.weight(1f))
            if (state.items.isNotEmpty()) {
                Text(
                    "共 ${state.items.size} 条",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                EmptyState(
                    title = if (state.query.isNotBlank()) "没有匹配的记录" else "词库还是空的",
                    body = if (state.query.isNotBlank()) {
                        "换个关键词试试，或只搜韩语原文。"
                    } else {
                        "去首页查第一个词吧。每次分析都会自动存下来，不额外花 token。"
                    },
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    BankRow(
                        item = item,
                        onClick = { onOpenResult(item.id) },
                        onToggleFavorite = { vm.toggleFavorite(item) },
                        onDelete = { pendingDelete = item },
                    )
                }
            }
        }
    }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "删除「${item.query.take(20)}」？",
            text = "删除后无法恢复，也不再出现在离线词库里。",
            confirmLabel = "删除",
            onConfirm = {
                vm.delete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun FilterPill(text: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun BankRow(
    item: StoredAnalysis,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    EtyCard(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.query,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    AnalysisFormatter.snippet(item.analysis).ifBlank { "（无译文）" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    relativeTime(item.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val mix = AnalysisFormatter.originMix(item.analysis)
                if (mix.isNotEmpty()) {
                    Spacer(Modifier.size(3.dp))
                    OriginBadge(mix.first().first)
                }
            }
        }
        Spacer(Modifier.size(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.model.ifBlank { "AI" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = if (item.favorite) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (item.favorite) "取消收藏" else "收藏",
                    modifier = Modifier.size(18.dp),
                    tint = if (item.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(2.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

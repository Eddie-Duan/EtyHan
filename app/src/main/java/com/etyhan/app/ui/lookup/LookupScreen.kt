package com.etyhan.app.ui.lookup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.etyhan.app.AppContainer
import com.etyhan.app.data.AnalyzeOutcome
import com.etyhan.app.data.StoredAnalysis
import com.etyhan.app.ui.components.BrandMark
import com.etyhan.app.ui.components.ErrorBlock
import com.etyhan.app.ui.components.EtyCard
import com.etyhan.app.ui.components.RawTextDialog
import com.etyhan.app.ui.components.SectionHeader
import com.etyhan.app.ui.components.TagPill
import com.etyhan.app.ui.relativeTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LookupScreen(
    container: AppContainer,
    prefill: String,
    onOpenResult: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: LookupViewModel = viewModel(
        factory = viewModelFactory { initializer { LookupViewModel(container) } },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    var rawDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let { id ->
            vm.consumeNavigation()
            onOpenResult(id)
        }
    }
    LaunchedEffect(prefill) {
        if (prefill.isNotBlank()) vm.autoAnalyze(prefill)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 42)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("EtyHan", style = MaterialTheme.typography.headlineMedium)
                }
                ProviderPill(
                    text = if (state.ready) "${state.providerName} · ${state.settings.model}" else "未配置模型",
                    warn = !state.ready,
                    onClick = onOpenSettings,
                )
            }
        }

        item {
            EtyCard {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = vm::setInput,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入韩语单词或句子…") },
                    minLines = 2,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.input.isNotEmpty()) {
                            IconButton(onClick = { vm.setInput("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "清空")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { vm.analyze() }),
                    supportingText = {
                        Text(
                            text = "深度：${state.settings.depth.label} · ${state.settings.depth.hint}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagPill("韩 → 中")
                    Spacer(Modifier.width(6.dp))
                    TagPill("同词再查不耗 token", container = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { vm.analyze() },
                        enabled = state.input.isNotBlank() && !state.loading,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("分析")
                    }
                }
            }
        }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("학생", "먹다", "안녕하세요", "학교에 갑니다", "한국어를 배우고 싶어요").forEach { s ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            vm.setInput(s)
                            vm.analyze()
                        },
                    ) {
                        Text(
                            s,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        if (!state.ready) {
            item {
                EtyCard(container = MaterialTheme.colorScheme.primaryContainer) {
                    Text("还差一步：配置模型", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "EtyHan 不内置任何模型额度。填入 DeepSeek / GLM 等任一服务的 API Key 即可开始，Key 只存在这台手机里。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onOpenSettings, shape = RoundedCornerShape(12.dp)) {
                        Text("去设置")
                    }
                }
            }
        }

        if (state.loading) {
            item {
                EtyCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("正在分析 · ${state.elapsedSec}s", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = vm::cancel) { Text("取消") }
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${state.providerName} / ${state.settings.model} 正在拆语法、查词源。通常 3~10 秒。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        state.error?.let { err ->
            item {
                ErrorBlock(
                    error = err,
                    onRetry = { vm.analyze(force = true) },
                    onSettings = onOpenSettings,
                    onShowRaw = { rawDialog = err.raw },
                    onDismiss = vm::dismissError,
                )
            }
        }

        if (state.recent.isNotEmpty()) {
            item {
                SectionHeader("最近查询", modifier = Modifier.padding(top = 6.dp))
            }
            items(state.recent, key = { it.id }) { item ->
                RecentRow(item) { onOpenResult(item.id) }
            }
        } else if (!state.loading) {
            item {
                IntroCard()
            }
        }
    }

    rawDialog?.let { raw ->
        RawTextDialog(title = "模型原始输出", text = raw, onDismiss = { rawDialog = null })
    }
}

@Composable
private fun ProviderPill(text: String, warn: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (warn) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (warn) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentRow(item: StoredAnalysis, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.query,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    com.etyhan.app.ai.AnalysisFormatter.snippet(item.analysis).ifBlank { "（无译文）" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    relativeTime(item.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.favorite) {
                    Text("★", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun IntroCard() {
    EtyCard {
        Text("为什么给韩语做“词源”", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        listOf(
            "韩语词汇约六成是汉字词：학생=學生、도서관=圖書館，中文母语者自带一半词汇量。",
            "汉字音有规律：학↔學、국↔國，记住对应关系就能一次记一串词。",
            "同形异义才是真正的坑：집 是“房子”不是“集”，신문 是“报纸”不是“新闻”。",
        ).forEach { line ->
            Row(Modifier.padding(vertical = 3.dp)) {
                Box(
                    Modifier
                        .padding(top = 7.dp)
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.width(8.dp))
                Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

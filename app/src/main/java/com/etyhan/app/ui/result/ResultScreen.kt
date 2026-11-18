package com.etyhan.app.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.etyhan.app.AppContainer
import com.etyhan.app.ai.AnalysisFormatter
import com.etyhan.app.data.Analysis
import com.etyhan.app.data.AppSettings
import com.etyhan.app.data.EtyItem
import com.etyhan.app.data.Origin
import com.etyhan.app.data.PosLabels
import com.etyhan.app.data.Segment
import com.etyhan.app.data.StoredAnalysis
import com.etyhan.app.ui.components.ConfirmDialog
import com.etyhan.app.ui.components.DividerLine
import com.etyhan.app.ui.components.EmptyState
import com.etyhan.app.ui.components.ErrorBlock
import com.etyhan.app.ui.components.EtyCard
import com.etyhan.app.ui.components.OriginBadge
import com.etyhan.app.ui.components.RawTextDialog
import com.etyhan.app.ui.components.SectionHeader
import com.etyhan.app.ui.components.SpeakButton
import com.etyhan.app.ui.components.TagPill
import com.etyhan.app.ui.components.copyToClipboard
import com.etyhan.app.ui.components.originColors
import com.etyhan.app.ui.components.shareText
import com.etyhan.app.ui.formatDuration
import com.etyhan.app.ui.formatTokens
import com.etyhan.app.ui.relativeTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    container: AppContainer,
    id: Long,
    onBack: () -> Unit,
    onWordClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: ResultViewModel = viewModel(
        key = "result-$id",
        factory = viewModelFactory { initializer { ResultViewModel(container, id) } },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val settings by container.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val context = LocalContext.current

    var showJson by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    when {
        state.loading -> LoadingPane()
        state.stored == null -> EmptyState(
            title = "找不到这条分析",
            body = "它可能已被删除。回到首页重新查一次吧。",
            modifier = Modifier.fillMaxSize(),
            action = { Button(onClick = onBack) { Text("返回") } },
        )
        else -> {
            val stored = state.stored!!
            val analysis = stored.analysis

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp),
            ) {
                item {
                    TopActions(
                        favorite = stored.favorite,
                        onBack = onBack,
                        onToggleFavorite = vm::toggleFavorite,
                        onShare = { shareText(context, vm.plainText()) },
                        onReanalyze = vm::reanalyze,
                        onShowJson = { showJson = true },
                        onDelete = { confirmDelete = true },
                    )
                }
                if (state.analyzing) {
                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                }
                state.error?.let { err ->
                    item {
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            ErrorBlock(
                                error = err,
                                onRetry = vm::reanalyze,
                                onSettings = onOpenSettings,
                                onShowRaw = null,
                                onDismiss = vm::dismissError,
                            )
                        }
                    }
                }
                item {
                    Hero(
                        query = stored.query,
                        analysis = analysis,
                        showRomanization = settings.showRomanization,
                        speaker = container.speaker,
                    )
                }
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        TranslationCard(analysis)
                    }
                }
                if (analysis.tips.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            TipsCard(analysis.tips)
                        }
                    }
                }
                item {
                    TabsAndBody(
                        analysis = analysis,
                        speaker = container.speaker,
                        onWordClick = onWordClick,
                    )
                }
                if (analysis.related.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            SectionHeader("相关词")
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                analysis.related.forEach { rel ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.clickable { onWordClick(rel.word) },
                                    ) {
                                        Text(
                                            "${rel.word}  ${rel.zh}",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Footer(
                        stored = stored,
                        speaker = container.speaker,
                        onReanalyze = vm::reanalyze,
                        onCopy = { copyToClipboard(context, vm.plainText(), "已复制分析全文") },
                        onShare = { shareText(context, vm.plainText()) },
                    )
                }
            }

            if (showJson) {
                RawTextDialog(
                    title = "本次分析的 JSON",
                    text = stored.rawJson.ifBlank { "（无）" },
                    onDismiss = { showJson = false },
                    onCopy = { copyToClipboard(context, stored.rawJson, "已复制 JSON") },
                )
            }
            if (confirmDelete) {
                ConfirmDialog(
                    title = "删除这条记录？",
                    text = "删除后不再出现在词库里，已收藏也会一并移除。",
                    confirmLabel = "删除",
                    onConfirm = {
                        confirmDelete = false
                        vm.delete { onBack() }
                    },
                    onDismiss = { confirmDelete = false },
                )
            }
        }
    }
}

@Composable
private fun LoadingPane() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun TopActions(
    favorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onReanalyze: () -> Unit,
    onShowJson: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (favorite) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = if (favorite) "取消收藏" else "收藏",
                tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Rounded.Share, contentDescription = "分享")
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("重新分析") },
                    leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                    onClick = { menu = false; onReanalyze() },
                )
                DropdownMenuItem(
                    text = { Text("查看 JSON") },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                    onClick = { menu = false; onShowJson() },
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hero(
    query: String,
    analysis: Analysis,
    showRomanization: Boolean,
    speaker: com.etyhan.app.tts.Speaker,
) {
    val big = query.length <= 12
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = query,
                style = if (big) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp),
            )
            SpeakButton(text = query, speaker = speaker, modifier = Modifier.size(44.dp))
        }
        if (showRomanization && analysis.romanization.isNotBlank()) {
            Text(
                analysis.romanization,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (analysis.level.isNotBlank()) TagPill(analysis.level)
            TagPill(if (analysis.isSentence) "句子" else "单词")
            if (analysis.segments.isNotEmpty()) TagPill("${analysis.segments.size} 个成分")
        }
    }
}

@Composable
private fun TranslationCard(analysis: Analysis) {
    EtyCard {
        Text(
            "译文",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = analysis.translation.ifBlank { "—" },
            style = MaterialTheme.typography.headlineSmall,
            lineHeight = 32.sp,
        )
        if (analysis.nuance.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            DividerLine()
            Spacer(Modifier.height(8.dp))
            Text(
                "语感：${analysis.nuance}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TipsCard(tips: List<String>) {
    EtyCard(container = MaterialTheme.colorScheme.secondaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "给中文母语者",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.height(6.dp))
        tips.forEach { tip ->
            Row(Modifier.padding(vertical = 2.dp)) {
                Text("·", color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(6.dp))
                Text(
                    tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

private data class ResultTab(val title: String, val badge: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TabsAndBody(
    analysis: Analysis,
    speaker: com.etyhan.app.tts.Speaker,
    onWordClick: (String) -> Unit,
) {
    val tabs = remember(analysis) {
        buildList {
            if (analysis.segments.isNotEmpty()) add(ResultTab("分段", analysis.segments.size.toString()))
            if (analysis.grammar != null) add(ResultTab("语法", analysis.grammar.points.size.toString()))
            val etyCount = (analysis.etymology?.items?.size ?: 0) + (analysis.etymology?.cognates?.size ?: 0)
            if (etyCount > 0 || !analysis.etymology?.summary.isNullOrBlank()) add(ResultTab("词源", etyCount.toString()))
            if (analysis.examples.isNotEmpty()) add(ResultTab("例句", analysis.examples.size.toString()))
        }
    }
    if (tabs.isEmpty()) return

    var selected by remember(analysis) { mutableIntStateOf(0) }
    val index = selected.coerceIn(0, tabs.lastIndex)

    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEachIndexed { i, tab ->
                val active = i == index
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { selected = i },
                ) {
                    Text(
                        "${tab.title} ${tab.badge}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        when (tabs[index].title) {
            "分段" -> SegmentsBody(analysis, onWordClick)
            "语法" -> GrammarBody(analysis)
            "词源" -> EtymologyBody(analysis, speaker, onWordClick)
            else -> ExamplesBody(analysis, speaker)
        }
    }
}

@Composable
private fun SegmentsBody(analysis: Analysis, onWordClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        analysis.segments.forEachIndexed { i, seg ->
            if (i > 0) DividerLine()
            SegmentRow(seg, onWordClick)
        }
        if (analysis.segments.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "点按任一词可单独查它的词源；语尾和助词也可以点。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SegmentRow(seg: Segment, onWordClick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = seg.ko.isNotBlank()) { onWordClick(seg.ko) }
            .padding(vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                seg.ko,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
            )
            if (seg.rom.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    seg.rom,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            if (seg.pos.isNotBlank()) {
                TagPill(PosLabels.zh(seg.pos).ifBlank { seg.pos })
            }
            if (seg.origin.isNotBlank() && Origin.of(seg.origin) != Origin.UNKNOWN) {
                Spacer(Modifier.width(4.dp))
                OriginBadge(Origin.of(seg.origin))
            }
        }
        if (seg.zh.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(seg.zh, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(5.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (seg.tag.isNotBlank()) TagPill(seg.tag)
            if (seg.base.isNotBlank()) TagPill("原形 ${seg.base}")
            if (seg.hanja.isNotBlank()) {
                val (bg, fg) = originColors(Origin.SINO)
                TagPill("漢 ${seg.hanja}", container = bg, contentColor = fg)
            }
        }
        if (seg.note.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                seg.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GrammarBody(analysis: Analysis) {
    val g = analysis.grammar ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EtyCard {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (g.type.isNotBlank()) TagPill(g.type)
                if (g.tense.isNotBlank()) TagPill(g.tense)
                if (g.speech.isNotBlank()) TagPill(g.speech)
            }
            if (g.structure.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                SectionHeader("句子成分")
                Spacer(Modifier.height(4.dp))
                Text(g.structure, style = MaterialTheme.typography.bodyMedium)
            }
            if (g.pattern.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                SectionHeader("句型")
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        g.pattern,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }

        g.points.forEachIndexed { i, p ->
            EtyCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (i + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(p.title, style = MaterialTheme.typography.titleMedium)
                }
                if (p.desc.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(p.desc, style = MaterialTheme.typography.bodyMedium)
                }
                if (p.tip.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(p.tip, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EtymologyBody(
    analysis: Analysis,
    speaker: com.etyhan.app.tts.Speaker,
    onWordClick: (String) -> Unit,
) {
    val ety = analysis.etymology ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val mix = AnalysisFormatter.originMix(analysis)
        if (mix.isNotEmpty()) {
            EtyCard {
                SectionHeader("词源构成")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                ) {
                    mix.forEach { (origin, count) ->
                        val (_, fg) = originColors(origin)
                        Box(
                            Modifier
                                .weight(count.toFloat())
                                .fillMaxSize()
                                .background(fg),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    mix.forEach { (origin, count) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (_, fg) = originColors(origin)
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(fg),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${origin.label} $count",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (ety.summary.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(ety.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        ety.items.forEach { item ->
            EtyItemCard(item, speaker, onWordClick)
        }

        if (ety.cognates.isNotEmpty()) {
            EtyCard {
                SectionHeader("韩汉对照")
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("韩语", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.1f))
                    Text("汉字", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.9f))
                    Text("中文", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                ety.cognates.forEach { c ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text(c.ko, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.1f))
                        Text(c.hanja, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.9f), fontFamily = FontFamily.Serif)
                        Text(c.zh, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                    if (c.note.isNotBlank()) {
                        Text(
                            c.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DividerLine()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EtyItemCard(
    item: EtyItem,
    speaker: com.etyhan.app.tts.Speaker,
    onWordClick: (String) -> Unit,
) {
    EtyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.word,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.clickable { onWordClick(item.word) },
            )
            if (item.hanja.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    item.hanja,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(8.dp))
            SpeakButton(text = item.word, speaker = speaker)
            Spacer(Modifier.weight(1f))
            OriginBadge(Origin.of(item.origin))
        }
        if (item.rom.isNotBlank() || item.meaning.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(item.rom.ifBlank { null }, item.meaning.ifBlank { null }).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.chars.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.chars.forEach { ch ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                ch.hanja.ifBlank { ch.zh },
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(ch.ko, style = MaterialTheme.typography.labelSmall)
                            if (ch.zh.isNotBlank()) {
                                Text(
                                    ch.zh,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (item.english.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("外来原词：${item.english}", style = MaterialTheme.typography.bodySmall)
        }
        if (item.tip.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(10.dp),
            ) {
                Icon(
                    Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(item.tip, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ExamplesBody(analysis: Analysis, speaker: com.etyhan.app.tts.Speaker) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        analysis.examples.forEach { ex ->
            EtyCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        ex.ko,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.weight(1f),
                    )
                    SpeakButton(text = ex.ko, speaker = speaker)
                }
                if (ex.zh.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(ex.zh, style = MaterialTheme.typography.bodyMedium)
                }
                if (ex.point.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "语法点：${ex.point}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Footer(
    stored: StoredAnalysis,
    speaker: com.etyhan.app.tts.Speaker,
    onReanalyze: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        DividerLine()
        Spacer(Modifier.height(12.dp))
        val meta = buildString {
            append(stored.model.ifBlank { "AI" })
            if (stored.tokensIn + stored.tokensOut > 0) {
                append(" · ").append(formatTokens(stored.tokensIn)).append("+").append(formatTokens(stored.tokensOut)).append(" tokens")
            }
            append(" · ").append(formatDuration(stored.elapsedMs))
            append(" · ").append(relativeTime(stored.updatedAt))
        }
        Text(
            meta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCopy, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("复制")
            }
            OutlinedButton(onClick = onShare, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("分享")
            }
            TextButton(onClick = onReanalyze) { Text("重新分析") }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "译文与分析由你配置的模型生成，可能有误。词源以语言学结论为准，重要场合请核对。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

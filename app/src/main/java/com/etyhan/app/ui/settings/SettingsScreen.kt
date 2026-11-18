package com.etyhan.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.etyhan.app.AppContainer
import com.etyhan.app.BuildConfig
import com.etyhan.app.data.Depth
import com.etyhan.app.data.Providers
import com.etyhan.app.ui.components.ConfirmDialog
import com.etyhan.app.ui.components.DividerLine
import com.etyhan.app.ui.components.EtyCard
import com.etyhan.app.ui.components.RawTextDialog
import com.etyhan.app.ui.components.ScreenTitle
import com.etyhan.app.ui.components.SectionHeader
import com.etyhan.app.ui.components.copyToClipboard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(container: AppContainer) {
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container) } },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val s = state.settings
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var modelMenu by remember { mutableStateOf(false) }

    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenTitle(title = "设置", subtitle = "模型服务 · 分析口径 · 数据")

        // ---------- 模型服务 ----------
        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EtyCard {
                SectionHeader("模型服务")
                Spacer(Modifier.height(6.dp))
                Text(
                    "全部使用 OpenAI 兼容接口。选一个，填 Key 就能用；Key 只保存在本机。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Providers.all.forEach { p ->
                        ProviderChip(
                            name = p.name,
                            selected = p.id == s.providerId,
                            onClick = { vm.selectProvider(p.id) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    state.provider.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.provider.keyUrl.isNotBlank()) {
                    TextButton(
                        onClick = { open(state.provider.keyUrl) },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    ) {
                        Text("去 ${state.provider.name} 申请 Key", style = MaterialTheme.typography.labelMedium)
                        Icon(
                            Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(14.dp),
                        )
                    }
                }

                if (state.provider.needsKey) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = vm::setApiKey,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key") },
                        placeholder = { Text("sk-…") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (state.keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = vm::toggleKeyVisible) {
                                Icon(
                                    imageVector = if (state.keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (state.keyVisible) "隐藏 Key" else "显示 Key",
                                )
                            }
                        },
                        supportingText = {
                            Text(
                                "存在应用私有目录（Android Keystore 加密），不会上传到除模型服务之外的任何地方。",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = s.baseUrl,
                    onValueChange = vm::setBaseUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    placeholder = { Text("https://api.deepseek.com/v1") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = s.model,
                        onValueChange = vm::setModel,
                        modifier = Modifier.weight(1f),
                        label = { Text("模型名") },
                        placeholder = { Text("deepseek-chat") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    if (state.provider.models.isNotEmpty()) {
                        IconButton(onClick = { modelMenu = true }) {
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "选择模型")
                        }
                        DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                            state.provider.models.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        vm.setModel(m)
                                        modelMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = vm::testConnection,
                        enabled = !state.testing,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (state.testing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (state.testing) "测试中…" else "测试连接")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "发一条 2 字符的请求，几乎不花钱",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.testMessage?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = if (state.testOk) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                        contentColor = if (state.testOk) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                        )
                    }
                }
            }

            // ---------- 分析偏好 ----------
            EtyCard {
                SectionHeader("分析偏好")
                Spacer(Modifier.height(8.dp))
                Text(
                    "详细度直接决定 token 花多少。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Depth.entries.forEach { d ->
                        SelectChip(
                            text = d.label,
                            selected = s.depth == d,
                            onClick = { vm.setDepth(d) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    s.depth.hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(12.dp))
                DividerLine()
                SwitchRow(
                    title = "要求 JSON 模式",
                    subtitle = "服务端支持时能显著降低格式出错率；不支持会自动重试一次",
                    checked = s.jsonMode,
                    onChange = vm::setJsonMode,
                )
                SwitchRow(
                    title = "显示罗马音",
                    subtitle = "给还不熟悉韩文字母的阶段用",
                    checked = s.showRomanization,
                    onChange = vm::setShowRomanization,
                )
                SwitchRow(
                    title = "韩语朗读",
                    subtitle = "用系统 TTS 读单词和例句，离线可用",
                    checked = s.ttsEnabled,
                    onChange = vm::setTtsEnabled,
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = s.extraInstruction,
                    onValueChange = vm::setExtraInstruction,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("附加要求（可选）") },
                    placeholder = { Text("例如：每个词都补充 TOPIK 等级；用例句都用敬语；解释尽量短") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Text(
                            "会追加到系统提示词，仍然要求返回同一份 JSON。",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }

            // ---------- 分析模板 ----------
            EtyCard {
                SectionHeader("分析模板")
                Spacer(Modifier.height(6.dp))
                Text(
                    "一次性写完的分析框架：翻译、语法分段、语法分析、词源（汉字/固有/外来）、例句、给中文母语者的提示。模型只负责填空。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = vm::previewPrompt, shape = RoundedCornerShape(12.dp)) {
                        Text("预览 Prompt")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "schema-1 · 稳定前缀，利于上下文缓存",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---------- 数据 ----------
            EtyCard {
                SectionHeader("数据")
                Spacer(Modifier.height(6.dp))
                Text(
                    "词库共 ${state.historyCount} 条，包含历史查询与收藏。数据只在本机数据库里。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(
                        onClick = {
                            vm.refreshCount()
                            confirmClear = true
                        },
                    ) { Text("清空全部记录") }
                }
            }

            // ---------- 关于 ----------
            EtyCard {
                SectionHeader("关于 EtyHan")
                Spacer(Modifier.height(6.dp))
                Text(
                    "版本 ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "EtyHan 是给中文母语者的韩语精读工具：一次请求拿到译文、逐词语法分段、语法讲解和词源（汉字词逐字对照）。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "结构与功能参考了 Soju Platform（MIT）：词汇/语法/例句/朗读的组织方式。EtyHan 改为“单次分析 + 卡片呈现”，并加入词源层。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { open("https://github.com/orbingol/soju-platform") },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("github.com/orbingol/soju-platform", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "EtyHan 以 MIT 协议开源。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(32.dp))
    }

    state.promptPreview?.let { preview ->
        RawTextDialog(
            title = "发给模型的 Prompt",
            text = preview,
            onDismiss = vm::closePreview,
            onCopy = { copyToClipboard(context, preview, "已复制 Prompt") },
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            title = "清空全部记录？",
            text = "历史查询和收藏都会删除，无法恢复。API Key 不受影响。",
            confirmLabel = "清空",
            onConfirm = {
                vm.clearHistory()
                confirmClear = false
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun ProviderChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(3.dp))
            }
            Text(name, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

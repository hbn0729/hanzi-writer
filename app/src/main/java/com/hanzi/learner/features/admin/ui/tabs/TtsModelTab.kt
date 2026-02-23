package com.hanzi.learner.features.admin.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hanzi.learner.R
import com.hanzi.learner.speech.contract.TtsEngineInfo
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelUiItem
import com.hanzi.learner.speech.model.TtsSettings

@Composable
fun TtsModelTab(
    modifier: Modifier = Modifier,
    models: List<TtsModelUiItem>,
    activeModelId: String?,
    error: String?,
    isPlayingPreview: Boolean,
    currentlyPlayingModelId: String?,
    settings: TtsSettings,
    currentEngine: TtsEngineInfo?,
    availableEngines: List<TtsEngineInfo>,
    isChineseSupported: Boolean,
    onDownload: (String) -> Unit,
    onCancel: (String) -> Unit,
    onEnable: (String) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onPreview: (String) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSettingsPreview: () -> Unit,
    onEngineChange: (String) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(text = "语音设置", style = MaterialTheme.typography.headlineSmall) }

        if (error != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onClearError) { Text(text = "清除") }
                    }
                }
            }
        }

        item {
            TtsSettingsCard(
                settings = settings,
                currentEngine = currentEngine,
                availableEngines = availableEngines,
                isChineseSupported = isChineseSupported,
                onSpeechRateChange = onSpeechRateChange,
                onPitchChange = onPitchChange,
                onPreview = onSettingsPreview,
                onEngineChange = onEngineChange,
            )
        }

        item {
            Text(
                text = "语音模型",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        activeModelId?.let { activeId ->
            val activeModel = models.find { it.info.id == activeId }
            activeModel?.let { model ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "当前启用",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = model.info.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }

        items(models, key = { it.info.id }) { model ->
            TtsModelCard(
                model = model,
                isPlayingPreview = isPlayingPreview && currentlyPlayingModelId == model.info.id,
                onDownload = onDownload,
                onCancel = onCancel,
                onEnable = onEnable,
                onPause = onPause,
                onResume = onResume,
                onPreview = onPreview,
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onBack) { Text(text = "返回") }
        }
    }
}

@Composable
private fun TtsSettingsCard(
    settings: TtsSettings,
    currentEngine: TtsEngineInfo?,
    availableEngines: List<TtsEngineInfo>,
    isChineseSupported: Boolean,
    onSpeechRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onPreview: () -> Unit,
    onEngineChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "语音参数",
                style = MaterialTheme.typography.titleMedium,
            )

            if (availableEngines.isNotEmpty()) {
                Column {
                    Text(
                        text = "TTS引擎",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    EngineSelector(
                        engines = availableEngines,
                        currentEngine = currentEngine,
                        onEngineSelected = onEngineChange,
                    )
                }
            }

            currentEngine?.let { engine ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "中文支持",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (engine.isChineseSupported) "✓ 支持" else "✗ 不支持",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (engine.isChineseSupported) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (!isChineseSupported) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 提示",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text = "当前引擎不支持中文语音。请选择支持中文的引擎，或安装\"科大讯飞语音引擎\"获得更好的中文语音效果。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "语速",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = String.format("%.1fx", settings.speechRate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = settings.speechRate,
                    onValueChange = onSpeechRateChange,
                    valueRange = TtsSettings.MIN_SPEECH_RATE..TtsSettings.MAX_SPEECH_RATE,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "音调",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = String.format("%.1f", settings.pitch),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = settings.pitch,
                    onValueChange = onPitchChange,
                    valueRange = TtsSettings.MIN_PITCH..TtsSettings.MAX_PITCH,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedButton(
                onClick = onPreview,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_volume),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(text = "试听效果")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngineSelector(
    engines: List<TtsEngineInfo>,
    currentEngine: TtsEngineInfo?,
    onEngineSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = currentEngine?.label ?: currentEngine?.name ?: "选择引擎",
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            engines.forEach { engine ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text = engine.label)
                            if (!engine.isChineseSupported) {
                                Text(
                                    text = "不支持中文",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                    onClick = {
                        onEngineSelected(engine.packageName)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TtsModelCard(
    model: TtsModelUiItem,
    isPlayingPreview: Boolean,
    onDownload: (String) -> Unit,
    onCancel: (String) -> Unit,
    onEnable: (String) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onPreview: (String) -> Unit,
) {
    val isSystemTts = model.info.isSystemTts

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (model.isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.info.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (model.info.description.isNotBlank()) {
                        Text(
                            text = model.info.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                when (val state = model.downloadState) {
                    is TtsModelDownloadState.NotDownloaded -> {
                        if (isSystemTts) {
                            StatusChip(text = "系统语音", isActive = false)
                        } else {
                            StatusChip(text = "未下载", isActive = false)
                        }
                    }
                    is TtsModelDownloadState.Downloading -> {
                        StatusChip(
                            text = "下载中 ${(state.progress * 100).toInt()}%",
                            isActive = true,
                        )
                    }
                    is TtsModelDownloadState.Paused -> {
                        StatusChip(
                            text = "已暂停 ${(state.progress * 100).toInt()}%",
                            isActive = false,
                        )
                    }
                    is TtsModelDownloadState.Downloaded -> {
                        if (model.isSelected) {
                            StatusChip(text = "✓ 已启用", isActive = true)
                        } else {
                            StatusChip(text = "已下载", isActive = false)
                        }
                    }
                    is TtsModelDownloadState.Error -> {
                        StatusChip(text = "错误", isActive = false, isError = true)
                    }
                }
            }

            when (val state = model.downloadState) {
                is TtsModelDownloadState.Downloading -> {
                    LinearProgressIndicator(
                        progress = state.progress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is TtsModelDownloadState.Paused -> {
                    LinearProgressIndicator(
                        progress = state.progress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {}
            }

            if (model.downloadState is TtsModelDownloadState.Error) {
                val errorState = model.downloadState as TtsModelDownloadState.Error
                Text(
                    text = errorState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onPreview(model.info.id) },
                    enabled = !isPlayingPreview,
                ) {
                    Icon(
                            painter = painterResource(
                                id = if (isPlayingPreview) R.drawable.ic_stop else R.drawable.ic_volume,
                            ),
                            contentDescription = if (isPlayingPreview) "停止试听" else "试听",
                        )
                }

                when (val state = model.downloadState) {
                    is TtsModelDownloadState.NotDownloaded -> {
                        if (!isSystemTts) {
                            Button(onClick = { onDownload(model.info.id) }) {
                                Text(text = "下载")
                            }
                        } else {
                            if (model.isSelected) {
                                OutlinedButton(enabled = false, onClick = {}) {
                                    Text(text = "已启用")
                                }
                            } else {
                                Button(onClick = { onEnable(model.info.id) }) {
                                    Text(text = "启用")
                                }
                            }
                        }
                    }
                    is TtsModelDownloadState.Downloading -> {
                        OutlinedButton(onClick = { onPause(model.info.id) }) {
                            Text(text = "暂停")
                        }
                        OutlinedButton(onClick = { onCancel(model.info.id) }) {
                            Text(text = "取消")
                        }
                    }
                    is TtsModelDownloadState.Paused -> {
                        Button(onClick = { onResume(model.info.id) }) {
                            Text(text = "继续")
                        }
                        OutlinedButton(onClick = { onCancel(model.info.id) }) {
                            Text(text = "取消")
                        }
                    }
                    is TtsModelDownloadState.Downloaded -> {
                        if (model.isSelected) {
                            OutlinedButton(enabled = false, onClick = {}) {
                                Text(text = "已启用")
                            }
                        } else {
                            Button(onClick = { onEnable(model.info.id) }) {
                                Text(text = "启用")
                            }
                        }
                        OutlinedButton(onClick = { onCancel(model.info.id) }) {
                            Text(text = "删除")
                        }
                    }
                    is TtsModelDownloadState.Error -> {
                        if (state.retryable) {
                            Button(onClick = { onDownload(model.info.id) }) {
                                Text(text = "重试")
                            }
                        }
                        OutlinedButton(onClick = { onCancel(model.info.id) }) {
                            Text(text = "取消")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    isActive: Boolean,
    isError: Boolean = false,
) {
    val containerColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isActive -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isActive -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

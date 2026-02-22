package com.hanzi.learner.features.admin.ui.tabs

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hanzi.learner.R
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelUiItem

@Composable
fun TtsModelTab(
    modifier: Modifier = Modifier,
    models: List<TtsModelUiItem>,
    activeModelId: String?,
    error: String?,
    isPlayingPreview: Boolean,
    currentlyPlayingModelId: String?,
    onDownload: (String) -> Unit,
    onCancel: (String) -> Unit,
    onEnable: (String) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onPreview: (String) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(text = "语音模型", style = MaterialTheme.typography.headlineSmall) }

        // Error message
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

        // Active model info
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

        // Model list
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
            // Title row with status
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

                // Status indicator
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

            // Progress bar for downloading/paused states
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

            // Error message
            if (model.downloadState is TtsModelDownloadState.Error) {
                val errorState = model.downloadState as TtsModelDownloadState.Error
                Text(
                    text = errorState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Preview button (always shown)
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

                // Action buttons based on state
                when (val state = model.downloadState) {
                    is TtsModelDownloadState.NotDownloaded -> {
                        if (!isSystemTts) {
                            Button(onClick = { onDownload(model.info.id) }) {
                                Text(text = "下载")
                            }
                        } else {
                            // System TTS: show enable button directly
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

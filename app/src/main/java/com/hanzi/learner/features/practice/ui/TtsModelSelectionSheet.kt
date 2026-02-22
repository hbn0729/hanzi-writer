package com.hanzi.learner.features.practice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hanzi.learner.R
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelUiItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsModelSelectionSheet(
    models: List<TtsModelUiItem>,
    isPlayingPreview: Boolean,
    currentlyPlayingModelId: String?,
    onModelSelected: (String) -> Unit,
    onPreview: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "选择语音模型",
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = "请选择一个语音模型用于练习时的发音。系统语音无需下载，其他模型需要先下载。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyColumn(
                modifier = Modifier.weight(weight = 1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(models, key = { it.info.id }) { model ->
                    TtsModelSelectionItem(
                        model = model,
                        isPlayingPreview = isPlayingPreview && currentlyPlayingModelId == model.info.id,
                        onSelect = { onModelSelected(model.info.id) },
                        onPreview = { onPreview(model.info.id) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(text = "跳过（稍后选择）")
            }
        }
    }
}

@Composable
private fun TtsModelSelectionItem(
    model: TtsModelUiItem,
    isPlayingPreview: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
) {
    val isSystemTts = model.info.isSystemTts

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemTts) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = model.info.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (isSystemTts) {
                        Text(
                            text = "无需下载",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (model.info.description.isNotBlank()) {
                    Text(
                        text = model.info.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (!isSystemTts) {
                    val sizeText = formatFileSize(model.info.fileSizeBytes)
                    Text(
                        text = "大小: $sizeText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPreview,
                    enabled = !isPlayingPreview,
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isPlayingPreview) R.drawable.ic_stop else R.drawable.ic_volume,
                        ),
                        contentDescription = if (isPlayingPreview) "停止" else "试听",
                    )
                }

                if (isSystemTts) {
                    Button(onClick = onSelect) {
                        Text(text = "选择")
                    }
                } else {
                    when (model.downloadState) {
                        is TtsModelDownloadState.NotDownloaded -> {
                            OutlinedButton(onClick = onSelect) {
                                Text(text = "下载并选择")
                            }
                        }
                        is TtsModelDownloadState.Downloading -> {
                            val progress = (model.downloadState.progress * 100).toInt()
                            OutlinedButton(enabled = false, onClick = {}) {
                                Text(text = "下载中 $progress%")
                            }
                        }
                        is TtsModelDownloadState.Paused -> {
                            OutlinedButton(onClick = onSelect) {
                                Text(text = "继续下载")
                            }
                        }
                        is TtsModelDownloadState.Downloaded -> {
                            Button(onClick = onSelect) {
                                Text(text = "选择")
                            }
                        }
                        is TtsModelDownloadState.Error -> {
                            OutlinedButton(onClick = onSelect) {
                                Text(text = "重试")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 100 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        bytes >= 10 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}

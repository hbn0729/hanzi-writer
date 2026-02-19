package com.hanzi.learner.features.admin.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanzi.learner.data.model.ExportOptions
import com.hanzi.learner.data.model.ImportMode

private enum class BackupImportType {
    STROKES,
    PHRASES,
    CURRICULUM,
}

@Composable
fun BackupTab(
    modifier: Modifier = Modifier,
    onExport: (ExportOptions, String) -> Unit,
    onImport: (ImportMode) -> Unit,
    importStatus: String?,
    onRequestImportStrokes: () -> Unit,
    onRequestImportPhrases: () -> Unit,
    onRequestImportCurriculum: (disableOthers: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var importType by remember { mutableStateOf(BackupImportType.STROKES) }
    var disableOthers by remember { mutableStateOf(true) }

    if (showImportDialog) {
        val example = when (importType) {
            BackupImportType.STROKES -> """
ZIP 内包含（来自 makemeahanzi 原始数据）：
- graphics.txt
- dictionary.txt

graphics.txt（每行一个 JSON，示例）：
{"character":"人","strokes":["M..."],"medians":[[[10,20],[11,21]]]}

dictionary.txt（每行一个 JSON，示例）：
{"character":"人","pinyin":["ren2"]}
""".trimIndent()

            BackupImportType.PHRASES -> """
JSON（对象映射，示例）：
{"人":["人民","人生"],"口":["人口"]}

约束：
- 每个 key 是单字
- 每条短语长度 ≤ 5
""".trimIndent()

            BackupImportType.CURRICULUM -> """
支持三种格式（示例）：

1) TXT（每行一个字）：
人
口
山

2) JSON 数组：
["人","口","山"]

3) CSV（至少一列 char）：
char
人
口
山
""".trimIndent()
        }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(text = "数据导入") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { importType = BackupImportType.STROKES },
                        ) {
                            RadioButton(selected = importType == BackupImportType.STROKES, onClick = { importType = BackupImportType.STROKES })
                            Text(text = "1. 笔画数据（makemeahanzi 原始）")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { importType = BackupImportType.PHRASES },
                        ) {
                            RadioButton(selected = importType == BackupImportType.PHRASES, onClick = { importType = BackupImportType.PHRASES })
                            Text(text = "2. 短语库数据")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { importType = BackupImportType.CURRICULUM },
                        ) {
                            RadioButton(selected = importType == BackupImportType.CURRICULUM, onClick = { importType = BackupImportType.CURRICULUM })
                            Text(text = "3. 课程字表数据")
                        }
                    }
                    Divider()
                    if (importType == BackupImportType.CURRICULUM) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(checked = disableOthers, onCheckedChange = { disableOthers = it })
                            Text(text = "仅启用导入字（其余全部禁用）")
                        }
                        Divider()
                    }
                    Text(text = "格式例子：")
                    Text(text = example)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (importType) {
                            BackupImportType.STROKES -> onRequestImportStrokes()
                            BackupImportType.PHRASES -> onRequestImportPhrases()
                            BackupImportType.CURRICULUM -> onRequestImportCurriculum(disableOthers)
                        }
                        showImportDialog = false
                    },
                ) { Text(text = "选择文件并导入") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text(text = "关闭") }
            },
        )
    }

    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(text = "备份") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showImportDialog = true }) { Text(text = "数据导入") }
                if (!importStatus.isNullOrBlank()) {
                    Text(text = "导入状态：$importStatus")
                }
                Button(onClick = { onExport(ExportOptions(), "hanzi-learner-backup.json") }) {
                    Text(text = "导出备份（全量）")
                }
                Button(onClick = { onImport(ImportMode.Replace) }) { Text(text = "导入备份（替换）") }
                OutlinedButton(onClick = { onImport(ImportMode.Merge) }) { Text(text = "导入备份（合并）") }
                Divider()
                OutlinedButton(
                    onClick = {
                        onExport(
                ExportOptions(
                                progress = true,
                                phraseOverrides = false,
                                disabledChars = false,
                                settings = false,
                            ),
                            "hanzi-progress.json",
                        )
                    },
                ) { Text(text = "仅导出学习进度") }
                OutlinedButton(
                    onClick = {
                        onExport(
                ExportOptions(
                                progress = false,
                                phraseOverrides = true,
                                disabledChars = false,
                                settings = false,
                            ),
                            "hanzi-phrase-overrides.json",
                        )
                    },
                ) { Text(text = "仅导出短语覆盖") }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onBack) { Text(text = "返回") }
        }
    }
}

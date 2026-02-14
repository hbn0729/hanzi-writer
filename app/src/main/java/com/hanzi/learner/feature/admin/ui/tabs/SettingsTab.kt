package com.hanzi.learner.feature.admin.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanzi.learner.feature.admin.model.AdminSettings

@Composable
fun SettingsTab(
    modifier: Modifier = Modifier,
    settings: AdminSettings?,
    onUpdateSettings: (AdminSettings) -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(text = "设置") }
        item {
            val s = settings ?: AdminSettings()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "提示阈值（错几次后显示提示笔画）：${s.hintAfterMisses}")
                RowWithButtons(
                    onMinus = { onUpdateSettings(s.copy(hintAfterMisses = (s.hintAfterMisses - 1).coerceAtLeast(0))) },
                    onPlus = { onUpdateSettings(s.copy(hintAfterMisses = (s.hintAfterMisses + 1).coerceAtMost(10))) },
                )

                Text(text = "到期抽取上限：${s.duePickLimit}")
                RowWithButtons(
                    onMinus = { onUpdateSettings(s.copy(duePickLimit = (s.duePickLimit - 10).coerceAtLeast(10))) },
                    onPlus = { onUpdateSettings(s.copy(duePickLimit = (s.duePickLimit + 10).coerceAtMost(500))) },
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "使用外部字库（导入笔画数据后启用）")
                    Switch(
                        checked = s.useExternalDataset,
                        onCheckedChange = { onUpdateSettings(s.copy(useExternalDataset = it)) },
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onBack) { Text(text = "返回") }
        }
    }
}

@Composable
private fun RowWithButtons(
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onMinus, modifier = Modifier.width(96.dp)) { Text(text = "-") }
            Button(onClick = onPlus, modifier = Modifier.width(96.dp)) { Text(text = "+") }
        }
    }
}

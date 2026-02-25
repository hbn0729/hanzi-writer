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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.hanzi.learner.features.admin.model.AdminSettings

@Composable
fun SettingsTab(
    modifier: Modifier = Modifier,
    settings: AdminSettings?,
    onUpdateSettings: (AdminSettings) -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text(text = "系统设置", style = MaterialTheme.typography.titleLarge) }
        
        item {
            val s = settings ?: AdminSettings()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "提示阈值（错 ${s.hintAfterMisses} 次后显示提示）", style = MaterialTheme.typography.titleMedium)
                        Text(text = "较低的数值能更快获得提示", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        RowWithButtons(
                            onMinus = { onUpdateSettings(s.copy(hintAfterMisses = (s.hintAfterMisses - 1).coerceAtLeast(0))) },
                            onPlus = { onUpdateSettings(s.copy(hintAfterMisses = (s.hintAfterMisses + 1).coerceAtMost(10))) },
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "每日复习抽取上限：${s.duePickLimit} 个", style = MaterialTheme.typography.titleMedium)
                        Text(text = "每天最多复习多少个字，避免压力过大", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        RowWithButtons(
                            onMinus = { onUpdateSettings(s.copy(duePickLimit = (s.duePickLimit - 10).coerceAtLeast(10))) },
                            onPlus = { onUpdateSettings(s.copy(duePickLimit = (s.duePickLimit + 10).coerceAtMost(500))) },
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "使用外部字库", style = MaterialTheme.typography.titleMedium)
                            Text(text = "导入自定义笔画数据后需开启此项", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = s.useExternalDataset,
                            onCheckedChange = { onUpdateSettings(s.copy(useExternalDataset = it)) },
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "自动朗读", style = MaterialTheme.typography.titleMedium)
                            Text(text = "进入练习时自动朗读汉字和词语", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = s.autoReadAloud,
                            onCheckedChange = { onUpdateSettings(s.copy(autoReadAloud = it)) },
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) { 
                Text(text = "返回上一页") 
            }
        }
    }
}

@Composable
private fun RowWithButtons(
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onMinus, 
            modifier = Modifier.width(80.dp)
        ) { 
            Text(text = "减少") 
        }
        Button(
            onClick = onPlus, 
            modifier = Modifier.width(80.dp)
        ) { 
            Text(text = "增加") 
        }
    }
}

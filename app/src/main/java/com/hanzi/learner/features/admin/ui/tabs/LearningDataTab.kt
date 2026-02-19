package com.hanzi.learner.features.admin.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanzi.learner.features.admin.model.AdminProgress
import com.hanzi.learner.character_writer.data.CharIndexItem

@Composable
fun LearningDataTab(
    modifier: Modifier = Modifier,
    indexItems: List<CharIndexItem>,
    allProgress: Map<String, AdminProgress>,
    onClearAll: () -> Unit,
    onClearProgress: () -> Unit,
    onResetSettings: () -> Unit,
    onCleanupOrphanProgress: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val indexSet = remember(indexItems) { indexItems.map { it.char }.toHashSet() }
    val orphanChars = remember(allProgress, indexSet) { allProgress.keys.filter { it !in indexSet }.sorted() }

    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(text = "学习数据") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClearAll) { Text(text = "清空所有学习数据") }
                Button(onClick = onClearProgress) { Text(text = "清空学习进度") }
                Button(onClick = onResetSettings) { Text(text = "重置设置") }
            }
        }

        item { Divider() }

        item {
            Text(text = "孤儿进度（进度存在但字库不存在）：${orphanChars.size}")
        }

        if (orphanChars.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onCleanupOrphanProgress(orphanChars) }) { Text(text = "清理孤儿进度") }
                }
            }
            items(orphanChars, key = { it }) { ch ->
                Text(text = ch)
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onBack) { Text(text = "返回") }
        }
    }
}

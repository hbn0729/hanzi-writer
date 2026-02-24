package com.hanzi.learner.features.admin.ui.tabs

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.hanzi.learner.features.admin.model.AdminProgress
import com.hanzi.learner.character_writer.data.CharIndexItem

@Composable
fun LearningDataTab(
    modifier: Modifier = Modifier,
    indexItems: List<CharIndexItem>,
    allProgress: Map<String, AdminProgress>,
    onClearAll: () -> Unit,
    onClearProgress: () -> Unit,
    onCleanupOrphanProgress: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val indexSet = remember(indexItems) { indexItems.map { it.char }.toHashSet() }
    val orphanChars = remember(allProgress, indexSet) { allProgress.keys.filter { it !in indexSet }.sorted() }

    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { 
            Text(text = "学习数据管理", style = MaterialTheme.typography.titleLarge) 
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "危险操作：数据清空后无法恢复！", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    
                    Button(
                        onClick = onClearAll,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) { 
                        Text(text = "清空所有学习数据") 
                    }
                    
                    Button(
                        onClick = onClearProgress,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha=0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) { 
                        Text(text = "仅清空学习进度") 
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Text(text = "孤立进度（进度存在但字库已被移除）：${orphanChars.size} 个", style = MaterialTheme.typography.titleMedium)
        }

        if (orphanChars.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onCleanupOrphanProgress(orphanChars) },
                            modifier = Modifier.fillMaxWidth()
                        ) { 
                            Text(text = "清理孤立进度") 
                        }
                        
                        Text(
                            text = "孤立字列表：" + orphanChars.joinToString("，"),
                            style = MaterialTheme.typography.bodySmall
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

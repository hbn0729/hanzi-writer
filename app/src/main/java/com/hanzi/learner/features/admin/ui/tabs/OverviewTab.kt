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
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanzi.learner.features.admin.model.AdminProgress
import com.hanzi.learner.features.admin.model.AdminStudyCount
import com.hanzi.learner.features.admin.ui.epochDayToText
import com.hanzi.learner.character_writer.data.CharIndexItem
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.hanzi.learner.app.theme.claymorphism
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box

@Composable
fun OverviewTab(
    modifier: Modifier = Modifier,
    indexItems: List<CharIndexItem>,
    disabledChars: Set<String>,
    learnedCount: Int,
    dueCount: Int,
    phraseOverrideCount: Int,
    topWrong: List<AdminProgress>,
    dueProgress: List<AdminProgress>,
    studyCounts: List<AdminStudyCount>,
    onMarkDueToday: (List<String>) -> Unit,
    onResetProgress: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val totalChars = indexItems.size
    val disabledCount = disabledChars.size
    val enabledCount = (totalChars - disabledCount).coerceAtLeast(0)
    val unlearnedCount = (totalChars - learnedCount).coerceAtLeast(0)

    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(text = "仪表盘", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "字库总字数：$totalChars（启用 $enabledCount / 禁用 $disabledCount）", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "学习状态：已学 $learnedCount / 未学 $unlearnedCount", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "今日到期复习：$dueCount", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "短语覆盖条数：$phraseOverrideCount", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(text = "到期复习（最多显示50个）", style = MaterialTheme.typography.titleMedium)
        }
        items(dueProgress.take(50), key = { "due_${it.char}" }) { p ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = p.char, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = { onMarkDueToday(listOf(p.char)) }) { Text(text = "今天复习") }
                        OutlinedButton(onClick = { onResetProgress(listOf(p.char)) }) { Text(text = "清零") }
                    }
                    Text(
                        text = "下次复习=${epochDayToText(p.nextDueDay)}，间隔=${p.intervalDays}天\n正确=${p.correctCount}，错误笔画=${p.wrongCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(text = "易错字（最多显示20个）", style = MaterialTheme.typography.titleMedium)
        }
        items(topWrong.take(20), key = { "wrong_${it.char}" }) { p ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = p.char, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "错误笔画=${p.wrongCount}\n正确=${p.correctCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(text = "最近学习记录（30天内）", style = MaterialTheme.typography.titleMedium)
        }
        items(studyCounts, key = { it.day }) { row ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Text(
                    text = "${epochDayToText(row.day)}：${row.count}次练习",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) { 
                Text(text = "返回上一页") 
            }
        }
    }
}

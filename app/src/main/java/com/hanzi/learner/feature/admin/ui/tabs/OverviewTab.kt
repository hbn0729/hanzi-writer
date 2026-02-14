package com.hanzi.learner.feature.admin.ui.tabs

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
import com.hanzi.learner.feature.admin.model.AdminProgress
import com.hanzi.learner.feature.admin.model.AdminStudyCount
import com.hanzi.learner.feature.admin.ui.epochDayToText
import com.hanzi.learner.hanzi.data.CharIndexItem

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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "仪表盘")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "字库总字数：$totalChars（启用 $enabledCount / 禁用 $disabledCount）")
                Text(text = "学习状态：已学 $learnedCount / 未学 $unlearnedCount")
                Text(text = "今日到期复习：$dueCount")
                Text(text = "短语覆盖条数：$phraseOverrideCount")
            }
        }

        item { Divider() }

        item {
            Text(text = "到期复习列表（最多显示 50）")
        }
        items(dueProgress.take(50), key = { "due_${it.char}" }) { p ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = p.char)
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { onMarkDueToday(listOf(p.char)) }) { Text(text = "今天复习") }
                OutlinedButton(onClick = { onResetProgress(listOf(p.char)) }) { Text(text = "清零") }
            }
            Text(
                text = "下次复习日=${epochDayToText(p.nextDueDay)} 间隔=${p.intervalDays}天 正确=${p.correctCount} 错误笔画=${p.wrongCount}",
            )
            Divider()
        }

        item {
            Text(text = "易错 Top（最多显示 20）")
        }
        items(topWrong.take(20), key = { "wrong_${it.char}" }) { p ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = p.char)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "错误笔画=${p.wrongCount} 正确=${p.correctCount}")
            }
            Divider()
        }

        item {
            Text(text = "最近学习（最多显示 30 天）")
        }
        items(studyCounts, key = { it.day }) { row ->
            Text(text = "${epochDayToText(row.day)}：${row.count}")
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onBack) { Text(text = "返回") }
        }
    }
}

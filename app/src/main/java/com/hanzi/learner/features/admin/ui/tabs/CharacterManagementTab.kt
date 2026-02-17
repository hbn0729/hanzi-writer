package com.hanzi.learner.features.admin.ui.tabs

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanzi.learner.features.admin.model.AdminProgress
import com.hanzi.learner.features.admin.ui.epochDayToText
import com.hanzi.learner.character_writer.data.CharIndexItem

private enum class CharFilterMode {
    ALL,
    DUE,
    LEARNED,
    UNLEARNED,
    DISABLED,
}

@Composable
fun CharacterManagementTab(
    modifier: Modifier = Modifier,
    indexItems: List<CharIndexItem>,
    disabledChars: Set<String>,
    allProgress: Map<String, AdminProgress>,
    todayEpochDay: Long,
    selectedChar: String?,
    selectedItem: CharIndexItem?,
    progress: AdminProgress?,
    overridePhrases: List<String>,
    newPhrase: String,
    onNewPhraseChange: (String) -> Unit,
    onSelectChar: (String) -> Unit,
    onToggleEnabled: (char: String, enabled: Boolean) -> Unit,
    onSavePhraseOverride: (char: String, phrases: List<String>) -> Unit,
    onDeletePhraseOverride: (char: String) -> Unit,
    onMarkDueToday: (List<String>) -> Unit,
    onResetProgress: (List<String>) -> Unit,
    onResetWrongCount: (List<String>) -> Unit,
    onBulkDisable: (List<String>) -> Unit,
    onBulkEnable: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    var searchText by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(CharFilterMode.ALL) }
    var selectedChars by remember { mutableStateOf<Set<String>>(emptySet()) }

    val filteredItems = remember(indexItems, disabledChars, allProgress, searchText, filterMode, todayEpochDay) {
        indexItems.asSequence().filter { item ->
            val ch = item.char
            val p = allProgress[ch]
            val isDisabled = ch in disabledChars
            val isLearned = p != null
            val isDue = p != null && p.nextDueDay <= todayEpochDay
            val searchOk = searchText.isBlank() ||
                ch.contains(searchText) ||
                item.pinyin.any { it.contains(searchText, ignoreCase = true) } ||
                item.strokeCount.toString() == searchText

            val filterOk = when (filterMode) {
                CharFilterMode.ALL -> true
                CharFilterMode.DUE -> isDue
                CharFilterMode.LEARNED -> isLearned
                CharFilterMode.UNLEARNED -> !isLearned
                CharFilterMode.DISABLED -> isDisabled
            }

            searchOk && filterOk
        }.toList()
    }

    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "字管理")
        }

        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "搜索（字 / 拼音 / 笔画数）") },
            )
        }

        item {
            FilterModeRow(
                filterMode = filterMode,
                onFilterModeChange = { filterMode = it },
            )
        }

        if (selectedChars.isNotEmpty()) {
            item {
                SelectedCharsActions(
                    selectedChars = selectedChars,
                    onBulkEnable = onBulkEnable,
                    onBulkDisable = onBulkDisable,
                    onMarkDueToday = onMarkDueToday,
                    onResetWrongCount = onResetWrongCount,
                    onResetProgress = onResetProgress,
                    onClearSelection = { selectedChars = emptySet() },
                )
            }
        }

        selectedItem?.let { item ->
            item {
                Divider()
            }
            item {
                SelectedCharacterDetails(
                    selectedItem = item,
                    disabledChars = disabledChars,
                    progress = progress,
                    overridePhrases = overridePhrases,
                    newPhrase = newPhrase,
                    onNewPhraseChange = onNewPhraseChange,
                    onToggleEnabled = onToggleEnabled,
                    onSavePhraseOverride = onSavePhraseOverride,
                    onDeletePhraseOverride = onDeletePhraseOverride,
                    onMarkDueToday = onMarkDueToday,
                    onResetWrongCount = onResetWrongCount,
                    onResetProgress = onResetProgress,
                )
            }
        }

        item { Divider() }
        item { Text(text = "字表（${filteredItems.size}）") }

        items(filteredItems, key = { it.char }) { item ->
            val ch = item.char
            val p = allProgress[ch]
            val enabled = ch !in disabledChars
            val isDue = p != null && p.nextDueDay <= todayEpochDay
            val statusText = buildList {
                add(if (enabled) "启用" else "禁用")
                add(if (p == null) "未学" else "已学")
                if (isDue) add("到期")
            }.joinToString(" / ")
            CharacterListItemRow(
                item = item,
                selectedChar = selectedChar,
                selectedChars = selectedChars,
                statusText = statusText,
                enabled = enabled,
                onSelectChar = onSelectChar,
                onToggleChecked = { isChecked ->
                    selectedChars = if (isChecked) selectedChars + ch else selectedChars - ch
                },
                onToggleEnabled = onToggleEnabled,
            )
            Divider()
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onBack) { Text(text = "返回") }
        }
    }
}

@Composable
private fun FilterModeRow(
    filterMode: CharFilterMode,
    onFilterModeChange: (CharFilterMode) -> Unit,
) {
    fun label(mode: CharFilterMode, text: String): String {
        return if (mode == filterMode) "[$text]" else text
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onFilterModeChange(CharFilterMode.ALL) }) { Text(text = label(CharFilterMode.ALL, "全部")) }
        OutlinedButton(onClick = { onFilterModeChange(CharFilterMode.DUE) }) { Text(text = label(CharFilterMode.DUE, "到期")) }
        OutlinedButton(onClick = { onFilterModeChange(CharFilterMode.LEARNED) }) { Text(text = label(CharFilterMode.LEARNED, "已学")) }
        OutlinedButton(onClick = { onFilterModeChange(CharFilterMode.UNLEARNED) }) { Text(text = label(CharFilterMode.UNLEARNED, "未学")) }
        OutlinedButton(onClick = { onFilterModeChange(CharFilterMode.DISABLED) }) { Text(text = label(CharFilterMode.DISABLED, "禁用")) }
    }
}

@Composable
private fun SelectedCharsActions(
    selectedChars: Set<String>,
    onBulkEnable: (List<String>) -> Unit,
    onBulkDisable: (List<String>) -> Unit,
    onMarkDueToday: (List<String>) -> Unit,
    onResetWrongCount: (List<String>) -> Unit,
    onResetProgress: (List<String>) -> Unit,
    onClearSelection: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "已选：${selectedChars.size}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onBulkEnable(selectedChars.toList()) }) { Text(text = "批量启用") }
            Button(onClick = { onBulkDisable(selectedChars.toList()) }) { Text(text = "批量禁用") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onMarkDueToday(selectedChars.toList()) }) { Text(text = "批量今天复习") }
            Button(onClick = { onResetWrongCount(selectedChars.toList()) }) { Text(text = "批量清零错误") }
            Button(onClick = { onResetProgress(selectedChars.toList()) }) { Text(text = "批量清零进度") }
        }
        OutlinedButton(onClick = onClearSelection) { Text(text = "清空选择") }
    }
}

@Composable
private fun SelectedCharacterDetails(
    selectedItem: CharIndexItem,
    disabledChars: Set<String>,
    progress: AdminProgress?,
    overridePhrases: List<String>,
    newPhrase: String,
    onNewPhraseChange: (String) -> Unit,
    onToggleEnabled: (char: String, enabled: Boolean) -> Unit,
    onSavePhraseOverride: (char: String, phrases: List<String>) -> Unit,
    onDeletePhraseOverride: (char: String) -> Unit,
    onMarkDueToday: (List<String>) -> Unit,
    onResetWrongCount: (List<String>) -> Unit,
    onResetProgress: (List<String>) -> Unit,
) {
    val char = selectedItem.char
    val enabled = char !in disabledChars

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "当前：$char")
        Text(text = "拼音：${selectedItem.pinyin.joinToString("、")}")
        Text(text = "笔画数：${selectedItem.strokeCount}")
        Text(text = "默认短语：${selectedItem.phrases.joinToString("、")}")
        Text(text = "覆盖短语：${overridePhrases.joinToString("、")}")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = if (enabled) "已启用" else "已禁用")
            Switch(checked = enabled, onCheckedChange = { onToggleEnabled(char, it) })
        }

        progress?.let { p ->
            Text(text = "正确次数=${p.correctCount} 错误笔画=${p.wrongCount}")
            Text(text = "上次学习日=${epochDayToText(p.lastStudiedDay)} 下次复习日=${epochDayToText(p.nextDueDay)} 间隔=${p.intervalDays}天")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onMarkDueToday(listOf(char)) }) { Text(text = "今天复习") }
                Button(onClick = { onResetWrongCount(listOf(char)) }) { Text(text = "清零错误") }
                Button(onClick = { onResetProgress(listOf(char)) }) { Text(text = "清零进度") }
            }
        }

        OutlinedTextField(
            value = newPhrase,
            onValueChange = onNewPhraseChange,
            label = { Text(text = "新增短语（≤5字）") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val phrase = newPhrase.trim()
                    if (phrase.isEmpty() || phrase.length > 5) return@Button
                    onSavePhraseOverride(char, (overridePhrases + phrase).distinct())
                },
            ) { Text(text = "添加并保存覆盖") }

            OutlinedButton(onClick = { onDeletePhraseOverride(char) }) { Text(text = "清空覆盖") }
        }
    }
}

@Composable
private fun CharacterListItemRow(
    item: CharIndexItem,
    selectedChar: String?,
    selectedChars: Set<String>,
    statusText: String,
    enabled: Boolean,
    onSelectChar: (String) -> Unit,
    onToggleChecked: (Boolean) -> Unit,
    onToggleEnabled: (char: String, enabled: Boolean) -> Unit,
) {
    val char = item.char
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onSelectChar(char) })
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(
            checked = char in selectedChars,
            onCheckedChange = onToggleChecked,
        )
        Text(text = if (selectedChar == char) "▶ $char" else char)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "拼音：${item.pinyin.firstOrNull().orEmpty()}  笔画：${item.strokeCount}")
            Text(text = statusText)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggleEnabled(char, it) },
        )
    }
}

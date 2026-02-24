package com.hanzi.learner.features.admin.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanzi.learner.character_writer.data.CharIndexItem
import com.hanzi.learner.features.admin.model.AdminProgress
import com.hanzi.learner.features.admin.ui.epochDayToText
import com.hanzi.learner.features.admin.viewmodel.CharFilterMode
import com.hanzi.learner.features.admin.viewmodel.FilteredCharacterResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterManagementTab(
    modifier: Modifier = Modifier,
    searchText: String,
    filterMode: CharFilterMode,
    filteredResult: FilteredCharacterResult,
    disabledChars: Set<String>,
    allProgress: Map<String, AdminProgress>,
    todayEpochDay: Long,
    selectedItem: CharIndexItem?,
    progress: AdminProgress?,
    overridePhrases: List<String>,
    newPhrase: String,
    onSearchTextChange: (String) -> Unit,
    onFilterModeChange: (CharFilterMode) -> Unit,
    onLoadMore: () -> Unit,
    onNewPhraseChange: (String) -> Unit,
    onSelectChar: (String?) -> Unit,
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
    var selectedChars by remember { mutableStateOf<Set<String>>(emptySet()) }

    val totalCount = filteredResult.totalCount
    val visibleItems = filteredResult.visibleItems
    val displayCount = visibleItems.size

    Column(modifier = modifier.fillMaxSize()) {
        // Sticky Header area
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索字、拼音或笔画数...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { onSearchTextChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Filter Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CharFilterMode.values().forEach { mode ->
                        FilterChip(
                            selected = filterMode == mode,
                            onClick = { onFilterModeChange(mode) },
                            label = { Text(mode.label) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Bulk Actions Panel
                AnimatedVisibility(
                    visible = selectedChars.isNotEmpty(),
                    enter = expandVertically(animationSpec = tween(250)),
                    exit = shrinkVertically(animationSpec = tween(250))
                ) {
                    SelectedCharsActions(
                        selectedChars = selectedChars,
                        onBulkEnable = onBulkEnable,
                        onBulkDisable = onBulkDisable,
                        onMarkDueToday = onMarkDueToday,
                        onResetWrongCount = onResetWrongCount,
                        onResetProgress = onResetProgress,
                        onSelectAll = {
                            selectedChars = visibleItems.map { it.char }.toSet()
                        },
                        onClearSelection = { selectedChars = emptySet() },
                    )
                }
            }
        }

        // List Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "共 $totalCount 个字",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (totalCount > displayCount) {
                Text(
                    text = "已显示 $displayCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Character List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibleItems, key = { it.char }) { item ->
                val ch = item.char
                val p = allProgress[ch]
                val enabled = ch !in disabledChars
                val isDue = p != null && p.nextDueDay <= todayEpochDay
                
                CharacterListItemRow(
                    item = item,
                    progress = p,
                    isDue = isDue,
                    enabled = enabled,
                    isChecked = ch in selectedChars,
                    onSelectChar = { onSelectChar(ch) },
                    onToggleChecked = { isChecked ->
                        selectedChars = if (isChecked) selectedChars + ch else selectedChars - ch
                    },
                    onToggleEnabled = { e -> onToggleEnabled(ch, e) },
                )
            }

            if (totalCount > displayCount) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(
                            onClick = onLoadMore,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "加载更多 (+20)")
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(text = "返回") }
            }
        }
    }

    // Bottom Sheet for Details
    if (selectedItem != null) {
        ModalBottomSheet(
            onDismissRequest = { onSelectChar(null) },
            windowInsets = WindowInsets.navigationBars
        ) {
            SelectedCharacterDetails(
                selectedItem = selectedItem,
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
                onClose = { onSelectChar(null) }
            )
        }
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
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已选择 ${selectedChars.size} 项",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSelectAll) { Text("全选本页") }
                    TextButton(onClick = onClearSelection) { Text("清除") }
                }
            }
            
            // First row of actions
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onBulkEnable(selectedChars.toList()) }, shape = RoundedCornerShape(8.dp)) { 
                    Text(text = "启用") 
                }
                Button(onClick = { onBulkDisable(selectedChars.toList()) }, shape = RoundedCornerShape(8.dp)) { 
                    Text(text = "禁用") 
                }
                Button(onClick = { onMarkDueToday(selectedChars.toList()) }, shape = RoundedCornerShape(8.dp)) { 
                    Text(text = "复习") 
                }
                Button(onClick = { onResetWrongCount(selectedChars.toList()) }, shape = RoundedCornerShape(8.dp)) { 
                    Text(text = "清错") 
                }
                Button(onClick = { onResetProgress(selectedChars.toList()) }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { 
                    Text(text = "清进度") 
                }
            }
        }
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
    onClose: () -> Unit,
) {
    val char = selectedItem.char
    val enabled = char !in disabledChars

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 24.dp), // Extra bottom padding for safe area
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "管理字：$char",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = enabled,
                onCheckedChange = { onToggleEnabled(char, it) }
            )
        }

        // Basic Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "拼音: ${selectedItem.pinyin.joinToString("、")}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "笔画: ${selectedItem.strokeCount} 画", style = MaterialTheme.typography.bodyMedium)
                Text(text = "默认短语: ${selectedItem.phrases.joinToString("、")}", style = MaterialTheme.typography.bodyMedium)
                if (overridePhrases.isNotEmpty()) {
                    Text(text = "自定义短语: ${overridePhrases.joinToString("、")}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Progress Details
        if (progress != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "学习状态", style = MaterialTheme.typography.titleSmall)
                    Text(text = "正确: ${progress.correctCount} 次  |  错误: ${progress.wrongCount} 笔", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "上次复习: ${epochDayToText(progress.lastStudiedDay)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "下次复习: ${epochDayToText(progress.nextDueDay)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "当前间隔: ${progress.intervalDays} 天", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Text(text = "尚未学习该字", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Actions
        Text(text = "操作", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (progress != null) {
                OutlinedButton(onClick = { onMarkDueToday(listOf(char)) }) { Text("今天复习") }
                OutlinedButton(onClick = { onResetWrongCount(listOf(char)) }) { Text("清零错误") }
                OutlinedButton(
                    onClick = { onResetProgress(listOf(char)) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("清零进度") }
            }
        }

        Divider()

        // Phrase Override
        Text(text = "短语设置", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newPhrase,
                onValueChange = onNewPhraseChange,
                label = { Text(text = "新增（≤5字）") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    val phrase = newPhrase.trim()
                    if (phrase.isEmpty() || phrase.length > 5) return@Button
                    onSavePhraseOverride(char, (overridePhrases + phrase).distinct())
                }
            ) {
                Text("添加")
            }
        }
        
        if (overridePhrases.isNotEmpty()) {
            OutlinedButton(
                onClick = { onDeletePhraseOverride(char) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("清空所有自定义短语")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("完成")
        }
    }
}

@Composable
private fun CharacterListItemRow(
    item: CharIndexItem,
    progress: AdminProgress?,
    isDue: Boolean,
    enabled: Boolean,
    isChecked: Boolean,
    onSelectChar: () -> Unit,
    onToggleChecked: (Boolean) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectChar),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onToggleChecked,
                modifier = Modifier.padding(end = 4.dp)
            )
            
            // Large Character Display
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.char,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "拼音: ${item.pinyin.firstOrNull().orEmpty()} | 笔画: ${item.strokeCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // Tags row
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!enabled) {
                        StatusTag("已禁用", MaterialTheme.colorScheme.error)
                    }
                    if (progress == null) {
                        StatusTag("未学", MaterialTheme.colorScheme.outline)
                    } else {
                        StatusTag("已学", MaterialTheme.colorScheme.primary)
                        if (isDue) {
                            StatusTag("到期", MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
            
            Switch(
                checked = enabled,
                onCheckedChange = onToggleEnabled,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun StatusTag(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

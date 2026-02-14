package com.hanzi.learner.feature.admin.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hanzi.learner.hanzi.data.CharIndexItem
import com.hanzi.learner.ui.AdminFeatureDependencies

private data class AdminTabDescriptor(
    val title: String,
    val content: @Composable (Modifier) -> Unit,
)

@Composable
fun AdminScreen(
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    deps: AdminFeatureDependencies,
) {
    val dataChangedNotifier = remember { AdminDataChangedNotifier() }
    val factories = remember(deps) { AdminViewModelFactories.from(deps) }
    val refreshVersion by dataChangedNotifier.version.collectAsState()
    var indexItems by remember { mutableStateOf<List<CharIndexItem>>(emptyList()) }
    LaunchedEffect(refreshVersion, deps) {
        indexItems = deps.adminIndexRepository.loadIndex()
    }

    val tabs = remember(onBack, factories, dataChangedNotifier, indexItems) {
        listOf(
            AdminTabDescriptor(
                title = "总览",
                content = { modifier ->
                    OverviewTabRoute(
                        modifier = modifier,
                        onBack = onBack,
                        factories = factories,
                        notifier = dataChangedNotifier,
                    )
                },
            ),
            AdminTabDescriptor(
                title = "字管理",
                content = { modifier ->
                    CharacterManagementTabRoute(
                        modifier = modifier,
                        onBack = onBack,
                        factories = factories,
                        notifier = dataChangedNotifier,
                    )
                },
            ),
            AdminTabDescriptor(
                title = "学习数据",
                content = { modifier ->
                    LearningDataTabRoute(
                        modifier = modifier,
                        onBack = onBack,
                        factories = factories,
                        notifier = dataChangedNotifier,
                    )
                },
            ),
            AdminTabDescriptor(
                title = "设置",
                content = { modifier ->
                    SettingsTabRoute(
                        modifier = modifier,
                        onBack = onBack,
                        factories = factories,
                        notifier = dataChangedNotifier,
                    )
                },
            ),
            AdminTabDescriptor(
                title = "备份",
                content = { modifier ->
                    BackupTabRoute(
                        modifier = modifier,
                        onBack = onBack,
                        factories = factories,
                        notifier = dataChangedNotifier,
                        indexItems = indexItems,
                    )
                },
            ),
        )
    }
    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { idx, tab ->
                Tab(
                    selected = selectedTabIndex == idx,
                    onClick = { selectedTabIndex = idx },
                    text = { Text(text = tab.title) },
                )
            }
        }

        val modifier = Modifier.fillMaxSize()
        tabs.getOrNull(selectedTabIndex)?.content?.invoke(modifier)
    }
}

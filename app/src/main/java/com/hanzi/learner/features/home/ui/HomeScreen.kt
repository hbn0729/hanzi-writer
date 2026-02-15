package com.hanzi.learner.features.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanzi.learner.features.home.viewmodel.HomeAction
import com.hanzi.learner.features.home.viewmodel.HomeViewModel
import com.hanzi.learner.features.home.viewmodel.PracticeMode

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.onAction(HomeAction.LoadData)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(64.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = { viewModel.onAction(HomeAction.SelectMode(PracticeMode.PRACTICE)) },
            modifier = Modifier.height(64.dp).width(200.dp)
        ) {
            Text(text = "练习 (${uiState.data?.unlearnedCount ?: 0})", fontSize = 20.sp)
        }
        Button(
            onClick = { viewModel.onAction(HomeAction.SelectMode(PracticeMode.REVIEW)) },
            modifier = Modifier.height(64.dp).width(200.dp)
        ) {
            Text(text = "复习 (${uiState.data?.reviewCount ?: 0})", fontSize = 20.sp)
        }
    }
}

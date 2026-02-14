package com.hanzi.learner.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hanzi.learner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTopBar(
    currentRoute: String?,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    onAdminLongPress: () -> Unit,
) {
    if (currentRoute == AppRoutes.PRACTICE || currentRoute == AppRoutes.REVIEW) return

    TopAppBar(
        title = { Text(text = "Hanzi Learner") },
        navigationIcon = {
            if (canNavigateBack && currentRoute != AppRoutes.HOME) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = {
            if (currentRoute == AppRoutes.HOME) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { onAdminLongPress() },
                            )
                        },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
    )
}

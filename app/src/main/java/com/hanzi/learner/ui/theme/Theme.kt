package com.hanzi.learner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun HanziLearnerTheme(
    content: @Composable () -> Unit,
) {
    val useDark = !LocalInspectionMode.current && (LocalContext.current.resources.configuration.uiMode and 0x30) == 0x20
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content,
    )
}

package com.hanzi.learner.hanzi.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hanzi.learner.hanzi.model.CharacterData
import kotlinx.coroutines.delay

@Composable
fun AnimatedHanziCanvas(
    character: CharacterData,
    modifier: Modifier = Modifier,
    padding: Dp = 12.dp,
    showOutline: Boolean = true,
    outlineColor: Color = Color(0xFFE0E0E0),
    animatedStrokeColor: Color = Color(0xFF1E88E5),
    animatedStrokeWidth: Dp = 10.dp,
    isPlaying: Boolean,
    strokeDurationMs: Int = 700,
    delayBetweenStrokesMs: Int = 150,
) {
    var strokeIndex by remember(character.char) { mutableStateOf(0) }
    val progress = remember(character.char) { Animatable(0f) }
    var activeIndex by remember(character.char) { mutableStateOf<Int?>(null) }

    LaunchedEffect(character.char, isPlaying) {
        if (!isPlaying) {
            activeIndex = null
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        strokeIndex = 0
        while (isPlaying) {
            activeIndex = strokeIndex
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = strokeDurationMs, easing = LinearEasing),
            )
            delay(delayBetweenStrokesMs.toLong())
            strokeIndex += 1
            if (strokeIndex >= character.medians.size) {
                strokeIndex = 0
            }
        }
    }

    HanziCanvas(
        character = character,
        modifier = modifier,
        padding = padding,
        showOutline = showOutline,
        outlineColor = outlineColor,
        animatedStrokeIndex = activeIndex,
        animatedStrokeColor = animatedStrokeColor,
        animatedStrokeWidth = animatedStrokeWidth,
        animatedStrokeProgress = progress.value,
    )
}

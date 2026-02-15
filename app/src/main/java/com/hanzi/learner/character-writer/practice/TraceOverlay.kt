package com.hanzi.learner.character-writer.practice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hanzi.learner.character-writer.match.StrokeMatchConfig
import com.hanzi.learner.character-writer.match.StrokeMatchOptions
import com.hanzi.learner.character-writer.match.StrokeMatchResult
import com.hanzi.learner.character-writer.model.CharacterData
import com.hanzi.learner.character-writer.model.Point
import com.hanzi.learner.character-writer.render.HanziCanvas
import com.hanzi.learner.character-writer.render.Positioner

@Composable
fun HanziTraceOverlay(
    character: CharacterData,
    modifier: Modifier = Modifier,
    padding: Dp = 12.dp,
    strokeIndex: Int,
    completedStrokeCount: Int = 0,
    showHintStroke: Boolean = true,
    matcher: (List<Point>, CharacterData, Int) -> StrokeMatchResult,
    positionerFactory: (IntSize, Float) -> Positioner = { s, paddingPx ->
        Positioner(
            width = s.width.toFloat(),
            height = s.height.toFloat(),
            padding = paddingPx,
        )
    },
    onStrokeEnd: (StrokeMatchResult) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var userStrokeCanvasPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var userStrokeHanziPoints by remember { mutableStateOf<List<Point>>(emptyList()) }

    val progress = remember(strokeIndex) { Animatable(0f) }

    LaunchedEffect(showHintStroke, strokeIndex) {
        if (showHintStroke) {
            while (true) {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 800, easing = LinearEasing)
                )
                delay(400)
            }
        } else {
            progress.snapTo(0f)
        }
    }

    Box(modifier = modifier.onSizeChanged { size = it }) {
        HanziCanvas(
            character = character,
            modifier = Modifier.fillMaxSize(),
            padding = padding,
            showOutline = true,
            completedStrokeCount = completedStrokeCount,
            animatedStrokeIndex = if (showHintStroke) strokeIndex else null,
            animatedStrokeProgress = if (showHintStroke) progress.value else 0f,
            animatedStrokeWidth = 10.dp,
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(character.char, strokeIndex, size, padding) {
                    if (size.width <= 0 || size.height <= 0) return@pointerInput
                    val positioner = positionerFactory(size, padding.toPx())

                    detectDragGestures(
                        onDragStart = { pos ->
                            userStrokeCanvasPoints = listOf(pos)
                            userStrokeHanziPoints = listOf(positioner.toHanzi(Point(pos.x, pos.y)))
                        },
                        onDrag = { change, _ ->
                            val pos = change.position
                            userStrokeCanvasPoints = userStrokeCanvasPoints + pos
                            userStrokeHanziPoints = userStrokeHanziPoints + positioner.toHanzi(Point(pos.x, pos.y))
                            change.consume()
                        },
                        onDragEnd = {
                            val res = matcher(userStrokeHanziPoints, character, strokeIndex)
                            onStrokeEnd(res)
                            userStrokeCanvasPoints = emptyList()
                            userStrokeHanziPoints = emptyList()
                        },
                        onDragCancel = {
                            userStrokeCanvasPoints = emptyList()
                            userStrokeHanziPoints = emptyList()
                        },
                    )
                },
        ) {
            if (userStrokeCanvasPoints.size >= 2) {
                val path = Path()
                path.moveTo(userStrokeCanvasPoints.first().x, userStrokeCanvasPoints.first().y)
                for (i in 1 until userStrokeCanvasPoints.size) {
                    val p = userStrokeCanvasPoints[i]
                    path.lineTo(p.x, p.y)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF212121),
                    style = Stroke(width = 10.dp.toPx()),
                )
            }
        }
    }
}

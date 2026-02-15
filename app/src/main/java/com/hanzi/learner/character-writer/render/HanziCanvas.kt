package com.hanzi.learner.character-writer.render

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.hanzi.learner.character-writer.model.CharacterData
import com.hanzi.learner.character-writer.model.Point

@Composable
fun HanziCanvas(
    character: CharacterData,
    modifier: Modifier = Modifier,
    padding: Dp = 12.dp,
    showOutline: Boolean = true,
    outlineColor: Color = Color(0xFFE0E0E0),
    animatedStrokeIndex: Int? = null,
    animatedStrokeColor: Color = Color(0xFF1E88E5),
    animatedStrokeWidth: Dp = 10.dp,
    animatedStrokeProgress: Float = 0f,
    completedStrokeCount: Int = 0,
    completedStrokeColor: Color = Color(0xFF616161),
) {
    val rawPaths = remember(character.strokes) {
        character.strokes.map { pathString ->
            PathParser.createPathFromPathData(pathString) ?: Path()
        }
    }

    val outlinePaint = remember(outlineColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = outlineColor.toArgb()
        }
    }

    val completedPaint = remember(completedStrokeColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = completedStrokeColor.toArgb()
        }
    }

    val strokePaint = remember(animatedStrokeColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = animatedStrokeColor.toArgb()
        }
    }

    Canvas(modifier = modifier) {
        val size = Size(width = size.width, height = size.height)
        val positioner = Positioner(
            width = size.width,
            height = size.height,
            padding = padding.toPx(),
        )

        val matrix = Matrix().apply {
            setScale(positioner.scale, -positioner.scale)
            postTranslate(positioner.xOffset, positioner.height - positioner.yOffset)
        }

        strokePaint.strokeWidth = animatedStrokeWidth.toPx()

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            if (showOutline) {
                rawPaths.forEach { raw ->
                    val transformed = Path(raw)
                    transformed.transform(matrix)
                    native.drawPath(transformed, outlinePaint)
                }
            }

            val completedCount = completedStrokeCount.coerceIn(0, rawPaths.size)
            for (i in 0 until completedCount) {
                val transformed = Path(rawPaths[i])
                transformed.transform(matrix)
                native.drawPath(transformed, completedPaint)
            }

            val index = animatedStrokeIndex
            if (index != null && index in character.medians.indices) {
                val median = character.medians[index]
                val medianPath = buildPolylinePath(median, positioner)
                val shown = extractPathSegment(medianPath, animatedStrokeProgress.coerceIn(0f, 1f))
                native.drawPath(shown, strokePaint)
            }
        }
    }
}

private fun buildPolylinePath(points: List<Point>, positioner: Positioner): Path {
    val path = Path()
    if (points.isEmpty()) return path
    val start = positioner.toCanvas(points[0])
    path.moveTo(start.x, start.y)
    for (i in 1 until points.size) {
        val p = positioner.toCanvas(points[i])
        path.lineTo(p.x, p.y)
    }
    return path
}

private fun extractPathSegment(path: Path, progress: Float): Path {
    val pm = PathMeasure(path, false)
    val length = pm.length
    val out = Path()
    if (length <= 0f) return out
    val stop = length * progress
    pm.getSegment(0f, stop, out, true)
    return out
}

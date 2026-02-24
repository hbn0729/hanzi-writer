package com.hanzi.learner.character_writer.render

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.hanzi.learner.character_writer.model.CharacterData

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

    // OPT-01: Pre-allocate reusable objects to eliminate per-frame allocations
    val reusableMatrix = remember { Matrix() }
    val reusableMedianPath = remember { Path() }
    val reusablePathMeasure = remember { PathMeasure() }
    val reusableOutputPath = remember { Path() }

    Canvas(modifier = modifier) {
        val positioner = Positioner(
            width = size.width,
            height = size.height,
            padding = padding.toPx(),
        )

        reusableMatrix.apply {
            setScale(positioner.scale, -positioner.scale)
            postTranslate(positioner.xOffset, positioner.height - positioner.yOffset)
        }

        strokePaint.strokeWidth = animatedStrokeWidth.toPx()

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas

            // Outlines: canvas.concat(matrix) instead of N per-path Path(raw)+transform
            if (showOutline) {
                native.save()
                native.concat(reusableMatrix)
                for (path in rawPaths) {
                    native.drawPath(path, outlinePaint)
                }
                native.restore()
            }

            // Completed strokes: same canvas transform approach
            val completedCount = completedStrokeCount.coerceIn(0, rawPaths.size)
            if (completedCount > 0) {
                native.save()
                native.concat(reusableMatrix)
                for (i in 0 until completedCount) {
                    native.drawPath(rawPaths[i], completedPaint)
                }
                native.restore()
            }

            // Animated stroke: reuse Path + PathMeasure objects
            val index = animatedStrokeIndex
            if (index != null && index in character.medians.indices) {
                val median = character.medians[index]
                reusableMedianPath.reset()
                if (median.isNotEmpty()) {
                    // Inline toCanvas to avoid Point allocations
                    val scale = positioner.scale
                    val xOff = positioner.xOffset
                    val yBase = positioner.height - positioner.yOffset
                    val p0 = median[0]
                    reusableMedianPath.moveTo(
                        p0.x * scale + xOff,
                        yBase - p0.y * scale,
                    )
                    for (j in 1 until median.size) {
                        val p = median[j]
                        reusableMedianPath.lineTo(
                            p.x * scale + xOff,
                            yBase - p.y * scale,
                        )
                    }
                }

                reusablePathMeasure.setPath(reusableMedianPath, false)
                val length = reusablePathMeasure.length
                if (length > 0f) {
                    val stop = length * animatedStrokeProgress.coerceIn(0f, 1f)
                    reusableOutputPath.reset()
                    reusablePathMeasure.getSegment(0f, stop, reusableOutputPath, true)
                    native.drawPath(reusableOutputPath, strokePaint)
                }
            }
        }
    }
}

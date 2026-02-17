package com.hanzi.learner.character_writer.render

import com.hanzi.learner.character_writer.model.Point
import kotlin.math.min

class Positioner(
    width: Float,
    height: Float,
    padding: Float,
) {
    val width: Float = width
    val height: Float = height
    val padding: Float = padding

    val xOffset: Float
    val yOffset: Float
    val scale: Float

    init {
        val fromX = 0f
        val fromY = -124f
        val toX = 1024f
        val toY = 900f

        val preScaledWidth = toX - fromX
        val preScaledHeight = toY - fromY

        val effectiveWidth = width - 2f * padding
        val effectiveHeight = height - 2f * padding

        val scaleX = effectiveWidth / preScaledWidth
        val scaleY = effectiveHeight / preScaledHeight
        scale = min(scaleX, scaleY)

        val xCenteringBuffer = padding + (effectiveWidth - scale * preScaledWidth) / 2f
        val yCenteringBuffer = padding + (effectiveHeight - scale * preScaledHeight) / 2f

        xOffset = -1f * fromX * scale + xCenteringBuffer
        yOffset = -1f * fromY * scale + yCenteringBuffer
    }

    fun toCanvas(point: Point): Point {
        val x = point.x * scale + xOffset
        val y = height - yOffset - point.y * scale
        return Point(x = x, y = y)
    }

    fun toHanzi(canvasPoint: Point): Point {
        val x = (canvasPoint.x - xOffset) / scale
        val y = (height - yOffset - canvasPoint.y) / scale
        return Point(x = x, y = y)
    }
}

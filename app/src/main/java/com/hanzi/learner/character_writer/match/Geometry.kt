package com.hanzi.learner.character_writer.match

import com.hanzi.learner.character_writer.model.Point
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

internal fun subtract(p1: Point, p2: Point): Point = Point(p1.x - p2.x, p1.y - p2.y)

internal fun magnitude(p: Point): Double = sqrt(p.x.toDouble().pow(2.0) + p.y.toDouble().pow(2.0))

internal fun distance(p1: Point, p2: Point): Double = magnitude(subtract(p1, p2))

internal fun almostEquals(p1: Point, p2: Point, eps: Double = 1e-3): Boolean =
    abs(p1.x - p2.x) <= eps && abs(p1.y - p2.y) <= eps

internal fun length(points: List<Point>): Double {
    if (points.size < 2) return 0.0
    var last = points[0]
    var acc = 0.0
    for (i in 1 until points.size) {
        val p = points[i]
        acc += distance(p, last)
        last = p
    }
    return acc
}

internal fun cosineSimilarity(p1: Point, p2: Point): Double {
    val dot = p1.x.toDouble() * p2.x.toDouble() + p1.y.toDouble() * p2.y.toDouble()
    val mag1 = magnitude(p1)
    val mag2 = magnitude(p2)
    val denom = mag1 * mag2
    if (denom <= 1e-9) return -1.0
    return dot / denom
}

private fun extendPointOnLine(p1: Point, p2: Point, dist: Double): Point {
    val vect = subtract(p2, p1)
    val mag = magnitude(vect)
    if (mag <= 1e-9) return p2
    val norm = dist / mag
    return Point(
        x = (p2.x + (norm * vect.x)).toFloat(),
        y = (p2.y + (norm * vect.y)).toFloat(),
    )
}

internal fun frechetDist(curve1: List<Point>, curve2: List<Point>): Double {
    val longCurve = if (curve1.size >= curve2.size) curve1 else curve2
    val shortCurve = if (curve1.size >= curve2.size) curve2 else curve1
    if (shortCurve.isEmpty()) return 0.0

    fun calcVal(
        i: Int,
        j: Int,
        prevResultsCol: DoubleArray,
        curResultsCol: DoubleArray,
        curLen: Int,
    ): Double {
        if (i == 0 && j == 0) {
            return distance(longCurve[0], shortCurve[0])
        }
        if (i > 0 && j == 0) {
            return max(prevResultsCol[0], distance(longCurve[i], shortCurve[0]))
        }
        val lastResult = curResultsCol[curLen - 1]
        if (i == 0 && j > 0) {
            return max(lastResult, distance(longCurve[0], shortCurve[j]))
        }
        return max(
            min(prevResultsCol[j], min(prevResultsCol[j - 1], lastResult)),
            distance(longCurve[i], shortCurve[j]),
        )
    }

    var prev = DoubleArray(shortCurve.size)
    for (i in longCurve.indices) {
        val cur = DoubleArray(shortCurve.size)
        var curLen = 0
        for (j in shortCurve.indices) {
            curLen += 1
            cur[j] = calcVal(i, j, prev, cur, curLen)
        }
        prev = cur
    }
    return prev[shortCurve.size - 1]
}

private fun subdivideCurve(curve: List<Point>, maxLen: Double = 0.05): List<Point> {
    if (curve.isEmpty()) return emptyList()
    val newCurve = ArrayList<Point>()
    newCurve.add(curve.first())
    for (point in curve.drop(1)) {
        val prevPoint = newCurve.last()
        val segLen = distance(point, prevPoint)
        if (segLen > maxLen) {
            val numNewPoints = ceil(segLen / maxLen).toInt()
            val newSegLen = segLen / numNewPoints
            for (i in 0 until numNewPoints) {
                newCurve.add(extendPointOnLine(point, prevPoint, -1.0 * newSegLen * (i + 1)))
            }
        } else {
            newCurve.add(point)
        }
    }
    return newCurve
}

private fun outlineCurve(curve: List<Point>, numPoints: Int = 30): List<Point> {
    if (curve.isEmpty()) return emptyList()
    if (curve.size == 1) return List(numPoints) { curve[0] }

    val curveLen = length(curve)
    val segmentLen = curveLen / (numPoints - 1)
    val outline = ArrayList<Point>()
    outline.add(curve[0])
    val endPoint = curve.last()
    val remaining = ArrayList<Point>(curve.drop(1))

    for (i in 0 until numPoints - 2) {
        var lastPoint = outline.last()
        var remainingDist = segmentLen
        var found = false
        while (!found) {
            val nextPointDist = distance(lastPoint, remaining[0])
            if (nextPointDist < remainingDist) {
                remainingDist -= nextPointDist
                lastPoint = remaining.removeAt(0)
            } else {
                val nextPoint = extendPointOnLine(
                    lastPoint,
                    remaining[0],
                    remainingDist - nextPointDist,
                )
                outline.add(nextPoint)
                found = true
            }
        }
    }
    outline.add(endPoint)
    return outline
}

internal fun normalizeCurve(curve: List<Point>): List<Point> {
    val outlined = outlineCurve(curve)
    val meanX = outlined.map { it.x.toDouble() }.average()
    val meanY = outlined.map { it.y.toDouble() }.average()
    val translated = outlined.map { p -> Point((p.x - meanX).toFloat(), (p.y - meanY).toFloat()) }
    val scale = sqrt(
        listOf(
            translated.first(),
            translated.last(),
        ).map { p -> p.x.toDouble().pow(2.0) + p.y.toDouble().pow(2.0) }.average()
    )
    val safeScale = if (scale <= 1e-9) 1.0 else scale
    val scaled = translated.map { p -> Point((p.x / safeScale).toFloat(), (p.y / safeScale).toFloat()) }
    return subdivideCurve(scaled)
}

internal fun rotate(curve: List<Point>, theta: Double): List<Point> {
    val c = cos(theta)
    val s = sin(theta)
    return curve.map { p ->
        Point(
            x = (c * p.x - s * p.y).toFloat(),
            y = (s * p.x + c * p.y).toFloat(),
        )
    }
}

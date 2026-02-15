package com.hanzi.learner.character-writer.match

import com.hanzi.learner.character-writer.model.Point
import kotlin.math.min

internal class Stroke(
    val points: List<Point>,
    val strokeNum: Int,
) {
    fun startingPoint(): Point = points.first()
    fun endingPoint(): Point = points.last()
    fun length(): Double = length(points)

    fun vectors(): List<Point> {
        if (points.size < 2) return emptyList()
        val out = ArrayList<Point>(points.size - 1)
        var last = points[0]
        for (i in 1 until points.size) {
            val p = points[i]
            out.add(subtract(p, last))
            last = p
        }
        return out
    }

    fun distanceTo(point: Point): Double {
        var minDist = Double.POSITIVE_INFINITY
        for (p in points) {
            minDist = min(minDist, distance(p, point))
        }
        return minDist
    }

    fun averageDistance(points: List<Point>): Double {
        if (points.isEmpty()) return Double.POSITIVE_INFINITY
        var acc = 0.0
        for (p in points) {
            acc += distanceTo(p)
        }
        return acc / points.size
    }
}

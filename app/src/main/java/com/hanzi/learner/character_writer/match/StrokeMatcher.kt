package com.hanzi.learner.character_writer.match

import com.hanzi.learner.character_writer.model.CharacterData
import com.hanzi.learner.character_writer.model.Point
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

private fun defaultShapeFitRotations(): DoubleArray {
    return doubleArrayOf(
        PI / 16.0,
        PI / 32.0,
        0.0,
        -PI / 32.0,
        -PI / 16.0,
    )
}

data class StrokeMatchMeta(
    val isStrokeBackwards: Boolean,
)

data class StrokeMatchResult(
    val isMatch: Boolean,
    val meta: StrokeMatchMeta,
    val avgDist: Double,
)

data class StrokeMatchConfig(
    val cosineSimilarityThreshold: Double = 0.0,
    val startAndEndDistanceThreshold: Double = 250.0,
    val frechetThreshold: Double = 0.4,
    val minLengthThreshold: Double = 0.35,
    val shapeFitRotations: DoubleArray = defaultShapeFitRotations(),
)

data class StrokeMatchOptions(
    val leniency: Double = 1.2,
    val isOutlineVisible: Boolean = true,
    val checkBackwards: Boolean = true,
    val averageDistanceThreshold: Double = 350.0,
)

interface StrokeMatcherContract {
    fun matches(
        userStrokePoints: List<Point>,
        character: CharacterData,
        strokeNum: Int,
        options: StrokeMatchOptions,
        config: StrokeMatchConfig,
    ): StrokeMatchResult
}

fun StrokeMatcherContract.matchesWithDefaults(
    userStrokePoints: List<Point>,
    character: CharacterData,
    strokeNum: Int,
    options: StrokeMatchOptions = StrokeMatchOptions(),
    config: StrokeMatchConfig = StrokeMatchConfig(),
): StrokeMatchResult = matches(
    userStrokePoints = userStrokePoints,
    character = character,
    strokeNum = strokeNum,
    options = options,
    config = config,
)

object DefaultStrokeMatcher : StrokeMatcherContract {
    override fun matches(
        userStrokePoints: List<Point>,
        character: CharacterData,
        strokeNum: Int,
        options: StrokeMatchOptions,
        config: StrokeMatchConfig,
    ): StrokeMatchResult {
        val strokes = character.medians.mapIndexed { index, pts -> Stroke(points = pts, strokeNum = index) }
        val points = stripDuplicates(userStrokePoints)
        if (points.size < 2) {
            return StrokeMatchResult(isMatch = false, meta = StrokeMatchMeta(false), avgDist = Double.POSITIVE_INFINITY)
        }

        val initial = getMatchData(points, strokes[strokeNum], options, config)
        if (!initial.isMatch) return initial

        val later = strokes.drop(strokeNum + 1)
        var closestMatchDist = initial.avgDist
        for (s in later) {
            val res = getMatchData(points, s, options.copy(checkBackwards = false), config)
            if (res.isMatch && res.avgDist < closestMatchDist) {
                closestMatchDist = res.avgDist
            }
        }
        if (closestMatchDist < initial.avgDist) {
            val leniencyAdjustment = (0.6 * (closestMatchDist + initial.avgDist)) / (2.0 * initial.avgDist)
            return getMatchData(points, strokes[strokeNum], options.copy(leniency = options.leniency * leniencyAdjustment), config)
        }

        return initial
    }

    private fun stripDuplicates(points: List<Point>): List<Point> {
        if (points.size < 2) return points
        val deduped = ArrayList<Point>(points.size)
        deduped.add(points[0])
        for (i in 1 until points.size) {
            val p = points[i]
            if (!almostEquals(p, deduped.last())) {
                deduped.add(p)
            }
        }
        return deduped
    }

    private fun startAndEndMatches(points: List<Point>, stroke: Stroke, leniency: Double, config: StrokeMatchConfig): Boolean {
        val startingDist = distance(stroke.startingPoint(), points.first())
        val endingDist = distance(stroke.endingPoint(), points.last())
        return startingDist <= config.startAndEndDistanceThreshold * leniency &&
            endingDist <= config.startAndEndDistanceThreshold * leniency
    }

    private fun edgeVectors(points: List<Point>): List<Point> {
        if (points.size < 2) return emptyList()
        val vectors = ArrayList<Point>(points.size - 1)
        var last = points[0]
        for (i in 1 until points.size) {
            val p = points[i]
            vectors.add(subtract(p, last))
            last = p
        }
        return vectors
    }

    private fun directionMatches(points: List<Point>, stroke: Stroke, config: StrokeMatchConfig): Boolean {
        val edgeVectors = edgeVectors(points)
        val strokeVectors = stroke.vectors()
        if (edgeVectors.isEmpty() || strokeVectors.isEmpty()) return false
        val similarities = edgeVectors.map { edgeVector ->
            var maxSim = -1.0
            for (strokeVector in strokeVectors) {
                maxSim = max(maxSim, cosineSimilarity(strokeVector, edgeVector))
            }
            maxSim
        }
        val avgSimilarity = similarities.average()
        return avgSimilarity > config.cosineSimilarityThreshold
    }

    private fun lengthMatches(points: List<Point>, stroke: Stroke, leniency: Double, config: StrokeMatchConfig): Boolean {
        val userLen = length(points) + 25.0
        val strokeLen = stroke.length() + 25.0
        return (leniency * userLen) / strokeLen >= config.minLengthThreshold
    }

    private fun shapeFit(curve1: List<Point>, curve2: List<Point>, leniency: Double, config: StrokeMatchConfig): Boolean {
        val norm1 = normalizeCurve(curve1)
        val norm2 = normalizeCurve(curve2)
        var minDist = Double.POSITIVE_INFINITY
        for (theta in config.shapeFitRotations) {
            val dist = frechetDist(norm1, rotate(norm2, theta))
            if (dist < minDist) minDist = dist
        }
        return minDist <= config.frechetThreshold * leniency
    }

    private fun getMatchData(
        points: List<Point>,
        stroke: Stroke,
        options: StrokeMatchOptions,
        config: StrokeMatchConfig,
    ): StrokeMatchResult {
        val leniency = options.leniency
        val avgDist = stroke.averageDistance(points)
        val distMod = if (options.isOutlineVisible || stroke.strokeNum > 0) 0.5 else 1.0
        val withinDist = avgDist <= options.averageDistanceThreshold * distMod * leniency
        if (!withinDist) {
            return StrokeMatchResult(isMatch = false, meta = StrokeMatchMeta(false), avgDist = avgDist)
        }

        val startAndEnd = startAndEndMatches(points, stroke, leniency, config)
        val direction = directionMatches(points, stroke, config)
        val shape = shapeFit(points, stroke.points, leniency, config)
        val lenMatch = lengthMatches(points, stroke, leniency, config)
        val isMatch = withinDist && startAndEnd && direction && shape && lenMatch

        if (options.checkBackwards && !isMatch) {
            val reversed = points.asReversed()
            val backwards = getMatchData(reversed, stroke, options.copy(checkBackwards = false), config)
            if (backwards.isMatch) {
                return StrokeMatchResult(isMatch = false, meta = StrokeMatchMeta(true), avgDist = avgDist)
            }
        }

        return StrokeMatchResult(isMatch = isMatch, meta = StrokeMatchMeta(false), avgDist = avgDist)
    }
}

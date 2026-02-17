package com.hanzi.learner.character_writer.match

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class StrokeMatchConfigTest {
    @Test
    fun defaultConfig_matchesCurrentDefaults() {
        val config = StrokeMatchConfig()
        assertEquals(0.0, config.cosineSimilarityThreshold, 0.0)
        assertEquals(250.0, config.startAndEndDistanceThreshold, 0.0)
        assertEquals(0.4, config.frechetThreshold, 0.0)
        assertEquals(0.35, config.minLengthThreshold, 0.0)

        assertArrayEquals(
            doubleArrayOf(PI / 16.0, PI / 32.0, 0.0, -PI / 32.0, -PI / 16.0),
            config.shapeFitRotations,
            0.0,
        )
    }
}


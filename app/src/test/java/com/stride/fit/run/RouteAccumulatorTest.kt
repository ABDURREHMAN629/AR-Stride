package com.stride.fit.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RouteAccumulatorTest {

    /** Metres per degree of latitude, near enough for tests. */
    private val mPerDegLat = 111_320.0

    private fun fix(
        acc: RouteAccumulator,
        northM: Double,
        ts: Long,
        accuracy: Float = 6f
    ) = acc.offer(
        rawLat = 31.5 + northM / mPerDegLat,
        rawLon = 73.4,
        altitude = 0.0,
        accuracyM = accuracy,
        speedMps = 1.4f,
        tsMs = ts,
        paused = false
    )

    @Test
    fun `a steady walk measures close to the true distance`() {
        val acc = RouteAccumulator()
        // 200 m due north at 1.4 m/s, one fix a second.
        for (s in 0..143) fix(acc, northM = s * 1.4, ts = s * 1000L)
        assertEquals(200.0, acc.totalM, 12.0)
    }

    @Test
    fun `a coarse fix jittering on the spot adds no distance`() {
        val acc = RouteAccumulator()
        val rng = Random(1)
        // Standing still for two minutes with a 35 m fix wandering +-10 m.
        for (s in 0..120) {
            fix(acc, northM = (rng.nextDouble() * 2 - 1) * 10, ts = s * 1000L, accuracy = 35f)
        }
        assertTrue("invented ${acc.totalM} m while standing still", acc.totalM < 5.0)
    }

    @Test
    fun `fixes worse than the accuracy limit are ignored`() {
        val acc = RouteAccumulator()
        fix(acc, 0.0, 0L, accuracy = 6f)
        val rejected = fix(acc, 500.0, 1000L, accuracy = 80f)
        assertEquals(null, rejected)
        assertEquals(0.0, acc.totalM, 0.001)
    }

    @Test
    fun `reanchoring after a gap does not count the gap as a jump`() {
        val acc = RouteAccumulator()
        for (s in 0..71) fix(acc, northM = s * 1.4, ts = s * 1000L)      // ~100 m
        val beforeGap = acc.totalM

        // GPS lost for 60 s while walking another ~84 m; the steps cover that.
        acc.reanchor()

        // GPS returns 84 m further on and the walk continues for ~100 m.
        val resume = 72 * 1.4 + 84.0
        for (s in 0..71) fix(acc, northM = resume + s * 1.4, ts = (132 + s) * 1000L)

        val afterGap = acc.totalM - beforeGap
        // Only the ~100 m walked after the gap, not ~184 m including the jump.
        assertEquals(100.0, afterGap, 12.0)
    }
}

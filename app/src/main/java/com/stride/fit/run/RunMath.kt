package com.stride.fit.run

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

object RunMath {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Great-circle distance in metres. */
    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(a), sqrt(1 - a))
    }
}

/**
 * One-dimensional Kalman filter over latitude and longitude, using the reported GPS
 * accuracy as the measurement variance. Integrating raw GPS wander straight into a
 * distance total is what makes naive trackers report a kilometre for a walk to the shop.
 */
class GeoKalman(private val processNoiseMps: Double = 1.4) {

    var lat: Double = 0.0
        private set
    var lon: Double = 0.0
        private set

    private var variance = -1.0
    private var timeMs = 0L

    val hasFix: Boolean get() = variance >= 0

    fun reset() {
        variance = -1.0
        timeMs = 0L
    }

    fun process(newLat: Double, newLon: Double, accuracyM: Float, tsMs: Long) {
        val accuracy = max(accuracyM.toDouble(), 1.0)

        if (variance < 0) {
            lat = newLat
            lon = newLon
            variance = accuracy * accuracy
            timeMs = tsMs
            return
        }

        val dtSeconds = (tsMs - timeMs).coerceAtLeast(0) / 1000.0
        if (dtSeconds > 0) {
            variance += dtSeconds * processNoiseMps * processNoiseMps
            timeMs = tsMs
        }

        val gain = variance / (variance + accuracy * accuracy)
        lat += gain * (newLat - lat)
        lon += gain * (newLon - lon)
        variance = (1 - gain) * variance
    }
}

/**
 * Turns a stream of filtered fixes into distance, elevation and splits. Every rule exists
 * to stop the tracker inventing distance: fixes worse than [MAX_ACCURACY_M] are ignored,
 * movement below [MIN_STEP_M] or [JITTER_FRACTION] of the fix's uncertainty is jitter,
 * speeds above [MAX_SPEED_MPS] are glitches, altitude is smoothed before a climb counts.
 */
class RouteAccumulator {

    companion object {
        /** Was 25 m; the test device averaged 75 m accuracy and lost nearly every fix. */
        const val MAX_ACCURACY_M = 40f
        const val MIN_STEP_M = 2.5

        /** Movement below this share of the fix's own uncertainty is noise, not progress. */
        const val JITTER_FRACTION = 0.35
        const val MAX_SPEED_MPS = 9.0        // 32 km/h: beyond any sustained run
        const val ELEV_THRESHOLD_M = 1.0
        const val SPLIT_METRES = 1000.0
    }

    data class Accepted(
        val lat: Double,
        val lon: Double,
        val altitude: Double,
        val accuracy: Float,
        val speedMps: Float,
        val ts: Long,
        val deltaM: Double,
        val cumulativeM: Double,
        val splitCompleted: Split?
    )

    data class Split(
        val index: Int,
        val meters: Double,
        val durationMs: Long,
        val paceSecPerKm: Int,
        val elevDeltaM: Double
    )

    private val kalman = GeoKalman()

    var totalM: Double = 0.0
        private set
    var elevGainM: Double = 0.0
        private set
    var elevLossM: Double = 0.0
        private set

    private var lastLat = 0.0
    private var lastLon = 0.0
    private var lastTs = 0L
    private var hasPrevious = false

    private var smoothedAltitude = Double.NaN
    private var splitIndex = 0
    private var splitStartMs = 0L
    private var splitStartAltitude = 0.0
    private var lastSplitBoundaryM = 0.0

    /**
     * Forget the last position but keep the distance. Used when GPS returns after a gap
     * the step counter already covered, which would otherwise count it twice as one jump.
     */
    fun reanchor() {
        kalman.reset()
        hasPrevious = false
        smoothedAltitude = Double.NaN
    }

    fun reset() {
        kalman.reset()
        totalM = 0.0
        elevGainM = 0.0
        elevLossM = 0.0
        hasPrevious = false
        smoothedAltitude = Double.NaN
        splitIndex = 0
        splitStartMs = 0L
        lastSplitBoundaryM = 0.0
    }

    /** @return the accepted point, or null when the fix was rejected. */
    fun offer(
        rawLat: Double,
        rawLon: Double,
        altitude: Double,
        accuracyM: Float,
        speedMps: Float,
        tsMs: Long,
        paused: Boolean
    ): Accepted? {
        if (accuracyM <= 0f || accuracyM > MAX_ACCURACY_M) return null

        kalman.process(rawLat, rawLon, accuracyM, tsMs)
        val lat = kalman.lat
        val lon = kalman.lon

        if (!hasPrevious) {
            hasPrevious = true
            lastLat = lat
            lastLon = lon
            lastTs = tsMs
            splitStartMs = tsMs
            smoothedAltitude = altitude
            splitStartAltitude = altitude
            return Accepted(lat, lon, altitude, accuracyM, speedMps, tsMs, 0.0, 0.0, null)
        }

        val delta = RunMath.haversine(lastLat, lastLon, lat, lon)
        val dtSeconds = (tsMs - lastTs).coerceAtLeast(1) / 1000.0

        // Reject impossible jumps rather than smearing them into the total.
        if (delta / dtSeconds > MAX_SPEED_MPS) return null

        // While paused we keep the filter warm but do not accumulate anything.
        if (paused) {
            lastLat = lat
            lastLon = lon
            lastTs = tsMs
            return null
        }

        if (delta < max(MIN_STEP_M, accuracyM * JITTER_FRACTION)) {
            // Keep the previous anchor *and* its timestamp: at walking pace one second
            // covers well under the floor, so displacement must build across several fixes.
            // Resetting the clock here would make the speed check reject real walking.
            return null
        }

        totalM += delta
        lastLat = lat
        lastLon = lon
        lastTs = tsMs

        trackElevation(altitude)

        val split = maybeCloseSplit(tsMs, altitude)

        return Accepted(lat, lon, altitude, accuracyM, speedMps, tsMs, delta, totalM, split)
    }

    private fun trackElevation(altitude: Double) {
        if (altitude == 0.0) return
        if (smoothedAltitude.isNaN()) {
            smoothedAltitude = altitude
            return
        }
        val next = smoothedAltitude + (altitude - smoothedAltitude) * 0.25
        val change = next - smoothedAltitude
        if (abs(change) >= ELEV_THRESHOLD_M) {
            if (change > 0) elevGainM += change else elevLossM += -change
        }
        smoothedAltitude = next
    }

    private fun maybeCloseSplit(tsMs: Long, altitude: Double): Split? {
        if (totalM - lastSplitBoundaryM < SPLIT_METRES) return null
        splitIndex++
        val meters = totalM - lastSplitBoundaryM
        val durationMs = tsMs - splitStartMs
        val pace = if (meters > 0) (durationMs / 1000.0 / (meters / 1000.0)).toInt() else 0
        val split = Split(
            index = splitIndex,
            meters = meters,
            durationMs = durationMs,
            paceSecPerKm = pace,
            elevDeltaM = altitude - splitStartAltitude
        )
        lastSplitBoundaryM = totalM
        splitStartMs = tsMs
        splitStartAltitude = altitude
        return split
    }
}

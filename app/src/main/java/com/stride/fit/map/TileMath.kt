package com.stride.fit.map

import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/** One map tile: a 256 x 256 image at a given zoom. */
data class TileKey(val z: Int, val x: Int, val y: Int) {
    val path: String get() = "$z/$x/$y"
}

/**
 * Web Mercator tile arithmetic - the same scheme every slippy map uses. At zoom z the
 * world is a 2^z by 2^z grid of 256 px tiles, so it is 256 * 2^z pixels across;
 * everything below converts in and out of that pixel space.
 */
object TileMath {

    const val TILE_SIZE = 256

    fun worldSizePx(zoom: Int): Double = TILE_SIZE * 2.0.pow(zoom)

    fun lonToWorldX(lon: Double, zoom: Int): Double =
        (lon + 180.0) / 360.0 * worldSizePx(zoom)

    fun latToWorldY(lat: Double, zoom: Int): Double {
        val clamped = lat.coerceIn(-85.05112878, 85.05112878)
        val rad = Math.toRadians(clamped)
        val mercator = ln(tan(rad) + 1.0 / cos(rad))
        return (1.0 - mercator / PI) / 2.0 * worldSizePx(zoom)
    }

    fun worldXToLon(x: Double, zoom: Int): Double =
        x / worldSizePx(zoom) * 360.0 - 180.0

    fun worldYToLat(y: Double, zoom: Int): Double {
        val n = PI - 2.0 * PI * y / worldSizePx(zoom)
        return Math.toDegrees(atan(sinh(n)))
    }

    fun tileX(lon: Double, zoom: Int): Int =
        floor(lonToWorldX(lon, zoom) / TILE_SIZE).toInt()
            .coerceIn(0, (1 shl zoom) - 1)

    fun tileY(lat: Double, zoom: Int): Int =
        floor(latToWorldY(lat, zoom) / TILE_SIZE).toInt()
            .coerceIn(0, (1 shl zoom) - 1)

    /** Ground resolution in metres per pixel, which is latitude dependent. */
    fun metresPerPixel(lat: Double, zoom: Int): Double =
        156543.03392 * cos(Math.toRadians(lat)) / 2.0.pow(zoom)

    /** Largest zoom at which a box still fits a viewport, to frame a recorded route. */
    fun zoomToFit(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        viewWidthPx: Int,
        viewHeightPx: Int,
        minZoom: Int,
        maxZoom: Int
    ): Int {
        if (viewWidthPx <= 0 || viewHeightPx <= 0) return minZoom
        for (z in maxZoom downTo minZoom) {
            val w = lonToWorldX(maxLon, z) - lonToWorldX(minLon, z)
            val h = latToWorldY(minLat, z) - latToWorldY(maxLat, z)
            if (w <= viewWidthPx && h <= viewHeightPx) return z
        }
        return minZoom
    }

    /** Every tile covering a lat/lon box at one zoom. */
    fun tilesForBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        zoom: Int
    ): List<TileKey> {
        val x0 = tileX(minLon, zoom)
        val x1 = tileX(maxLon, zoom)
        val y0 = tileY(maxLat, zoom)     // north edge is the smaller y
        val y1 = tileY(minLat, zoom)
        val out = ArrayList<TileKey>((x1 - x0 + 1) * (y1 - y0 + 1))
        for (x in x0..x1) for (y in y0..y1) out += TileKey(zoom, x, y)
        return out
    }

    /** A square box of the given radius; longitude degrees shrink with latitude. */
    fun boundsAround(lat: Double, lon: Double, radiusKm: Double): Bounds {
        val latDelta = radiusKm / 111.32
        val lonDelta = radiusKm / (111.32 * cos(Math.toRadians(lat)).coerceAtLeast(0.01))
        return Bounds(
            minLat = (lat - latDelta).coerceIn(-85.0, 85.0),
            maxLat = (lat + latDelta).coerceIn(-85.0, 85.0),
            minLon = (lon - lonDelta).coerceIn(-180.0, 180.0),
            maxLon = (lon + lonDelta).coerceIn(-180.0, 180.0)
        )
    }

    fun boundsOf(points: List<Pair<Double, Double>>): Bounds? {
        if (points.isEmpty()) return null
        return Bounds(
            minLat = points.minOf { it.first },
            maxLat = points.maxOf { it.first },
            minLon = points.minOf { it.second },
            maxLon = points.maxOf { it.second }
        )
    }

    /** Count the tiles a download would need, so the size can be shown before starting. */
    fun countTiles(bounds: Bounds, minZoom: Int, maxZoom: Int): Int =
        (minZoom..maxZoom).sumOf {
            tilesForBounds(bounds.minLat, bounds.maxLat, bounds.minLon, bounds.maxLon, it).size
        }
}

data class Bounds(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
) {
    val centerLat: Double get() = (minLat + maxLat) / 2
    val centerLon: Double get() = (minLon + maxLon) / 2

    /** Grow the box by a fraction of its size so a route is not drawn against the edge. */
    fun padded(fraction: Double): Bounds {
        val latPad = ((maxLat - minLat) * fraction).coerceAtLeast(0.0009)
        val lonPad = ((maxLon - minLon) * fraction).coerceAtLeast(0.0009)
        return Bounds(
            minLat = (minLat - latPad).coerceIn(-85.0, 85.0),
            maxLat = (maxLat + latPad).coerceIn(-85.0, 85.0),
            minLon = (minLon - lonPad).coerceIn(-180.0, 180.0),
            maxLon = (maxLon + lonPad).coerceIn(-180.0, 180.0)
        )
    }

    fun contains(lat: Double, lon: Double): Boolean =
        lat in minLat..maxLat && lon in minLon..maxLon
}

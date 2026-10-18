package com.stride.fit.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/**
 * The route drawn on a real map, using only tiles already saved on this device.
 *
 * Nothing here reaches for the network. If a tile is missing the map simply leaves
 * that square blank and still draws the route, so a run recorded outside any
 * downloaded area is never lost - it just shows the bare trace instead.
 */
@Composable
fun RouteMap(
    points: List<Pair<Double, Double>>,
    tileStore: TileStore,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    routeColor: Color = MaterialTheme.colorScheme.secondary,
    minZoom: Int = TileDownloader.DEFAULT_MIN_ZOOM,
    maxZoom: Int = TileDownloader.DEFAULT_MAX_ZOOM,
    showMissingHint: Boolean = true
) {
    val density = LocalDensity.current
    val startColor = MaterialTheme.colorScheme.primary
    val endColor = MaterialTheme.colorScheme.tertiary
    val emptyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val backdrop = MaterialTheme.colorScheme.surfaceVariant

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(backdrop)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }.toInt()
        val heightPx = with(density) { maxHeight.toPx() }.toInt()

        val bounds = remember(points) { TileMath.boundsOf(points)?.padded(0.18) }

        if (bounds == null || points.size < 2 || widthPx <= 0 || heightPx <= 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No route yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = emptyColor
                )
            }
            return@BoxWithConstraints
        }

        val zoom = remember(bounds, widthPx, heightPx) {
            TileMath.zoomToFit(
                bounds.minLat, bounds.maxLat, bounds.minLon, bounds.maxLon,
                widthPx, heightPx, minZoom, maxZoom
            )
        }

        // Top-left corner of the viewport, in world pixels at this zoom.
        val originX = remember(bounds, zoom, widthPx) {
            TileMath.lonToWorldX(bounds.centerLon, zoom) - widthPx / 2.0
        }
        val originY = remember(bounds, zoom, heightPx) {
            TileMath.latToWorldY(bounds.centerLat, zoom) - heightPx / 2.0
        }

        val visibleTiles = remember(originX, originY, zoom, widthPx, heightPx) {
            val maxIndex = (1 shl zoom) - 1
            val x0 = floor(originX / TileMath.TILE_SIZE).toInt()
            val x1 = floor((originX + widthPx) / TileMath.TILE_SIZE).toInt()
            val y0 = floor(originY / TileMath.TILE_SIZE).toInt()
            val y1 = floor((originY + heightPx) / TileMath.TILE_SIZE).toInt()
            buildList {
                for (x in x0..x1) for (y in y0..y1) {
                    if (x in 0..maxIndex && y in 0..maxIndex) add(TileKey(zoom, x, y))
                }
            }
        }

        val loaded = remember { mutableStateMapOf<String, ImageBitmap>() }

        LaunchedEffect(visibleTiles) {
            visibleTiles.forEach { key ->
                if (!loaded.containsKey(key.path)) {
                    tileStore.load(key)?.let { loaded[key.path] = it }
                }
            }
        }

        val haveAnyTile = visibleTiles.any { loaded.containsKey(it.path) }

        Canvas(Modifier.fillMaxSize()) {
            visibleTiles.forEach { key ->
                val image = loaded[key.path] ?: return@forEach
                val left = (key.x * TileMath.TILE_SIZE - originX).toFloat()
                val top = (key.y * TileMath.TILE_SIZE - originY).toFloat()
                drawImage(
                    image = image,
                    dstOffset = IntOffset(left.toInt(), top.toInt()),
                    dstSize = IntSize(TileMath.TILE_SIZE, TileMath.TILE_SIZE)
                )
            }

            drawRoute(points, zoom, originX, originY, routeColor, startColor, endColor)
        }

        if (!haveAnyTile && showMissingHint) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    "No offline map saved for this area. Download one from the Run tab " +
                        "to see streets behind your route.",
                    style = MaterialTheme.typography.bodySmall,
                    color = emptyColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** The polyline itself, plus a casing so it stays legible over busy map imagery. */
private fun DrawScope.drawRoute(
    points: List<Pair<Double, Double>>,
    zoom: Int,
    originX: Double,
    originY: Double,
    routeColor: Color,
    startColor: Color,
    endColor: Color
) {
    fun project(index: Int): Offset {
        val (lat, lon) = points[index]
        return Offset(
            (TileMath.lonToWorldX(lon, zoom) - originX).toFloat(),
            (TileMath.latToWorldY(lat, zoom) - originY).toFloat()
        )
    }

    val path = Path().apply {
        val first = project(0)
        moveTo(first.x, first.y)
        for (i in 1 until points.size) {
            val p = project(i)
            lineTo(p.x, p.y)
        }
    }

    drawPath(
        path,
        Color.White.copy(alpha = 0.85f),
        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path,
        routeColor,
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val start = project(0)
    val end = project(points.size - 1)
    drawCircle(Color.White, radius = 7.dp.toPx(), center = start)
    drawCircle(startColor, radius = 5.dp.toPx(), center = start)
    drawCircle(Color.White, radius = 7.dp.toPx(), center = end)
    drawCircle(endColor, radius = 5.dp.toPx(), center = end)
}

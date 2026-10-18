package com.stride.fit.map

import android.util.Log
import com.stride.fit.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.UnknownHostException
import java.net.URL

/** Live state of an area download. */
data class DownloadProgress(
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val bytes: Long = 0,
    val message: String = ""
) {
    val fraction: Float get() = if (total <= 0) 0f else (done + skipped).toFloat() / total
}

/**
 * Downloads map tiles for an area so the map works with no connection afterwards.
 *
 * The only part of Stride that touches the network, and only ever for map imagery - no
 * fitness data is sent anywhere, ever. OpenStreetMap's tile policy asks for a real
 * User-Agent, no heavy parallelism and no bulk downloading, so this is deliberately
 * polite: one request at a time, a delay between each, a hard ceiling per area, and
 * nothing already on disk is refetched. For more, point [urlTemplate] at a paid provider.
 */
class TileDownloader(
    private val store: TileStore,
    private val urlTemplate: String = OSM_TEMPLATE,
    private val userAgent: String = USER_AGENT
) {

    companion object {
        private const val TAG = "TileDownloader"

        const val OSM_TEMPLATE = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

        /** OSM requires a genuine identifying agent; anonymous clients get blocked. */
        const val USER_AGENT = "AR-Stride/1.2 (offline fitness app; personal use)"

        /** Politeness gap between requests. */
        private const val REQUEST_GAP_MS = 120L

        /** A dropped packet should not leave a permanent hole in a region. */
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_BACKOFF_MS = 400L

        /** This many unresolvable hosts in a row means the network is unavailable. */
        private const val FAILURES_BEFORE_ABORT = 8

        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 20_000

        /** Refuse to start an area that would hammer the tile server. */
        const val MAX_TILES_PER_REGION = 2_500

        const val DEFAULT_MIN_ZOOM = 12
        const val DEFAULT_MAX_ZOOM = 16
    }

    /**
     * Fetch every tile for [bounds] across the zoom range, reporting progress.
     * Cancelling the calling coroutine stops it; tiles already written are kept.
     */
    suspend fun download(
        bounds: Bounds,
        minZoom: Int = DEFAULT_MIN_ZOOM,
        maxZoom: Int = DEFAULT_MAX_ZOOM,
        onProgress: suspend (DownloadProgress) -> Unit
    ): DownloadProgress = withContext(Dispatchers.IO) {

        val keys = (minZoom..maxZoom).flatMap {
            TileMath.tilesForBounds(bounds.minLat, bounds.maxLat, bounds.minLon, bounds.maxLon, it)
        }

        if (keys.size > MAX_TILES_PER_REGION) {
            val refused = DownloadProgress(
                running = false,
                total = keys.size,
                message = "That area needs ${keys.size} tiles, over the ${MAX_TILES_PER_REGION} " +
                    "limit. Choose a smaller radius or a lower maximum zoom."
            )
            onProgress(refused)
            return@withContext refused
        }

        var progress = DownloadProgress(
            running = true,
            total = keys.size,
            message = "Downloading ${keys.size} tiles"
        )
        onProgress(progress)

        var consecutiveNetworkFailures = 0

        for (key in keys) {
            currentCoroutineContext().ensureActive()

            if (store.has(key)) {
                progress = progress.copy(skipped = progress.skipped + 1)
                onProgress(progress)
                continue
            }

            val result = fetchWithRetry(key)
            progress = if (result.bytes != null && store.write(key, result.bytes)) {
                consecutiveNetworkFailures = 0
                progress.copy(done = progress.done + 1, bytes = progress.bytes + result.bytes.size)
            } else {
                if (result.networkUnreachable) consecutiveNetworkFailures++
                else consecutiveNetworkFailures = 0
                progress.copy(failed = progress.failed + 1)
            }
            onProgress(progress)

            // Nothing is getting through: stop rather than grind through thousands.
            if (consecutiveNetworkFailures >= FAILURES_BEFORE_ABORT) {
                val aborted = progress.copy(
                    running = false,
                    message = "No network access. ${Brand.NAME} could not reach the map server at " +
                        "all.\n\nIf other apps can reach the internet, this phone is blocking " +
                        "network access for ${Brand.NAME} specifically - some Android versions " +
                        "start sideloaded apps with it switched off. Open Settings, Apps, " +
                        "${Brand.NAME}, and allow network access on Wi-Fi and mobile data. Then " +
                        "try again."
                )
                onProgress(aborted)
                return@withContext aborted
            }

            delay(REQUEST_GAP_MS)
        }

        val finished = progress.copy(
            running = false,
            message = buildString {
                append("Saved ${progress.done} tiles")
                if (progress.skipped > 0) append(", ${progress.skipped} already had")
                if (progress.failed > 0) append(", ${progress.failed} failed")
            }
        )
        onProgress(finished)
        finished
    }

    private data class FetchResult(
        val bytes: ByteArray?,
        /** True when the host could not be reached at all, as opposed to a bad tile. */
        val networkUnreachable: Boolean
    )

    /** Retries, so one dropped packet cannot leave a permanent hole in a region. */
    private suspend fun fetchWithRetry(key: TileKey): FetchResult {
        var last = FetchResult(null, false)
        repeat(MAX_ATTEMPTS) { attempt ->
            currentCoroutineContext().ensureActive()
            last = fetch(key)
            if (last.bytes != null) return last
            if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_BACKOFF_MS * (attempt + 1))
        }
        return last
    }

    /** Pull one tile. Returns null bytes on any failure; the caller counts it. */
    private fun fetch(key: TileKey): FetchResult {
        val url = urlTemplate
            .replace("{z}", key.z.toString())
            .replace("{x}", key.x.toString())
            .replace("{y}", key.y.toString())

        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "image/png,image/*")
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Tile ${key.path} returned HTTP $code")
                // Drain the error body so the socket returns to the keep-alive pool.
                runCatching { connection.errorStream?.use { it.readBytes() } }
                return FetchResult(null, networkUnreachable = false)
            }
            val bytes = connection.inputStream.use { it.readBytes() }
            // A truncated or error-page response is not a tile.
            FetchResult(if (bytes.size < 100) null else bytes, networkUnreachable = false)
        } catch (e: UnknownHostException) {
            // No connection, or the OS denying this app network access: LineageOS-based
            // ROMs have a per-app network switch that starts off for sideloaded apps.
            Log.w(TAG, "Tile ${key.path}: cannot resolve host (${e.message})")
            FetchResult(null, networkUnreachable = true)
        } catch (e: IOException) {
            Log.w(TAG, "Tile ${key.path} failed: ${e.message}")
            FetchResult(null, networkUnreachable = false)
        } catch (e: Exception) {
            Log.w(TAG, "Tile ${key.path} failed: ${e.message}")
            FetchResult(null, networkUnreachable = false)
        }
        // No disconnect() or finally block: use() has closed the streams, and leaving the
        // socket in the keep-alive pool lets the next tile skip the handshake.
    }

    /** Rough size estimate so the user can decide before committing to a download. */
    fun estimateBytes(tileCount: Int): Long = tileCount * 14_000L
}

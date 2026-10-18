package com.stride.fit.map

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Where downloaded map tiles live. Files, not a database: a tile is already a compressed
 * PNG and the filesystem handles a few thousand perfectly well. Under filesDir rather
 * than cacheDir so Android will not quietly delete the maps downloaded for a trip.
 */
class TileStore(context: Context) {

    private val root = File(context.filesDir, "tiles").apply { mkdirs() }

    // Decoding a PNG on every frame would make panning stutter.
    private val memory = object : LruCache<String, ImageBitmap>(120) {
        override fun sizeOf(key: String, value: ImageBitmap) = 1
    }

    fun fileFor(key: TileKey): File = File(root, "${key.z}/${key.x}/${key.y}.png")

    fun has(key: TileKey): Boolean = fileFor(key).let { it.exists() && it.length() > 0 }

    fun cached(key: TileKey): ImageBitmap? = memory.get(key.path)

    suspend fun load(key: TileKey): ImageBitmap? {
        memory.get(key.path)?.let { return it }
        return withContext(Dispatchers.IO) {
            val file = fileFor(key)
            if (!file.exists() || file.length() == 0L) return@withContext null
            val bitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
                ?: return@withContext null
            val image = bitmap.asImageBitmap()
            memory.put(key.path, image)
            image
        }
    }

    suspend fun write(key: TileKey, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileFor(key)
            file.parentFile?.mkdirs()
            // Write to a temporary file first so an interrupted download never leaves a
            // half-written tile that looks valid.
            val temp = File(file.parentFile, "${key.y}.part")
            temp.writeBytes(bytes)
            if (file.exists()) file.delete()
            temp.renameTo(file)
        }.getOrDefault(false)
    }

    suspend fun totalBytes(): Long = withContext(Dispatchers.IO) {
        root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun tileCount(): Int = withContext(Dispatchers.IO) {
        root.walkTopDown().count { it.isFile && it.extension == "png" }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        root.deleteRecursively()
        root.mkdirs()
        memory.evictAll()
    }

    /** Remove only the tiles a region covers that no other saved region needs. */
    suspend fun deleteTiles(keys: Collection<TileKey>) = withContext(Dispatchers.IO) {
        keys.forEach { key ->
            runCatching { fileFor(key).delete() }
            memory.remove(key.path)
        }
    }
}

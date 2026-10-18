package com.stride.fit.sensors

import android.content.Context
import android.util.Log
import java.io.File
import java.util.Locale

/**
 * A rolling on-device record of what the step classifier decided and why. When a real
 * walk is mis-read, this is the evidence needed to fix it rather than guess: every
 * non-still analysis window with its features, and every batch of steps with its verdict.
 *
 * Pull it over USB with
 *   adb pull /sdcard/Android/data/com.stride.fit/files/motion-log.csv
 * It never leaves the phone otherwise, and rotates at a few megabytes.
 */
class MotionLog(context: Context) {

    private val dir: File = context.getExternalFilesDir(null) ?: context.filesDir
    private val file = File(dir, FILE_NAME)
    private val previous = File(dir, "$FILE_NAME.old")

    private val buffer = StringBuilder()
    private var buffered = 0
    private val lock = Any()

    /** One classifier window. Still windows are skipped; they are most of the day. */
    fun window(
        atMs: Long,
        motion: MotionClass,
        f: MotionFeatures,
        gateOpen: Boolean,
        recognised: RecognisedActivity
    ) {
        if (motion == MotionClass.STILL) return
        append(
            String.format(
                Locale.US,
                "w,%d,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%s",
                atMs, motion.name, f.sd, f.p2p, f.kurtosis, f.dominantHz, f.periodicity,
                if (gateOpen) 1 else 0, recognised.name
            )
        )
    }

    /** One batch of hardware steps and what happened to it. */
    fun steps(atMs: Long, count: Int, verdict: Verdict, motion: MotionClass, cadenceSpm: Double) {
        append(
            String.format(
                Locale.US,
                "s,%d,%d,%s,%s,%.0f",
                atMs, count, verdict.name, motion.name, cadenceSpm
            )
        )
    }

    private fun append(line: String) {
        synchronized(lock) {
            buffer.append(line).append('\n')
            buffered++
        }
    }

    /** Write out whatever has been buffered. Cheap to call often. */
    fun flush() {
        val chunk: String
        synchronized(lock) {
            if (buffered == 0) return
            chunk = buffer.toString()
            buffer.setLength(0)
            buffered = 0
        }
        runCatching {
            if (file.length() > MAX_BYTES) {
                previous.delete()
                file.renameTo(previous)
            }
            if (!file.exists()) file.writeText(HEADER)
            file.appendText(chunk)
        }.onFailure { Log.w("MotionLog", "Could not write motion log: ${it.message}") }
    }

    private companion object {
        const val FILE_NAME = "motion-log.csv"
        const val MAX_BYTES = 4L * 1024 * 1024
        const val HEADER =
            "# w,time,class,sd,p2p,kurtosis,stepHz,periodicity,gateOpen,playServices\n" +
                "# s,time,steps,verdict,class,cadence\n"
    }
}

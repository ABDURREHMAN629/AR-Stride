package com.stride.fit.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Time {

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun today(): Long = LocalDate.now(zone()).toEpochDay()

    fun epochDayOf(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate().toEpochDay()

    fun startOfDayMs(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone()).toInstant().toEpochMilli()

    fun endOfDayMs(epochDay: Long): Long = startOfDayMs(epochDay + 1) - 1

    /** Minute buckets are plain UTC minutes; the day boundary is applied on read. */
    fun epochMinuteOf(millis: Long): Long = millis / 60_000L

    fun minuteRangeForDay(epochDay: Long): LongRange =
        epochMinuteOf(startOfDayMs(epochDay))..epochMinuteOf(endOfDayMs(epochDay))

    fun hourOfMinute(epochMinute: Long): Int =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMinute * 60_000L), zone()).hour

    fun dayLabel(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        val today = LocalDate.now(zone())
        return when (epochDay) {
            today.toEpochDay() -> "Today"
            today.toEpochDay() - 1 -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
        }
    }

    fun shortWeekday(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay)
            .format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))

    fun clockOf(millis: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone())
            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

    fun dateTimeOf(millis: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone())
            .format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault()))

    fun lastNDays(n: Int, endDay: Long = today()): List<Long> =
        ((endDay - n + 1)..endDay).toList()
}

object Fmt {

    fun steps(v: Int): String = "%,d".format(Locale.getDefault(), v)

    fun distance(meters: Double, metric: Boolean = true): String = if (metric) {
        if (meters < 1000) "${meters.roundToInt()} m"
        else String.format(Locale.getDefault(), "%.2f km", meters / 1000.0)
    } else {
        val miles = meters / 1609.344
        if (miles < 0.1) "${(meters * 3.28084).roundToInt()} ft"
        else String.format(Locale.getDefault(), "%.2f mi", miles)
    }

    fun distanceValue(meters: Double, metric: Boolean = true): String =
        if (metric) String.format(Locale.getDefault(), "%.2f", meters / 1000.0)
        else String.format(Locale.getDefault(), "%.2f", meters / 1609.344)

    fun distanceUnit(metric: Boolean = true): String = if (metric) "km" else "mi"

    fun kcal(v: Double): String = "${v.roundToInt()}"

    fun weight(kg: Double, metric: Boolean = true): String =
        if (metric) String.format(Locale.getDefault(), "%.1f kg", kg)
        else String.format(Locale.getDefault(), "%.1f lb", kg * 2.20462)

    /** h:mm:ss when the session runs past an hour, otherwise m:ss. */
    fun duration(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    fun clockDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    fun pace(secPerKm: Int, metric: Boolean = true): String {
        if (secPerKm <= 0) return "--:--"
        val adjusted = if (metric) secPerKm else (secPerKm * 1.609344).roundToInt()
        val m = adjusted / 60
        val s = adjusted % 60
        return String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    fun paceUnit(metric: Boolean = true): String = if (metric) "/km" else "/mi"

    fun speed(kmh: Double, metric: Boolean = true): String =
        if (metric) String.format(Locale.getDefault(), "%.1f km/h", kmh)
        else String.format(Locale.getDefault(), "%.1f mph", kmh / 1.609344)

    fun signed(v: Double, decimals: Int = 1): String {
        val sign = if (v > 0) "+" else if (v < 0) "-" else ""
        return sign + String.format(Locale.getDefault(), "%.${decimals}f", abs(v))
    }

    fun percent(fraction: Double): String = "${(fraction * 100).roundToInt()}%"

    fun oneDecimal(v: Double): String = String.format(Locale.getDefault(), "%.1f", v)
}

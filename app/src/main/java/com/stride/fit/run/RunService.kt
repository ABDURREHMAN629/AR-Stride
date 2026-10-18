package com.stride.fit.run

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.stride.fit.data.db.RunPoint
import com.stride.fit.data.db.RunSession
import com.stride.fit.data.db.RunSplit
import com.stride.fit.data.repo.StrideRepository
import com.stride.fit.health.HealthMath
import com.stride.fit.util.Fmt
import com.stride.fit.util.Notifications
import com.stride.fit.util.Time
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/** A single point of the live route, kept in memory for the on-screen trace. */
data class LivePoint(val lat: Double, val lon: Double, val ts: Long)

/** Where the distance figure is coming from right now. */
enum class DistanceSource(val label: String) {
    NONE(""),
    GPS("GPS"),
    STEPS("Steps"),
    MIXED("GPS + steps")
}

data class RunState(
    val active: Boolean = false,
    val paused: Boolean = false,
    val autoPaused: Boolean = false,
    val runId: Long = 0,
    val activity: String = "RUN",
    val startedAtMs: Long = 0,
    val elapsedMs: Long = 0,
    val movingMs: Long = 0,
    val distanceM: Double = 0.0,
    val gpsDistanceM: Double = 0.0,
    val stepDistanceM: Double = 0.0,
    val currentPaceSecPerKm: Int = 0,
    val avgPaceSecPerKm: Int = 0,
    val kcal: Double = 0.0,
    val elevGainM: Double = 0.0,
    val gpsAccuracyM: Float = 0f,
    val hasFix: Boolean = false,
    val locationOn: Boolean = true,
    val steps: Int = 0,
    val cadenceSpm: Int = 0,
    val source: DistanceSource = DistanceSource.NONE,
    val splits: List<RouteAccumulator.Split> = emptyList(),
    val route: List<LivePoint> = emptyList()
)

/**
 * Records a walk, run or hike from two measurements: GPS when there is a usable fix,
 * and the hardware step counter always - it works with location off and indoors.
 * Distance is GPS distance plus step distance covered only while GPS was unusable.
 */
class RunService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repo: StrideRepository

    private val accumulator = RouteAccumulator()
    private val route = mutableListOf<LivePoint>()
    private val splits = mutableListOf<RouteAccumulator.Split>()
    private val pendingPoints = mutableListOf<RunPoint>()

    private var fused: FusedLocationProviderClient? = null
    private var locationManager: LocationManager? = null
    private var sensorManager: SensorManager? = null
    private var usingFused = true
    private var switchedToRawGps = false

    private var runId = 0L
    private var startedAtMs = 0L
    private var pausedAccumulatedMs = 0L
    private var pauseStartedMs = 0L
    private var movingMs = 0L
    private var lastMoveTickMs = 0L
    private var autoPauseSince = 0L

    @Volatile private var lastGoodFixMs = 0L
    @Volatile private var lastLocationCallbackMs = 0L
    private var locationOnSinceMs = 0L
    private var locationWasOn = false
    private var gpsEverContributed = false

    // Written on the sensor thread, drained by the ticker.
    private val pendingSteps = AtomicLong(0)
    @Volatile private var lastRawSteps = -1L
    @Volatile private var lastStepMs = 0L
    private var runSteps = 0
    private var stepDistanceM = 0.0
    private var stepsEverContributed = false

    private val stepHistory = ArrayDeque<Pair<Long, Int>>()
    private val distanceHistory = ArrayDeque<Pair<Long, Double>>()

    private var kcal = 0.0
    private var splitStartDistance = 0.0
    private var splitStartMovingMs = 0L
    private var splitStartElev = 0.0

    private var weightKg = HealthMath.DEFAULT_WEIGHT_KG
    private var heightCm = HealthMath.DEFAULT_HEIGHT_CM
    private var male = HealthMath.DEFAULT_SEX_MALE
    private var autoPauseEnabled = true
    private var metric = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repo = StrideRepository.get(this)
        locationManager = getSystemService(LocationManager::class.java)
        sensorManager = getSystemService(SensorManager::class.java)
        Notifications.ensureChannels(this)
        scope.launch {
            val p = repo.profile.first()
            weightKg = p.weightKg
            heightCm = p.heightCm
            male = p.male
            autoPauseEnabled = p.autoPauseRuns
            metric = p.useMetric
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRun(intent.getStringExtra(EXTRA_ACTIVITY) ?: "RUN")
            ACTION_PAUSE -> pauseRun(auto = false)
            ACTION_RESUME -> resumeRun(auto = false)
            ACTION_STOP -> stopRun(discard = false)
            ACTION_DISCARD -> stopRun(discard = true)
        }
        return START_STICKY
    }


    private fun startRun(activity: String) {
        if (state.value.active) return

        startForeground(
            Notifications.ID_RUN,
            Notifications.runNotification(this, "Starting", "Waiting for GPS")
        )

        accumulator.reset()
        route.clear()
        splits.clear()
        pendingPoints.clear()
        stepHistory.clear()
        distanceHistory.clear()
        startedAtMs = System.currentTimeMillis()
        pausedAccumulatedMs = 0
        pauseStartedMs = 0
        movingMs = 0
        lastMoveTickMs = startedAtMs
        autoPauseSince = 0
        lastGoodFixMs = 0
        lastLocationCallbackMs = 0
        gpsEverContributed = false
        pendingSteps.set(0)
        lastRawSteps = -1
        lastStepMs = 0
        runSteps = 0
        stepDistanceM = 0.0
        stepsEverContributed = false
        kcal = 0.0
        splitStartDistance = 0.0
        splitStartMovingMs = 0
        splitStartElev = 0.0
        switchedToRawGps = false

        val on = isLocationOn()
        locationWasOn = on
        locationOnSinceMs = if (on) startedAtMs else 0L

        state.value = RunState(
            active = true,
            activity = activity,
            startedAtMs = startedAtMs,
            locationOn = on
        )

        scope.launch {
            runId = repo.runDao.insertSession(
                RunSession(startMs = startedAtMs, activity = activity, finished = false)
            )
            state.value = state.value.copy(runId = runId)
        }

        registerStepCounter()
        requestLocationUpdates()
        startTicker()
    }

    private fun pauseRun(auto: Boolean) {
        val s = state.value
        if (!s.active || s.paused) return
        pauseStartedMs = System.currentTimeMillis()
        state.value = s.copy(paused = true, autoPaused = auto)
    }

    private fun resumeRun(auto: Boolean) {
        val s = state.value
        if (!s.active || !s.paused) return
        if (pauseStartedMs > 0) {
            pausedAccumulatedMs += System.currentTimeMillis() - pauseStartedMs
            pauseStartedMs = 0
        }
        autoPauseSince = 0
        lastMoveTickMs = System.currentTimeMillis()
        state.value = s.copy(paused = false, autoPaused = false)
    }

    private fun stopRun(discard: Boolean) {
        val s = state.value
        if (!s.active) {
            stopSelf()
            return
        }
        stopLocationUpdates()
        unregisterStepCounter()

        val endMs = System.currentTimeMillis()
        val elapsed = endMs - startedAtMs - pausedAccumulatedMs -
                (if (pauseStartedMs > 0) endMs - pauseStartedMs else 0)
        val distance = accumulator.totalM + stepDistanceM
        val id = runId
        val steps = runSteps
        val movingMinutes = movingMs / 60_000.0
        val avgCadence = if (movingMinutes > 0.1) (steps / movingMinutes).roundToInt() else 0
        val note = when (sourceNow()) {
            DistanceSource.STEPS -> "Distance from steps - no GPS fix"
            DistanceSource.MIXED -> "GPS, with steps filling the gaps"
            else -> ""
        }

        val finalSplits = splits.toList()
        val finalPoints = pendingPoints.toList()
        pendingPoints.clear()
        val finalKcal = kcal

        scope.launch {
            if (discard || distance < 20.0) {
                repo.runDao.deletePoints(id)
                repo.runDao.deleteSplits(id)
                repo.runDao.deleteSession(id)
            } else {
                repo.runDao.insertPoints(finalPoints)
                finalSplits.forEach {
                    repo.runDao.insertSplit(
                        RunSplit(
                            runId = id,
                            index = it.index,
                            meters = it.meters,
                            durationMs = it.durationMs,
                            paceSecPerKm = it.paceSecPerKm,
                            elevDeltaM = it.elevDeltaM
                        )
                    )
                }
                repo.runDao.updateSession(
                    RunSession(
                        id = id,
                        startMs = startedAtMs,
                        endMs = endMs,
                        distanceM = distance,
                        movingMs = movingMs,
                        elapsedMs = elapsed,
                        kcal = finalKcal,
                        elevGainM = accumulator.elevGainM,
                        elevLossM = accumulator.elevLossM,
                        avgPaceSecPerKm = HealthMath.paceSecPerKm(distance, movingMs),
                        bestKmPaceSec = finalSplits.filter { it.paceSecPerKm > 0 }
                            .minOfOrNull { it.paceSecPerKm } ?: 0,
                        steps = steps,
                        avgCadenceSpm = avgCadence,
                        activity = s.activity,
                        finished = true,
                        note = note
                    )
                )
                repo.addWorkoutEnergy(Time.epochDayOf(startedAtMs), run = finalKcal)
            }
            state.value = RunState()
            stopForegroundCompat()
            stopSelf()
        }
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }


    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val raw = event.values[0].toLong()
            val previous = lastRawSteps
            lastRawSteps = raw
            // The first reading is only a baseline: the counter runs since boot.
            if (previous < 0 || raw <= previous) return
            val s = state.value
            // An auto-pause does not skip steps: they are what resumes the run.
            if (s.paused && !s.autoPaused) return
            pendingSteps.addAndGet(raw - previous)
            lastStepMs = System.currentTimeMillis()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun registerStepCounter() {
        val manager = sensorManager ?: return
        val counter = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        // No batching: a run wants every few steps, not a burst every 10 s.
        runCatching {
            manager.registerListener(stepListener, counter, SensorManager.SENSOR_DELAY_FASTEST, 0)
        }.onFailure { Log.w(TAG, "Step counter unavailable: ${it.message}") }
    }

    private fun unregisterStepCounter() {
        runCatching { sensorManager?.unregisterListener(stepListener) }
    }

    /** Stride for the cadence right now: running strides lengthen with cadence. */
    private fun strideM(cadenceSpm: Double): Double =
        if (cadenceSpm >= RUN_CADENCE_SPM) HealthMath.runningStrideM(heightCm, cadenceSpm)
        else HealthMath.walkingStrideM(heightCm, male, cadenceSpm.takeIf { it > 0 } ?: 105.0)


    private fun isLocationOn(): Boolean =
        runCatching { locationManager?.isLocationEnabled == true }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w(TAG, "Location permission missing; distance will come from steps")
            return
        }

        val client = runCatching { LocationServices.getFusedLocationProviderClient(this) }.getOrNull()
        if (client == null) {
            fallbackToLocationManager()
            return
        }
        fused = client
        usingFused = true
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(0f)
            .build()
        runCatching {
            // The failure arrives on the Task, not as a throw: catching the call alone
            // never falls back.
            client.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
                .addOnFailureListener {
                    Log.w(TAG, "Fused location refused (${it.message}); using raw GPS")
                    switchToRawGps()
                }
        }.onFailure { switchToRawGps() }
    }

    /** Play services is absent, sandboxed or silent: go straight to the GPS chip. */
    private fun switchToRawGps() {
        if (switchedToRawGps) return
        switchedToRawGps = true
        runCatching { fused?.removeLocationUpdates(fusedCallback) }
        fused = null
        fallbackToLocationManager()
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLocationManager() {
        usingFused = false
        val manager = locationManager ?: return
        runCatching {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 0f, rawListener, Looper.getMainLooper()
            )
        }.onFailure { Log.e(TAG, "No GPS provider available: ${it.message}") }
    }

    private fun stopLocationUpdates() {
        runCatching { fused?.removeLocationUpdates(fusedCallback) }
        runCatching { locationManager?.removeUpdates(rawListener) }
    }

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { onLocation(it) }
        }
    }

    private val rawListener = LocationListener { onLocation(it) }

    private fun onLocation(location: Location) {
        val s = state.value
        if (!s.active) return

        val now = System.currentTimeMillis()
        lastLocationCallbackMs = now
        val good = location.accuracy in 0.1f..RouteAccumulator.MAX_ACCURACY_M
        if (good) {
            // GPS is back after a gap the steps already covered. Start measuring from
            // here, not from the last fix before the gap, or the gap counts twice.
            if (lastGoodFixMs > 0 && now - lastGoodFixMs >= GPS_HEALTHY_MS) accumulator.reanchor()
            lastGoodFixMs = now
        }

        val accepted = accumulator.offer(
            rawLat = location.latitude,
            rawLon = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else 0.0,
            accuracyM = location.accuracy,
            speedMps = if (location.hasSpeed()) location.speed else 0f,
            tsMs = now,
            paused = s.paused && !s.autoPaused
        )

        state.value = state.value.copy(
            gpsAccuracyM = location.accuracy,
            hasFix = location.accuracy in 0.1f..RouteAccumulator.MAX_ACCURACY_M
        )

        if (accepted == null) return

        if (accepted.deltaM > 0) gpsEverContributed = true
        maybeAutoPause(now, moving = accepted.deltaM > 0)

        route += LivePoint(accepted.lat, accepted.lon, accepted.ts)
        pendingPoints += RunPoint(
            runId = runId,
            ts = accepted.ts,
            lat = accepted.lat,
            lon = accepted.lon,
            altitude = accepted.altitude,
            accuracyM = accepted.accuracy,
            speedMps = accepted.speedMps,
            cumulativeM = accepted.cumulativeM + stepDistanceM
        )

        // Flush to disk periodically so a crash mid-run does not lose the whole route.
        if (pendingPoints.size >= FLUSH_EVERY) {
            val batch = pendingPoints.toList()
            pendingPoints.clear()
            scope.launch { repo.runDao.insertPoints(batch) }
        }
    }

    private fun gpsHealthy(now: Long): Boolean =
        lastGoodFixMs > 0 && now - lastGoodFixMs < GPS_HEALTHY_MS

    private fun sourceNow(): DistanceSource = when {
        gpsEverContributed && stepsEverContributed -> DistanceSource.MIXED
        gpsEverContributed -> DistanceSource.GPS
        stepsEverContributed -> DistanceSource.STEPS
        else -> DistanceSource.NONE
    }

    /**
     * Pause at a traffic light, resume when the legs start. Steps count as movement, so
     * a slow walk on a coarse fix - or none - is never mistaken for standing still.
     */
    private fun maybeAutoPause(now: Long, moving: Boolean) {
        if (!autoPauseEnabled) return
        val s = state.value
        val stepping = lastStepMs > 0 && now - lastStepMs < STEPS_MOVING_MS
        if (moving || stepping) {
            if (s.autoPaused) resumeRun(auto = true)
            autoPauseSince = 0
            return
        }
        if (s.paused) return
        if (autoPauseSince == 0L) autoPauseSince = now
        else if (now - autoPauseSince > AUTO_PAUSE_AFTER_MS) pauseRun(auto = true)
    }


    private fun startTicker() {
        scope.launch {
            while (state.value.active) {
                delay(1000)
                if (!state.value.active) break
                tick(System.currentTimeMillis())
                updateNotification()
            }
        }
    }

    private fun tick(now: Long) {
        val on = isLocationOn()
        if (on && !locationWasOn) locationOnSinceMs = now
        locationWasOn = on

        // A fused request that has produced nothing for a while, with location on, is
        // a Play services that cannot deliver - common on ROMs that sandbox it.
        if (on && usingFused && !switchedToRawGps && locationOnSinceMs > 0 &&
            now - locationOnSinceMs > FUSED_WATCHDOG_MS &&
            lastLocationCallbackMs < locationOnSinceMs
        ) {
            Log.w(TAG, "Fused location silent for ${FUSED_WATCHDOG_MS / 1000}s; using raw GPS")
            switchToRawGps()
        }

        maybeAutoPause(now, moving = false)
        val s = state.value

        val newSteps = pendingSteps.getAndSet(0).toInt()
        if (!s.paused && newSteps > 0) runSteps += newSteps
        if (!s.paused) movingMs += now - lastMoveTickMs
        lastMoveTickMs = now

        stepHistory.addLast(now to runSteps)
        while (stepHistory.size > 1 && now - stepHistory.first.first > WINDOW_MS) stepHistory.removeFirst()
        val cadence = windowRate(stepHistory, now)

        // Steps only fill in distance while GPS cannot. When GPS is healthy it is the
        // better measure and counting both would double the distance.
        if (!s.paused && newSteps > 0 && !gpsHealthy(now)) {
            stepDistanceM += newSteps * strideM(cadence)
            stepsEverContributed = true
        }

        val distance = accumulator.totalM + stepDistanceM
        distanceHistory.addLast(now to distance)
        while (distanceHistory.size > 1 && now - distanceHistory.first.first > WINDOW_MS) {
            distanceHistory.removeFirst()
        }
        val recentSpeedMps = windowSpeed(distanceHistory)

        // Calories only for actual movement. A timer running while you stand still, or
        // while nothing is measuring you, is not burning anything extra.
        if (!s.paused && recentSpeedMps > 0.15) {
            val speedKmh = recentSpeedMps * 3.6
            val grade = if (distance > 0) accumulator.elevGainM / distance else 0.0
            kcal += HealthMath.kcal(HealthMath.ambulatoryMet(speedKmh, grade), weightKg, 1.0 / 60.0)
        }

        maybeCloseSplit(distance)

        val elapsed = now - startedAtMs - pausedAccumulatedMs -
                (if (pauseStartedMs > 0) now - pauseStartedMs else 0)

        state.value = s.copy(
            elapsedMs = elapsed,
            movingMs = movingMs,
            distanceM = distance,
            gpsDistanceM = accumulator.totalM,
            stepDistanceM = stepDistanceM,
            avgPaceSecPerKm = HealthMath.paceSecPerKm(distance, movingMs),
            currentPaceSecPerKm = if (recentSpeedMps > 0.3) (1000.0 / recentSpeedMps).roundToInt() else 0,
            kcal = kcal,
            elevGainM = accumulator.elevGainM,
            locationOn = on,
            hasFix = s.hasFix && gpsHealthy(now),
            steps = runSteps,
            cadenceSpm = cadence.roundToInt(),
            source = sourceNow(),
            splits = splits.toList(),
            route = route.toList()
        )
    }

    /** Kilometre splits on total distance, timed on moving time so pauses do not count. */
    private fun maybeCloseSplit(distance: Double) {
        if (distance - splitStartDistance < SPLIT_METRES) return
        val meters = distance - splitStartDistance
        val durationMs = movingMs - splitStartMovingMs
        splits += RouteAccumulator.Split(
            index = splits.size + 1,
            meters = meters,
            durationMs = durationMs,
            paceSecPerKm = HealthMath.paceSecPerKm(meters, durationMs),
            elevDeltaM = accumulator.elevGainM - splitStartElev
        )
        splitStartDistance = distance
        splitStartMovingMs = movingMs
        splitStartElev = accumulator.elevGainM
    }

    private fun windowRate(history: ArrayDeque<Pair<Long, Int>>, now: Long): Double {
        if (history.size < 2) return 0.0
        val (t0, c0) = history.first
        val (t1, c1) = history.last
        val spanMin = (t1 - t0) / 60_000.0
        if (spanMin < 5.0 / 60) return 0.0
        // No step for a few seconds means standing, whatever the average says.
        if (lastStepMs == 0L || now - lastStepMs > STEPS_MOVING_MS) return 0.0
        return (c1 - c0) / spanMin
    }

    private fun windowSpeed(history: ArrayDeque<Pair<Long, Double>>): Double {
        if (history.size < 2) return 0.0
        val (t0, d0) = history.first
        val (t1, d1) = history.last
        val spanS = (t1 - t0) / 1000.0
        if (spanS < 5.0) return 0.0
        return ((d1 - d0) / spanS).coerceAtLeast(0.0)
    }

    private fun updateNotification() {
        val s = state.value
        val title = buildString {
            append(Fmt.distance(s.distanceM, metric))
            append("  ")
            append(Fmt.duration(s.elapsedMs))
            if (s.paused) append(if (s.autoPaused) "  (auto-paused)" else "  (paused)")
        }
        val text = buildString {
            append("${Fmt.pace(s.avgPaceSecPerKm, metric)} ${Fmt.paceUnit(metric)}  ")
            append("${s.kcal.roundToInt()} kcal")
            when {
                !s.locationOn -> append("  location off, using steps")
                !s.hasFix -> append("  no GPS fix, using steps")
            }
        }
        runCatching {
            NotificationManagerCompat.from(this)
                .notify(Notifications.ID_RUN, Notifications.runNotification(this, title, text))
        }
    }

    override fun onDestroy() {
        stopLocationUpdates()
        unregisterStepCounter()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RunService"
        private const val FLUSH_EVERY = 30
        private const val AUTO_PAUSE_AFTER_MS = 12_000L

        /** A fix this recent means GPS is carrying the distance. */
        private const val GPS_HEALTHY_MS = 10_000L

        /** Steps within this window count as moving. */
        private const val STEPS_MOVING_MS = 5_000L

        /** How long a silent fused request is tolerated before switching to raw GPS. */
        private const val FUSED_WATCHDOG_MS = 25_000L

        /** Cadence, pace and speed are measured over this rolling window. */
        private const val WINDOW_MS = 30_000L

        private const val RUN_CADENCE_SPM = 140.0
        private const val SPLIT_METRES = 1000.0

        const val ACTION_START = "com.stride.fit.RUN_START"
        const val ACTION_PAUSE = "com.stride.fit.RUN_PAUSE"
        const val ACTION_RESUME = "com.stride.fit.RUN_RESUME"
        const val ACTION_STOP = "com.stride.fit.RUN_STOP"
        const val ACTION_DISCARD = "com.stride.fit.RUN_DISCARD"
        const val EXTRA_ACTIVITY = "activity"

        private val state = MutableStateFlow(RunState())
        val runState: StateFlow<RunState> = state.asStateFlow()

        fun start(context: Context, activity: String) = send(context, ACTION_START) {
            it.putExtra(EXTRA_ACTIVITY, activity)
        }

        fun pause(context: Context) = send(context, ACTION_PAUSE)
        fun resume(context: Context) = send(context, ACTION_RESUME)
        fun finish(context: Context) = send(context, ACTION_STOP)
        fun discard(context: Context) = send(context, ACTION_DISCARD)

        private fun send(context: Context, action: String, block: (Intent) -> Unit = {}) {
            val intent = Intent(context, RunService::class.java).setAction(action).also(block)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}

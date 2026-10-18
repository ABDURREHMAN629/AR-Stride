package com.stride.fit.sensors

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.stride.fit.Brand
import com.stride.fit.data.repo.StrideRepository
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

/**
 * The always-on pedometer, a foreground service so counting survives the screen going
 * off. TYPE_STEP_COUNTER says how many impacts happened but not whether they were
 * footsteps; a batched accelerometer feeds [MotionClassifier] so each batch can be
 * checked. A step reaches the database only once [PedestrianGate] agrees.
 */
class StepService : Service(), SensorEventListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repo: StrideRepository
    private lateinit var sensorManager: SensorManager
    private lateinit var prefs: android.content.SharedPreferences

    private val classifier = MotionClassifier()
    private val gate = PedestrianGate()
    private val fallbackDetector = FallbackStepDetector()
    private lateinit var motionLog: MotionLog

    private var stepCounter: Sensor? = null
    private var stepDetector: Sensor? = null
    private var accelerometer: Sensor? = null
    private var usingFallback = false

    /** Steps waiting for the accelerometer window that covers them to arrive. */
    private data class PendingSteps(val atMs: Long, val count: Int)

    private val pending = ArrayDeque<PendingSteps>()
    private val pendingLock = Any()

    private var lastRawCounter = -1L
    private var lastClassifyMs = 0L
    private var currentDay = Time.today()
    private var notifiedSteps = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repo = StrideRepository.get(this)
        sensorManager = getSystemService(SensorManager::class.java)
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        motionLog = MotionLog(this)
        lastRawCounter = prefs.getLong(KEY_LAST_RAW, -1L)

        Notifications.ensureChannels(this)
        startForeground(
            Notifications.ID_STEPS,
            Notifications.stepsNotification(this, "${Brand.NAME} is counting", "Starting up")
        )

        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        registerSensors()
        requestActivityUpdates()
        observeSensitivity()
        startDrainLoop()
        running.value = true
    }

    private fun registerSensors() {
        // Batch the accelerometer so the SoC can sleep between bursts. Timestamps stay
        // exact, so classification is unaffected by the delay.
        accelerometer?.let {
            sensorManager.registerListener(this, it, ACCEL_PERIOD_US, ACCEL_BATCH_US)
        }

        when {
            stepCounter != null ->
                sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)

            stepDetector != null ->
                sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)

            else -> {
                usingFallback = true
                Log.i(TAG, "No hardware step sensor; using accelerometer peak detection")
            }
        }
        hardwareAvailable.value = stepCounter != null || stepDetector != null
    }

    private fun observeSensitivity() {
        scope.launch {
            repo.profile.collect { classifier.sensitivity = it.sensitivity }
        }
    }

    /** A veto layer only: without it the accelerometer classifier still works. */
    private fun requestActivityUpdates() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            android.Manifest.permission.ACTIVITY_RECOGNITION
        else "com.google.android.gms.permission.ACTIVITY_RECOGNITION"

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Activity recognition permission not granted; classifier runs alone")
            return
        }
        runCatching {
            val intent = Intent(this, ActivityGateReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            ActivityRecognition.getClient(this)
                .requestActivityUpdates(ACTIVITY_INTERVAL_MS, pi)
        }.onFailure { Log.w(TAG, "Activity recognition unavailable: ${it.message}") }
    }

    /** Delayed so the accelerometer batch covering those steps has definitely arrived. */
    private fun startDrainLoop() {
        scope.launch {
            while (true) {
                delay(DRAIN_TICK_MS)
                drainPending(System.currentTimeMillis() - RESOLVE_DELAY_MS)
                rolloverIfNeeded()
                updateNotification()
            }
        }
    }

    private suspend fun drainPending(olderThanMs: Long) {
        val ready = mutableListOf<PendingSteps>()
        synchronized(pendingLock) {
            while (true) {
                val head = pending.peekFirst() ?: break
                if (head.atMs > olderThanMs) break
                ready += pending.removeFirst()
            }
        }
        if (ready.isEmpty()) return

        for (item in ready) {
            val resolution = gate.resolve(item.atMs)
            motionLog.steps(
                item.atMs, item.count, resolution.verdict, resolution.motion, resolution.cadenceSpm
            )
            val credited = gate.account(item.count, resolution.verdict)
            if (credited > 0) {
                repo.recordSteps(credited, item.atMs, resolution.motion, resolution.cadenceSpm)
            } else {
                repo.recordRejected(
                    steps = item.count,
                    riding = resolution.verdict == Verdict.REJECT_RIDING,
                    atMs = item.atMs
                )
            }
        }
        repo.refreshActiveMinutes()
        state.value = gate.snapshot()
        motionLog.flush()
    }

    private suspend fun rolloverIfNeeded() {
        val today = Time.today()
        if (today != currentDay) {
            currentDay = today
            gate.resetDailyCounters()
            repo.reviewStepGoal()
            repo.minuteDao.prune(Time.minuteRangeForDay(today - 90).first)
            notifiedSteps = -1
        }
    }

    private suspend fun updateNotification() {
        val stat = repo.dayStatDao.get(currentDay)
        val steps = stat?.steps ?: 0
        if (steps == notifiedSteps) return
        notifiedSteps = steps
        val profile = repo.profile.first()
        val distance = Fmt.distance(stat?.distanceM ?: 0.0, profile.useMetric)
        val note = Notifications.stepsNotification(
            this,
            "${Fmt.steps(steps)} steps",
            "$distance  ${Fmt.kcal(stat?.kcal ?: 0.0)} kcal  goal ${Fmt.steps(profile.stepGoal)}"
        )
        runCatching {
            NotificationManagerCompat.from(this).notify(Notifications.ID_STEPS, note)
        }
    }


    override fun onSensorChanged(event: SensorEvent) {
        val wallMs = wallClockOf(event.timestamp)
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> onAccelerometer(event, wallMs)
            Sensor.TYPE_STEP_COUNTER -> onStepCounter(event.values[0].toLong(), wallMs)
            Sensor.TYPE_STEP_DETECTOR -> enqueue(wallMs, event.values[0].toInt().coerceAtLeast(1))
        }
    }

    /** Sensor timestamps are nanoseconds since boot; convert to wall-clock. */
    private fun wallClockOf(sensorTimestampNs: Long): Long {
        val deltaNs = SystemClock.elapsedRealtimeNanos() - sensorTimestampNs
        return System.currentTimeMillis() - deltaNs / 1_000_000L
    }

    private fun onAccelerometer(event: SensorEvent, wallMs: Long) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        classifier.addSample(x, y, z, wallMs)

        if (usingFallback && fallbackDetector.onSample(x, y, z, wallMs) > 0) {
            enqueue(wallMs, 1)
        }

        // One verdict per second of sensor time, whether the batch arrived late or not.
        if (wallMs - lastClassifyMs >= CLASSIFY_INTERVAL_MS) {
            lastClassifyMs = wallMs
            val (motion, features) = classifier.classify()
            gate.onMotion(motion, features, wallMs)
            val snapshot = gate.snapshot()
            state.value = snapshot
            motionLog.window(wallMs, motion, features, snapshot.open, snapshot.recognised)
        }
    }

    private fun onStepCounter(raw: Long, wallMs: Long) {
        if (lastRawCounter < 0) {
            // Adopt as baseline, rather than crediting every step since the last boot.
            lastRawCounter = raw
            prefs.edit().putLong(KEY_LAST_RAW, raw).apply()
            return
        }
        if (raw < lastRawCounter) {
            // The hardware counter resets on reboot.
            lastRawCounter = raw
            prefs.edit().putLong(KEY_LAST_RAW, raw).apply()
            return
        }
        val delta = (raw - lastRawCounter).toInt()
        lastRawCounter = raw
        prefs.edit().putLong(KEY_LAST_RAW, raw).apply()
        if (delta <= 0) return
        // A huge jump means a stale baseline: crediting it would invent unverified steps.
        if (delta > MAX_PLAUSIBLE_BATCH) return
        enqueue(wallMs, delta)
    }

    private fun enqueue(atMs: Long, count: Int) {
        synchronized(pendingLock) { pending.addLast(PendingSteps(atMs, count)) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        running.value = false
        sensorManager.unregisterListener(this)
        // The sink is a static field holding a lambda that closes over this service.
        // Clearing it stops a destroyed instance being kept alive by the companion.
        recognitionSink = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "StepService"
        private const val PREFS = "stride_step_service"
        private const val KEY_LAST_RAW = "last_raw_counter"

        private const val ACCEL_PERIOD_US = 20_000       // 50 Hz
        private const val ACCEL_BATCH_US = 8_000_000     // 8 s of batching
        private const val CLASSIFY_INTERVAL_MS = 1_000L
        private const val DRAIN_TICK_MS = 5_000L
        private const val RESOLVE_DELAY_MS = 12_000L     // > batch latency
        private const val ACTIVITY_INTERVAL_MS = 20_000L
        private const val MAX_PLAUSIBLE_BATCH = 3_000

        /** Live gate state, so the app can show exactly what the classifier is doing. */
        private val state = MutableStateFlow(GateState())
        val gateState: StateFlow<GateState> = state.asStateFlow()

        private val running = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = running.asStateFlow()

        private val hardwareAvailable = MutableStateFlow(true)
        val hasHardwarePedometer: StateFlow<Boolean> = hardwareAvailable.asStateFlow()

        /** Called by the receiver when Play services reports a new activity. */
        fun onRecognisedActivity(activity: RecognisedActivity, confidence: Int) {
            recognitionSink?.invoke(activity, confidence)
        }

        private var recognitionSink: ((RecognisedActivity, Int) -> Unit)? = null

        fun start(context: Context) {
            val intent = Intent(context, StepService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StepService::class.java))
        }
    }

    init {
        recognitionSink = { activity, confidence ->
            gate.onRecognisedActivity(activity, confidence, System.currentTimeMillis())
            state.value = gate.snapshot()
        }
    }
}

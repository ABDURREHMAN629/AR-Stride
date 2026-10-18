package com.stride.fit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stride.fit.data.db.BodyLog
import com.stride.fit.data.db.DayStat
import com.stride.fit.data.db.RunPoint
import com.stride.fit.data.db.RunSession
import com.stride.fit.data.db.RunSplit
import com.stride.fit.data.db.SetLog
import com.stride.fit.data.db.WorkoutSession
import com.stride.fit.data.repo.Profile
import com.stride.fit.data.repo.StrideRepository
import com.stride.fit.data.repo.StepComparison
import com.stride.fit.data.repo.TodaySummary
import com.stride.fit.health.HealthMath
import com.stride.fit.sensors.GateState
import com.stride.fit.sensors.Sensitivity
import com.stride.fit.sensors.StepService
import com.stride.fit.training.Block
import com.stride.fit.training.Challenge30
import com.stride.fit.training.PlanDay
import com.stride.fit.training.PostureRoutine
import com.stride.fit.training.PostureRoutines
import com.stride.fit.training.AllRoutines
import com.stride.fit.training.ProgramDay
import com.stride.fit.training.Programs
import com.stride.fit.training.YogaFlows
import com.stride.fit.training.Progression
import com.stride.fit.util.Time
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate


abstract class RepoViewModel(app: Application) : AndroidViewModel(app) {
    protected val repo: StrideRepository = StrideRepository.get(app)

    val profile: StateFlow<Profile> = repo.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Profile())
}


data class TodayUi(
    val summary: TodaySummary? = null,
    val hourlySteps: List<Int> = List(24) { 0 },
    val week: List<DayStat> = emptyList(),
    val comparison: StepComparison = StepComparison(),
    val postureStreak: Int = 0,
    val strengthSessionsThisWeek: Int = 0
)

class TodayViewModel(app: Application) : RepoViewModel(app) {

    val gateState: StateFlow<GateState> = StepService.gateState
    val serviceRunning: StateFlow<Boolean> = StepService.isRunning
    val hasPedometer: StateFlow<Boolean> = StepService.hasHardwarePedometer

    private val _ui = MutableStateFlow(TodayUi())
    val ui: StateFlow<TodayUi> = _ui.asStateFlow()

    val todaySlot: Programs.WeekSlot
        get() = Programs.plannedFor(
            LocalDate.now(Time.zone()).dayOfWeek,
            Programs[profile.value.programId] ?: Programs.fullBodyFoundation
        )

    init {
        viewModelScope.launch {
            repo.observeTodaySummary().collect { s ->
                _ui.value = _ui.value.copy(summary = s)
            }
        }
        viewModelScope.launch {
            repo.observeHourlySteps(Time.today()).collect { h ->
                _ui.value = _ui.value.copy(hourlySteps = h)
            }
        }
        viewModelScope.launch {
            repo.observeWeek().collect { w ->
                _ui.value = _ui.value.copy(week = w)
            }
        }
        viewModelScope.launch {
            repo.observeStepComparison().collect { c ->
                _ui.value = _ui.value.copy(comparison = c)
            }
        }
        viewModelScope.launch {
            val today = Time.today()
            repo.workoutDao.observeCount(today - 6, today).collect { c ->
                _ui.value = _ui.value.copy(strengthSessionsThisWeek = c)
            }
        }
        refreshStreak()
    }

    fun refreshStreak() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(postureStreak = repo.postureStreak())
        }
    }
}


data class RunDetail(
    val session: RunSession,
    val points: List<RunPoint>,
    val splits: List<RunSplit>
)

class RunViewModel(app: Application) : RepoViewModel(app) {

    val liveState = com.stride.fit.run.RunService.runState

    val history: StateFlow<List<RunSession>> = repo.runDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _detail = MutableStateFlow<RunDetail?>(null)
    val detail: StateFlow<RunDetail?> = _detail.asStateFlow()

    private val _personalBests = MutableStateFlow<Map<String, String>>(emptyMap())
    val personalBests: StateFlow<Map<String, String>> = _personalBests.asStateFlow()

    init {
        refreshBests()
    }

    fun refreshBests() {
        viewModelScope.launch {
            val metric = profile.first().useMetric
            val longest = repo.runDao.longestRunM()
            val bestPace = repo.runDao.bestPaceSecPerKm()
            _personalBests.value = buildMap {
                if (longest > 0) put("Longest run", com.stride.fit.util.Fmt.distance(longest, metric))
                if (bestPace > 0) put(
                    "Best pace",
                    com.stride.fit.util.Fmt.pace(bestPace, metric) + " " +
                        com.stride.fit.util.Fmt.paceUnit(metric)
                )
            }
        }
    }

    fun loadDetail(runId: Long) {
        viewModelScope.launch {
            val session = repo.runDao.session(runId) ?: return@launch
            _detail.value = RunDetail(
                session = session,
                points = repo.runDao.points(runId),
                splits = repo.runDao.splits(runId)
            )
        }
    }

    fun clearDetail() {
        _detail.value = null
    }

    fun delete(runId: Long) {
        viewModelScope.launch {
            repo.runDao.deletePoints(runId)
            repo.runDao.deleteSplits(runId)
            repo.runDao.deleteSession(runId)
            _detail.value = null
            refreshBests()
        }
    }
}


data class WorkoutUi(
    val session: WorkoutSession? = null,
    val day: ProgramDay? = null,
    val sets: List<SetLog> = emptyList(),
    val suggestions: Map<String, Progression.Suggestion> = emptyMap(),
    val restEndsAtMs: Long = 0L,
    val restForExercise: String = ""
)

class TrainViewModel(app: Application) : RepoViewModel(app) {

    private val _workout = MutableStateFlow(WorkoutUi())
    val workout: StateFlow<WorkoutUi> = _workout.asStateFlow()

    val history: StateFlow<List<WorkoutSession>> = repo.workoutDao.observeRecent(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val program get() = Programs[profile.value.programId] ?: Programs.fullBodyFoundation

    /** Which plan days have a finished session against them. */
    val completedPlanDays: StateFlow<Set<String>> = repo.workoutDao.observeCompletedPlanDays()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        // Recover a session the user walked away from mid-workout.
        viewModelScope.launch {
            repo.workoutDao.unfinished()?.let { open ->
                val day = resolveDay(open.programDayId)
                _workout.value = WorkoutUi(session = open, day = day)
                observeSets(open.id)
                loadSuggestions(day)
            }
        }
    }

    /** A session id may belong to a standing programme or to the 30-day plan. */
    private fun resolveDay(id: String): ProgramDay? =
        Programs.dayById(id) ?: Challenge30.byId(id)?.let { Challenge30.asProgramDay(it) }

    /** Day 1 is whenever you press start, so missing a day shifts the plan rather than breaking it. */
    fun startPlan() {
        viewModelScope.launch {
            repo.settings.update { it.copy(planStartDay = Time.today()) }
        }
    }

    fun resetPlan() {
        viewModelScope.launch {
            repo.settings.update { it.copy(planStartDay = 0L) }
        }
    }

    fun startPlanDay(planDay: PlanDay) {
        startWorkout(Challenge30.asProgramDay(planDay))
    }

    fun selectProgram(id: String) {
        viewModelScope.launch { repo.settings.update { it.copy(programId = id) } }
    }

    fun startWorkout(day: ProgramDay) {
        viewModelScope.launch {
            val existing = repo.workoutDao.unfinished()
            if (existing != null && existing.programDayId == day.id) {
                _workout.value = WorkoutUi(session = existing, day = day)
                observeSets(existing.id)
                loadSuggestions(day)
                return@launch
            }
            existing?.let { repo.workoutDao.updateSession(it.copy(finished = true, endMs = System.currentTimeMillis())) }

            val id = repo.workoutDao.insertSession(
                WorkoutSession(
                    epochDay = Time.today(),
                    programDayId = day.id,
                    name = day.name,
                    startMs = System.currentTimeMillis()
                )
            )
            val session = repo.workoutDao.session(id)
            _workout.value = WorkoutUi(session = session, day = day)
            observeSets(id)
            loadSuggestions(day)
        }
    }

    private fun observeSets(sessionId: Long) {
        viewModelScope.launch {
            repo.workoutDao.observeSets(sessionId).collect { sets ->
                _workout.value = _workout.value.copy(sets = sets)
            }
        }
    }

    private fun loadSuggestions(day: ProgramDay?) {
        if (day == null) return
        viewModelScope.launch {
            val map = mutableMapOf<String, Progression.Suggestion>()
            day.main.forEach { block ->
                val history = repo.workoutDao.recentSets(block.exerciseId, 24)
                map[block.exerciseId] = Progression.suggest(block, history)
            }
            _workout.value = _workout.value.copy(suggestions = map)
        }
    }

    fun logSet(block: Block, setIndex: Int, reps: Int, weightKg: Double, rpe: Int) {
        val session = _workout.value.session ?: return
        viewModelScope.launch {
            val existing = _workout.value.sets.firstOrNull {
                it.exerciseId == block.exerciseId && it.setIndex == setIndex
            }
            repo.workoutDao.upsertSet(
                SetLog(
                    id = existing?.id ?: 0,
                    sessionId = session.id,
                    exerciseId = block.exerciseId,
                    setIndex = setIndex,
                    targetReps = block.targetReps,
                    reps = reps,
                    weightKg = weightKg,
                    rpe = rpe,
                    done = true,
                    ts = System.currentTimeMillis()
                )
            )
            startRest(block)
        }
    }

    fun undoSet(setId: Long) {
        viewModelScope.launch { repo.workoutDao.deleteSet(setId) }
    }

    private fun startRest(block: Block) {
        _workout.value = _workout.value.copy(
            restEndsAtMs = System.currentTimeMillis() + block.restSec * 1000L,
            restForExercise = block.exerciseId
        )
    }

    fun skipRest() {
        _workout.value = _workout.value.copy(restEndsAtMs = 0L, restForExercise = "")
    }

    fun addRest(seconds: Int) {
        val current = _workout.value.restEndsAtMs
        if (current <= 0) return
        _workout.value = _workout.value.copy(restEndsAtMs = current + seconds * 1000L)
    }

    fun finishWorkout() {
        val ui = _workout.value
        val session = ui.session ?: return
        viewModelScope.launch {
            val sets = repo.workoutDao.sets(session.id).filter { it.done }
            val endMs = System.currentTimeMillis()
            val minutes = ((endMs - session.startMs) / 60_000.0).coerceIn(1.0, 180.0)
            val weight = profile.first().weightKg
            val volume = sets.sumOf { it.weightKg * it.reps }
            val kcal = HealthMath.kcalFromStrength(minutes, weight, vigorous = sets.size >= 12)

            repo.workoutDao.updateSession(
                session.copy(
                    endMs = endMs,
                    finished = true,
                    volumeKg = volume,
                    totalSets = sets.size,
                    totalReps = sets.sumOf { it.reps },
                    kcal = kcal
                )
            )
            repo.addWorkoutEnergy(Time.today(), strength = kcal)
            _workout.value = WorkoutUi()
        }
    }

    fun discardWorkout() {
        val session = _workout.value.session ?: return
        viewModelScope.launch {
            repo.workoutDao.updateSession(
                session.copy(finished = true, endMs = System.currentTimeMillis())
            )
            _workout.value = WorkoutUi()
        }
    }
}


data class RoutinePlayerUi(
    val routine: PostureRoutine? = null,
    val stepIndex: Int = 0,
    val secondsLeft: Int = 0,
    val onSecondSide: Boolean = false,
    val running: Boolean = false,
    val elapsedSeconds: Int = 0,
    val finished: Boolean = false
)

class PostureViewModel(app: Application) : RepoViewModel(app) {

    private val _player = MutableStateFlow(RoutinePlayerUi())
    val player: StateFlow<RoutinePlayerUi> = _player.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    val todayLogs = repo.routineDao.observeDay(Time.today())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recent = repo.routineDao.observeRecent(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshStreak()
    }

    fun refreshStreak() {
        viewModelScope.launch { _streak.value = repo.postureStreak() }
    }

    fun start(routine: PostureRoutine) {
        val first = routine.steps.firstOrNull() ?: return
        _player.value = RoutinePlayerUi(
            routine = routine,
            stepIndex = 0,
            secondsLeft = first.seconds,
            onSecondSide = false,
            running = true
        )
    }

    /** Called once a second by the player screen. */
    fun tick() {
        val p = _player.value
        if (!p.running || p.routine == null) return
        if (p.secondsLeft > 1) {
            _player.value = p.copy(
                secondsLeft = p.secondsLeft - 1,
                elapsedSeconds = p.elapsedSeconds + 1
            )
            return
        }
        advance(countLastSecond = true)
    }

    fun advance(countLastSecond: Boolean = false) {
        val p = _player.value
        val routine = p.routine ?: return
        val step = routine.steps.getOrNull(p.stepIndex) ?: return
        val elapsed = p.elapsedSeconds + if (countLastSecond) 1 else 0

        // A two-sided drill runs twice before moving on.
        if (step.perSide && !p.onSecondSide) {
            _player.value = p.copy(
                onSecondSide = true,
                secondsLeft = step.seconds,
                elapsedSeconds = elapsed
            )
            return
        }

        val nextIndex = p.stepIndex + 1
        if (nextIndex >= routine.steps.size) {
            complete(elapsed)
            return
        }
        val next = routine.steps[nextIndex]
        _player.value = p.copy(
            stepIndex = nextIndex,
            onSecondSide = false,
            secondsLeft = next.seconds,
            elapsedSeconds = elapsed
        )
    }

    fun back() {
        val p = _player.value
        val routine = p.routine ?: return
        if (p.onSecondSide) {
            val step = routine.steps[p.stepIndex]
            _player.value = p.copy(onSecondSide = false, secondsLeft = step.seconds)
            return
        }
        val prevIndex = (p.stepIndex - 1).coerceAtLeast(0)
        _player.value = p.copy(
            stepIndex = prevIndex,
            onSecondSide = false,
            secondsLeft = routine.steps[prevIndex].seconds
        )
    }

    fun togglePause() {
        _player.value = _player.value.copy(running = !_player.value.running)
    }

    private fun complete(elapsedSeconds: Int) {
        val routine = _player.value.routine ?: return
        viewModelScope.launch {
            repo.logRoutine(routine.id, routine.name, elapsedSeconds)
            refreshStreak()
        }
        _player.value = _player.value.copy(running = false, finished = true)
    }

    /** Log whatever was completed and leave, rather than losing the effort entirely. */
    fun stopEarly() {
        val p = _player.value
        val routine = p.routine
        if (routine != null && p.elapsedSeconds >= 30) {
            viewModelScope.launch {
                repo.logRoutine(routine.id, routine.name, p.elapsedSeconds)
                refreshStreak()
            }
        }
        _player.value = RoutinePlayerUi()
    }

    fun dismiss() {
        _player.value = RoutinePlayerUi()
    }

    val postureRoutines: List<PostureRoutine> get() = PostureRoutines.all
    val yogaRoutines: List<PostureRoutine> get() = YogaFlows.all
    val routines: List<PostureRoutine> get() = AllRoutines.all

    /** Start any routine by id, so other screens can hand off to the player. */
    fun startById(id: String) {
        AllRoutines[id]?.let { start(it) }
    }
}


class ProfileViewModel(app: Application) : RepoViewModel(app) {

    val weightHistory: StateFlow<List<BodyLog>> = repo.observeWeightHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWeight: StateFlow<BodyLog?> = repo.bodyDao.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val gateState: StateFlow<GateState> = StepService.gateState
    val serviceRunning: StateFlow<Boolean> = StepService.isRunning
    val hasPedometer: StateFlow<Boolean> = StepService.hasHardwarePedometer

    val week: StateFlow<List<DayStat>> = repo.observeWeek()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateProfile(block: (Profile) -> Profile) {
        viewModelScope.launch { repo.settings.update(block) }
    }

    fun logWeight(kg: Double) {
        viewModelScope.launch { repo.logWeight(kg) }
    }

    fun setSensitivity(s: Sensitivity) {
        viewModelScope.launch { repo.settings.update { it.copy(sensitivity = s) } }
    }
}

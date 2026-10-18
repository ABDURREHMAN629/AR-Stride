package com.stride.fit.ui

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.stride.fit.training.BreathPattern
import com.stride.fit.training.BreathingPatterns
import com.stride.fit.training.Grounding
import com.stride.fit.training.PresenceExercises
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.stride.fit.util.Time

data class BreathUi(
    val pattern: BreathPattern? = null,
    val rounds: Int = 8,
    val currentRound: Int = 1,
    val phaseIndex: Int = 0,
    val secondsLeft: Int = 0,
    val running: Boolean = false,
    val finished: Boolean = false,
    val elapsedSeconds: Int = 0
) {
    val phase get() = pattern?.phases?.getOrNull(phaseIndex)

    /** 0..1 through the current phase, for the pacer ring. */
    val phaseProgress: Float
        get() {
            val total = phase?.seconds ?: return 0f
            if (total <= 0) return 0f
            return 1f - secondsLeft.toFloat() / total
        }
}

data class GroundingUi(
    val active: Boolean = false,
    val stepIndex: Int = 0,
    val secondsLeft: Int = 0,
    val ticked: Int = 0,
    val running: Boolean = false,
    val finished: Boolean = false,
    val elapsedSeconds: Int = 0
) {
    val step get() = Grounding.steps.getOrNull(stepIndex)
}

/**
 * Breathing and attention work. Both players are driven by a one-second tick from the
 * screen rather than a timer inside the view model, so pausing is simply not calling
 * tick - nothing is left running when the user walks away.
 */
class MindViewModel(app: Application) : RepoViewModel(app) {

    private val _breath = MutableStateFlow(BreathUi())
    val breath: StateFlow<BreathUi> = _breath.asStateFlow()

    private val _grounding = MutableStateFlow(GroundingUi())
    val grounding: StateFlow<GroundingUi> = _grounding.asStateFlow()

    val todayLogs = repo.routineDao.observeDay(Time.today())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val patterns = BreathingPatterns.all
    val presence = PresenceExercises.all


    fun startBreathing(pattern: BreathPattern, rounds: Int = pattern.defaultRounds) {
        _breath.value = BreathUi(
            pattern = pattern,
            rounds = rounds,
            currentRound = 1,
            phaseIndex = 0,
            secondsLeft = pattern.phases.first().seconds,
            running = true
        )
    }

    /** Call once a second while the pacer is on screen. */
    fun tickBreath() {
        val b = _breath.value
        val pattern = b.pattern ?: return
        if (!b.running || b.finished) return

        if (b.secondsLeft > 1) {
            _breath.value = b.copy(
                secondsLeft = b.secondsLeft - 1,
                elapsedSeconds = b.elapsedSeconds + 1
            )
            return
        }

        val elapsed = b.elapsedSeconds + 1
        val nextPhase = b.phaseIndex + 1

        if (nextPhase < pattern.phases.size) {
            _breath.value = b.copy(
                phaseIndex = nextPhase,
                secondsLeft = pattern.phases[nextPhase].seconds,
                elapsedSeconds = elapsed
            )
            return
        }

        // Round complete.
        if (b.currentRound >= b.rounds) {
            finishBreathing(elapsed)
            return
        }
        _breath.value = b.copy(
            currentRound = b.currentRound + 1,
            phaseIndex = 0,
            secondsLeft = pattern.phases.first().seconds,
            elapsedSeconds = elapsed
        )
    }

    private fun finishBreathing(elapsedSeconds: Int) {
        val pattern = _breath.value.pattern ?: return
        logSession("breath_${pattern.id}", pattern.name, elapsedSeconds)
        _breath.value = _breath.value.copy(
            running = false,
            finished = true,
            elapsedSeconds = elapsedSeconds
        )
    }

    fun toggleBreathPause() {
        _breath.value = _breath.value.copy(running = !_breath.value.running)
    }

    fun setRounds(rounds: Int) {
        _breath.value = _breath.value.copy(rounds = rounds.coerceIn(1, 40))
    }

    /** Keep whatever was completed rather than discarding the effort entirely. */
    fun stopBreathing() {
        val b = _breath.value
        val pattern = b.pattern
        if (pattern != null && !b.finished && b.elapsedSeconds >= 30) {
            logSession("breath_${pattern.id}", pattern.name, b.elapsedSeconds)
        }
        _breath.value = BreathUi()
    }

    fun dismissBreathing() {
        _breath.value = BreathUi()
    }


    fun startGrounding() {
        _grounding.value = GroundingUi(
            active = true,
            stepIndex = 0,
            secondsLeft = Grounding.steps.first().seconds,
            running = true
        )
    }

    fun tickGrounding() {
        val g = _grounding.value
        if (!g.active || !g.running || g.finished) return

        if (g.secondsLeft > 1) {
            _grounding.value = g.copy(
                secondsLeft = g.secondsLeft - 1,
                elapsedSeconds = g.elapsedSeconds + 1
            )
            return
        }
        advanceGrounding(countLastSecond = true)
    }

    fun advanceGrounding(countLastSecond: Boolean = false) {
        val g = _grounding.value
        if (!g.active) return
        val elapsed = g.elapsedSeconds + if (countLastSecond) 1 else 0
        val next = g.stepIndex + 1

        if (next >= Grounding.steps.size) {
            logSession(Grounding.ID, Grounding.NAME, elapsed)
            _grounding.value = g.copy(running = false, finished = true, elapsedSeconds = elapsed)
            return
        }
        _grounding.value = g.copy(
            stepIndex = next,
            secondsLeft = Grounding.steps[next].seconds,
            ticked = 0,
            elapsedSeconds = elapsed
        )
    }

    fun backGrounding() {
        val g = _grounding.value
        val previous = (g.stepIndex - 1).coerceAtLeast(0)
        _grounding.value = g.copy(
            stepIndex = previous,
            secondsLeft = Grounding.steps[previous].seconds,
            ticked = 0
        )
    }

    /** Tap as you find each thing, so the count is yours rather than the app's. */
    fun tickGroundingItem() {
        val g = _grounding.value
        val max = g.step?.count ?: return
        _grounding.value = g.copy(ticked = (g.ticked + 1).coerceAtMost(max))
    }

    fun toggleGroundingPause() {
        _grounding.value = _grounding.value.copy(running = !_grounding.value.running)
    }

    fun stopGrounding() {
        val g = _grounding.value
        if (g.active && !g.finished && g.elapsedSeconds >= 30) {
            logSession(Grounding.ID, Grounding.NAME, g.elapsedSeconds)
        }
        _grounding.value = GroundingUi()
    }

    fun dismissGrounding() {
        _grounding.value = GroundingUi()
    }


    /** Mind work is logged alongside posture work, so it feeds the same daily streak. */
    private fun logSession(id: String, name: String, seconds: Int) {
        viewModelScope.launch { repo.logRoutine(id, name, seconds) }
    }

    fun logPresence(id: String, name: String, minutes: Int) {
        logSession(id, name, minutes * 60)
    }
}

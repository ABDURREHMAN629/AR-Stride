package com.stride.fit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.fit.data.db.SetLog
import com.stride.fit.data.db.WorkoutSession
import com.stride.fit.training.Block
import com.stride.fit.training.Exercise
import com.stride.fit.training.Exercises
import com.stride.fit.training.Challenge30
import com.stride.fit.training.PlanDay
import com.stride.fit.training.PlanKind
import com.stride.fit.training.ProgramDay
import com.stride.fit.training.Programs
import com.stride.fit.training.Progression
import com.stride.fit.ui.TrainViewModel
import com.stride.fit.ui.components.Pill
import com.stride.fit.ui.components.SectionTitle
import com.stride.fit.ui.components.StatRow
import com.stride.fit.ui.components.StrideCard
import com.stride.fit.ui.components.ThinProgress
import com.stride.fit.ui.theme.Space
import com.stride.fit.ui.theme.StrideColors
import com.stride.fit.util.Fmt
import com.stride.fit.util.Time
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class TrainTab(val label: String) {
    PROGRAM("Programme"),
    PLAN("30-Day Plan")
}

@Composable
fun TrainScreen(
    vm: TrainViewModel,
    onOpenRoutine: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val workout by vm.workout.collectAsStateWithLifecycle()
    var detailExercise by remember { mutableStateOf<Exercise?>(null) }
    var tab by remember { mutableStateOf(TrainTab.PLAN) }

    detailExercise?.let {
        ExerciseDetailDialog(it) { detailExercise = null }
    }

    if (workout.session != null && workout.day != null) {
        WorkoutPlayer(
            vm = vm,
            contentPadding = contentPadding,
            onOpenExercise = { detailExercise = it }
        )
        return
    }

    when (tab) {
        TrainTab.PROGRAM -> ProgramOverview(
            vm = vm,
            tab = tab,
            onTabChange = { tab = it },
            contentPadding = contentPadding,
            onOpenExercise = { detailExercise = it }
        )
        TrainTab.PLAN -> PlanOverview(
            vm = vm,
            tab = tab,
            onTabChange = { tab = it },
            contentPadding = contentPadding,
            onOpenExercise = { detailExercise = it },
            onOpenRoutine = onOpenRoutine
        )
    }
}

@Composable
private fun TabSwitch(tab: TrainTab, onTabChange: (TrainTab) -> Unit) {
    Row(
        Modifier.padding(horizontal = Space.lg),
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        TrainTab.entries.forEach { option ->
            FilterChip(
                selected = tab == option,
                onClick = { onTabChange(option) },
                label = { Text(option.label) }
            )
        }
    }
}

/**
 * The 30-day plan.
 *
 * Day 1 is whenever you press start, not a fixed calendar. Miss a day and the plan
 * shifts rather than shaming you with a broken streak - the sequence matters, the
 * dates do not.
 */
@Composable
private fun PlanOverview(
    vm: TrainViewModel,
    tab: TrainTab,
    onTabChange: (TrainTab) -> Unit,
    contentPadding: PaddingValues,
    onOpenExercise: (Exercise) -> Unit,
    onOpenRoutine: (String) -> Unit
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val completed by vm.completedPlanDays.collectAsStateWithLifecycle()

    val started = profile.planStartDay > 0
    val currentDay = if (!started) 0
    else ((Time.today() - profile.planStartDay) + 1).toInt().coerceIn(1, 30)

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        item {
            Column(Modifier.padding(horizontal = Space.lg, vertical = Space.sm)) {
                Text("Train", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "30 days at home, no equipment needed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { TabSwitch(tab, onTabChange) }

        if (!started) {
            item {
                StrideCard(Modifier.padding(horizontal = Space.lg)) {
                    Text("The 30-Day Plan", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        "Twelve strength sessions, nine yoga flows, five posture days and four " +
                            "rest days, in an order that lets each one work.\n\n" +
                            "Everything is bodyweight or a resistance band, so it all happens at " +
                            "home. Nothing jumps, because at your weight the knees deserve better.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Space.md))
                    StatRow("Week 1", "Learn the patterns, two rounds")
                    StatRow("Week 2", "Same work, three rounds")
                    StatRow("Week 3", "Harder variations")
                    StatRow("Week 4", "Four rounds, then retest")
                    Spacer(Modifier.height(Space.md))
                    Text(
                        "Day 1 sets four benchmarks. Day 29 repeats them exactly, so you finish " +
                            "with a measured result rather than a feeling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StrideColors.Steps
                    )
                    Spacer(Modifier.height(Space.md))
                    Button(
                        onClick = { vm.startPlan() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Strength)
                    ) { Text("Start day 1 today") }
                }
            }
        } else {
            item {
                val today = Challenge30[currentDay]
                if (today != null) {
                    PlanDayCard(
                        planDay = today,
                        done = completed.contains(today.id),
                        highlighted = true,
                        onStart = { vm.startPlanDay(today) },
                        onOpenRoutine = onOpenRoutine,
                        onOpenExercise = onOpenExercise,
                        modifier = Modifier.padding(horizontal = Space.lg)
                    )
                }
            }

            item {
                StrideCard(Modifier.padding(horizontal = Space.lg)) {
                    val doneCount = completed.size
                    StatRow("Progress", "Day $currentDay of 30")
                    StatRow("Strength sessions logged", "$doneCount")
                    Spacer(Modifier.height(Space.sm))
                    ThinProgress(currentDay / 30f, color = StrideColors.Strength)
                    Spacer(Modifier.height(Space.md))
                    TextButton(onClick = { vm.resetPlan() }) {
                        Text("Restart the plan", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item { SectionTitle("All 30 days") }
        items(Challenge30.days, key = { it.id }) { planDay ->
            PlanDayCard(
                planDay = planDay,
                done = completed.contains(planDay.id),
                highlighted = started && planDay.day == currentDay,
                onStart = { vm.startPlanDay(planDay) },
                onOpenRoutine = onOpenRoutine,
                onOpenExercise = onOpenExercise,
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        item { Spacer(Modifier.height(Space.xl)) }
    }
}

@Composable
private fun PlanDayCard(
    planDay: PlanDay,
    done: Boolean,
    highlighted: Boolean,
    onStart: () -> Unit,
    onOpenRoutine: (String) -> Unit,
    onOpenExercise: (Exercise) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val accent = when (planDay.kind) {
        PlanKind.STRENGTH -> StrideColors.Strength
        PlanKind.YOGA -> StrideColors.Posture
        PlanKind.MOBILITY -> StrideColors.Posture
        PlanKind.TEST -> StrideColors.Run
        PlanKind.CARDIO -> StrideColors.Steps
        PlanKind.REST -> StrideColors.Rejected
    }

    StrideCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Day ${planDay.day}",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                    if (highlighted) {
                        Spacer(Modifier.width(Space.sm))
                        Pill("Today", accent)
                    }
                    if (done) {
                        Spacer(Modifier.width(Space.sm))
                        Pill("Done", StrideColors.Steps)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(planDay.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    planDay.focus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Pill(planDay.kind.label, accent)
        }

        if (planDay.coach.isNotEmpty()) {
            Spacer(Modifier.height(Space.sm))
            Text(
                planDay.coach,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (planDay.walkMinutes > 0) {
            Spacer(Modifier.height(Space.xs))
            Text(
                "Plus a ${planDay.walkMinutes} minute walk",
                style = MaterialTheme.typography.bodySmall,
                color = StrideColors.Steps
            )
        }

        if (expanded && planDay.blocks.isNotEmpty()) {
            Spacer(Modifier.height(Space.md))
            planDay.blocks.forEach { block ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenExercise(block.exercise) }
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        block.exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${block.sets} x ${block.targetReps}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (planDay.blocks.isNotEmpty()) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) { Text(if (done) "Repeat" else "Start") }
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) { Text(if (expanded) "Less" else "Exercises") }
            } else if (planDay.routineId.isNotEmpty()) {
                Button(
                    onClick = { onOpenRoutine(planDay.routineId) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) { Text("Open routine") }
            }
        }
    }
}


@Composable
private fun ProgramOverview(
    vm: TrainViewModel,
    tab: TrainTab,
    onTabChange: (TrainTab) -> Unit,
    contentPadding: PaddingValues,
    onOpenExercise: (Exercise) -> Unit
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val program = Programs[profile.programId] ?: Programs.fullBodyFoundation

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        item {
            Column(Modifier.padding(horizontal = Space.lg, vertical = Space.sm)) {
                Text("Strength", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${program.daysPerWeek} full body sessions a week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { TabSwitch(tab, onTabChange) }

        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Text(program.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Space.xs))
                Text(
                    program.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    "Needs: ${program.equipmentNeeded}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Programs.all.forEach { p ->
                        FilterChip(
                            selected = p.id == profile.programId,
                            onClick = { vm.selectProgram(p.id) },
                            label = { Text(p.name) }
                        )
                    }
                }
            }
        }

        item { SectionTitle("Sessions") }
        items(program.days, key = { it.id }) { day ->
            ProgramDayCard(
                day = day,
                onStart = { vm.startWorkout(day) },
                onOpenExercise = onOpenExercise,
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        item { SectionTitle("Recent sessions") }
        if (history.isEmpty()) {
            item {
                StrideCard(Modifier.padding(horizontal = Space.lg)) {
                    Text(
                        "Nothing logged yet. Start a session and every set you record feeds the " +
                            "progression suggestions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(history, key = { it.id }) { session ->
                WorkoutHistoryRow(session, Modifier.padding(horizontal = Space.lg))
            }
        }

        item { Spacer(Modifier.height(Space.xl)) }
    }
}

@Composable
private fun ProgramDayCard(
    day: ProgramDay,
    onStart: () -> Unit,
    onOpenExercise: (Exercise) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    StrideCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(day.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    day.focus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Pill("${day.estimatedMinutes} min", StrideColors.Strength)
        }

        Spacer(Modifier.height(Space.md))
        day.main.forEach { block ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenExercise(block.exercise) }
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    block.exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${block.sets} x ${block.targetReps}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (expanded) {
            Spacer(Modifier.height(Space.sm))
            Text(
                "Warm-up",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            day.warmup.forEach {
                Text(
                    "${it.exercise.name} - ${it.sets} x ${it.targetReps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Space.sm))
            Text(
                "Finisher",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            day.finisher.forEach {
                Text(
                    "${it.exercise.name} - ${it.sets} x ${it.targetReps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Button(
                onClick = onStart,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Strength)
            ) { Text("Start") }
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) { Text(if (expanded) "Less" else "Full session") }
        }
    }
}

@Composable
private fun WorkoutHistoryRow(session: WorkoutSession, modifier: Modifier = Modifier) {
    StrideCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(session.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    Time.dayLabel(session.epochDay),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${session.totalSets} sets",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (session.volumeKg > 0)
                        "${session.volumeKg.roundToInt()} kg volume"
                    else "${session.totalReps} reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun WorkoutPlayer(
    vm: TrainViewModel,
    contentPadding: PaddingValues,
    onOpenExercise: (Exercise) -> Unit
) {
    val workout by vm.workout.collectAsStateWithLifecycle()
    val day = workout.day ?: return
    val session = workout.session ?: return

    val context = androidx.compose.ui.platform.LocalContext.current
    var logging by remember { mutableStateOf<Pair<Block, Int>?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }

    // Buzz once when the rest period runs out, so you can put the phone down between sets.
    LaunchedEffect(workout.restEndsAtMs) {
        val target = workout.restEndsAtMs
        if (target <= 0) return@LaunchedEffect
        val wait = target - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        if (vm.workout.value.restEndsAtMs == target) buzz(context)
    }

    logging?.let { (block, setIndex) ->
        LogSetDialog(
            block = block,
            setIndex = setIndex,
            suggestion = workout.suggestions[block.exerciseId],
            previous = workout.sets.firstOrNull {
                it.exerciseId == block.exerciseId && it.setIndex == setIndex
            },
            onConfirm = { reps, weight, rpe ->
                vm.logSet(block, setIndex, reps, weight, rpe)
                logging = null
            },
            onDismiss = { logging = null }
        )
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish session?") },
            text = { Text("${workout.sets.count { it.done }} sets logged.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    vm.finishWorkout()
                }) { Text("Finish and save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    vm.discardWorkout()
                }) { Text("Abandon") }
            }
        )
    }

    val restRemaining = ((workout.restEndsAtMs - now) / 1000).toInt()

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(day.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            day.focus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        Fmt.duration(now - session.startMs),
                        style = MaterialTheme.typography.titleLarge,
                        color = StrideColors.Strength
                    )
                }
                Spacer(Modifier.height(Space.md))
                val done = workout.sets.count { it.done }
                val total = day.allBlocks.sumOf { it.sets }
                ThinProgress(
                    progress = if (total > 0) done.toFloat() / total else 0f,
                    color = StrideColors.Strength
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    "$done of $total sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (restRemaining > 0) {
            item {
                RestBanner(
                    secondsLeft = restRemaining,
                    onSkip = { vm.skipRest() },
                    onAdd = { vm.addRest(30) },
                    modifier = Modifier.padding(horizontal = Space.lg)
                )
            }
        }

        item { SectionTitle("Warm-up") }
        items(day.warmup, key = { "w_" + it.exerciseId }) { block ->
            BlockCard(
                block = block,
                sets = workout.sets,
                suggestion = null,
                onLog = { index -> logging = block to index },
                onUndo = { vm.undoSet(it) },
                onOpen = { onOpenExercise(block.exercise) },
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        item { SectionTitle("Main work") }
        items(day.main, key = { "m_" + it.exerciseId }) { block ->
            BlockCard(
                block = block,
                sets = workout.sets,
                suggestion = workout.suggestions[block.exerciseId],
                onLog = { index -> logging = block to index },
                onUndo = { vm.undoSet(it) },
                onOpen = { onOpenExercise(block.exercise) },
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        item { SectionTitle("Finisher") }
        items(day.finisher, key = { "f_" + it.exerciseId }) { block ->
            BlockCard(
                block = block,
                sets = workout.sets,
                suggestion = null,
                onLog = { index -> logging = block to index },
                onUndo = { vm.undoSet(it) },
                onOpen = { onOpenExercise(block.exercise) },
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        item {
            Button(
                onClick = { confirmFinish = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.lg),
                colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Strength)
            ) { Text("Finish session") }
        }

        item { Spacer(Modifier.height(Space.xl)) }
    }
}

@Composable
private fun RestBanner(
    secondsLeft: Int,
    onSkip: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    StrideCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Rest", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    Fmt.clockDuration(secondsLeft),
                    style = MaterialTheme.typography.displayMedium,
                    color = StrideColors.Run
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                OutlinedButton(onClick = onAdd) { Text("+30s") }
                Button(onClick = onSkip) { Text("Skip") }
            }
        }
    }
}

@Composable
private fun BlockCard(
    block: Block,
    sets: List<SetLog>,
    suggestion: Progression.Suggestion?,
    onLog: (Int) -> Unit,
    onUndo: (Long) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logged = sets.filter { it.exerciseId == block.exerciseId }

    StrideCard(modifier) {
        Row(
            Modifier.fillMaxWidth().clickable { onOpen() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(block.exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${block.sets} sets x ${block.targetReps}  -  rest ${block.restSec}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "How to",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (block.note.isNotEmpty()) {
            Spacer(Modifier.height(Space.xs))
            Text(
                block.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        suggestion?.let {
            Spacer(Modifier.height(Space.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(it.headline, StrideColors.Steps)
                Spacer(Modifier.width(Space.sm))
                Text(
                    it.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            repeat(block.sets) { index ->
                val entry = logged.firstOrNull { it.setIndex == index }
                SetChip(
                    index = index,
                    entry = entry,
                    onClick = { onLog(index) },
                    onLongClick = { entry?.let { onUndo(it.id) } }
                )
            }
        }
    }
}

@Composable
private fun SetChip(
    index: Int,
    entry: SetLog?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val done = entry != null
    val bg = if (done) StrideColors.Strength.copy(alpha = 0.18f)
    else MaterialTheme.colorScheme.surfaceVariant
    val border = if (done) StrideColors.Strength else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .size(width = 74.dp, height = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { if (done) onLongClick() else onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (entry == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Set ${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Log", style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (entry.weightKg > 0) "${trimNumber(entry.weightKg)} kg" else "Body",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${entry.reps} reps",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun trimNumber(v: Double): String =
    if (v == v.toInt().toDouble()) v.toInt().toString() else String.format("%.1f", v)

private fun buzz(context: android.content.Context) {
    runCatching {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val manager = context.getSystemService(android.os.VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.os.Vibrator::class.java)
        }
        vibrator?.vibrate(
            android.os.VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 180), -1)
        )
    }
}

@Composable
private fun LogSetDialog(
    block: Block,
    setIndex: Int,
    suggestion: Progression.Suggestion?,
    previous: SetLog?,
    onConfirm: (reps: Int, weightKg: Double, rpe: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var reps by remember {
        mutableStateOf((previous?.reps ?: suggestion?.reps ?: 10).toString())
    }
    var weight by remember {
        mutableStateOf(trimNumber(previous?.weightKg ?: suggestion?.weightKg ?: 0.0))
    }
    var rpe by remember { mutableIntStateOf(previous?.rpe ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${block.exercise.name}  -  set ${setIndex + 1}") },
        text = {
            Column {
                suggestion?.let {
                    Text(
                        it.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Space.md))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Reps") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { input ->
                            weight = input.filter { it.isDigit() || it == '.' }.take(6)
                        },
                        label = { Text("Weight kg") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(Space.md))
                Text(
                    "Effort (RPE)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    listOf(6, 7, 8, 9, 10).forEach { value ->
                        FilterChip(
                            selected = rpe == value,
                            onClick = { rpe = if (rpe == value) 0 else value },
                            label = { Text(value.toString()) }
                        )
                    }
                }
                Spacer(Modifier.height(Space.xs))
                Text(
                    "RPE 8 means you could have done two more reps. Logging it lets the app " +
                        "tell the difference between a hard set and an easy one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    reps.toIntOrNull() ?: 0,
                    weight.toDoubleOrNull() ?: 0.0,
                    rpe
                )
            }) { Text("Save set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@Composable
fun ExerciseDetailDialog(exercise: Exercise, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Pill(exercise.pattern.label, StrideColors.Strength)
                    Pill(exercise.equipment.label, StrideColors.Run)
                    Pill(exercise.level.label, StrideColors.Posture)
                }
                Spacer(Modifier.height(Space.md))
                StatRow("Works", exercise.muscles)
                if (exercise.secondary.isNotEmpty()) {
                    StatRow("Also", exercise.secondary.joinToString(", "))
                }
                if (exercise.tempo.isNotEmpty()) StatRow("Tempo", exercise.tempo)

                Spacer(Modifier.height(Space.md))
                Text("How to do it", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xs))
                exercise.steps.forEachIndexed { index, step ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text(
                            "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(step, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (exercise.cues.isNotEmpty()) {
                    Spacer(Modifier.height(Space.md))
                    Text("Cues", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Space.xs))
                    exercise.cues.forEach {
                        Text(
                            "- $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StrideColors.Steps,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                if (exercise.mistakes.isNotEmpty()) {
                    Spacer(Modifier.height(Space.md))
                    Text("Common mistakes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Space.xs))
                    exercise.mistakes.forEach {
                        Text(
                            "- $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StrideColors.Warning,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                if (exercise.breathing.isNotEmpty()) {
                    Spacer(Modifier.height(Space.md))
                    Text("Breathing", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Space.xs))
                    Text(exercise.breathing, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

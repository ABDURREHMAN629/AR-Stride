package com.stride.fit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.fit.training.PostureRoutine
import com.stride.fit.training.RoutineCategory
import com.stride.fit.ui.PostureViewModel
import com.stride.fit.ui.components.Pill
import com.stride.fit.ui.components.ProgressRing
import com.stride.fit.ui.components.SectionTitle
import com.stride.fit.ui.components.StatRow
import com.stride.fit.ui.components.StrideCard
import com.stride.fit.ui.theme.Space
import com.stride.fit.ui.theme.StrideColors
import com.stride.fit.util.Fmt
import kotlinx.coroutines.delay

@Composable
fun PostureScreen(
    vm: PostureViewModel,
    onOpenMind: () -> Unit,
    contentPadding: PaddingValues
) {
    val player by vm.player.collectAsStateWithLifecycle()

    if (player.routine != null) {
        RoutinePlayer(vm)
    } else {
        RoutineList(vm, onOpenMind, contentPadding)
    }
}

@Composable
private fun RoutineList(
    vm: PostureViewModel,
    onOpenMind: () -> Unit,
    contentPadding: PaddingValues
) {
    val streak by vm.streak.collectAsStateWithLifecycle()
    val todayLogs by vm.todayLogs.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()

    val todaySeconds = todayLogs.sumOf { it.seconds }
    val goalSeconds = profile.postureMinutesGoal * 60
    var category by remember { mutableStateOf(RoutineCategory.POSTURE) }
    val shown = if (category == RoutineCategory.YOGA) vm.yogaRoutines else vm.postureRoutines

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        item {
            Column(Modifier.padding(horizontal = Space.lg, vertical = Space.sm)) {
                Text("Posture and Yoga", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Undo what sitting does to you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${todaySeconds / 60} of ${profile.postureMinutesGoal} minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ProgressRing(
                        progress = if (goalSeconds > 0)
                            todaySeconds.toFloat() / goalSeconds else 0f,
                        size = 78.dp,
                        strokeWidth = 8.dp,
                        color = StrideColors.Posture
                    ) {
                        Text(
                            "${todaySeconds / 60}m",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                if (streak > 0) {
                    Spacer(Modifier.height(Space.md))
                    Pill("$streak day streak", StrideColors.Posture)
                }
            }
        }

        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Text("Why these routines", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xs))
                Text(
                    "Two patterns cause most desk-related posture complaints. Upper-crossed " +
                        "pulls the head forward and rounds the shoulders. Lower-crossed tips the " +
                        "pelvis forward and over-arches the lower back.\n\n" +
                        "Stretching the tight muscles alone rarely holds, so each routine pairs a " +
                        "release with the strengthening drill that keeps the new position.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            StrideCard(Modifier.padding(horizontal = Space.lg), onClick = onOpenMind) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Mind", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Box breathing, 5-4-3-2-1 grounding, presence drills",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Pill("Open", StrideColors.Steps)
                }
                Spacer(Modifier.height(Space.sm))
                Text(
                    "Breath work counts towards the same daily streak as posture. Four seconds " +
                        "in, four hold, four out, four hold - the pacer does the counting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrideColors.Steps
                )
            }
        }

        item {
            Row(
                Modifier.padding(horizontal = Space.lg),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                RoutineCategory.entries.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option.label) }
                    )
                }
            }
        }

        item {
            SectionTitle(
                if (category == RoutineCategory.YOGA) "Yoga flows" else "Posture routines"
            )
        }
        items(shown, key = { it.id }) { routine ->
            RoutineCard(
                routine = routine,
                onStart = { vm.start(routine) },
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        if (todayLogs.isNotEmpty()) {
            item { SectionTitle("Completed today") }
            item {
                StrideCard(Modifier.padding(horizontal = Space.lg)) {
                    todayLogs.forEach { log ->
                        StatRow(log.name, Fmt.clockDuration(log.seconds))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(Space.xl)) }
    }
}

@Composable
private fun RoutineCard(
    routine: PostureRoutine,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    StrideCard(modifier, onClick = onStart) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.padding(end = Space.sm)) {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    routine.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Pill("${routine.minutes} min", StrideColors.Posture)
        }
        Spacer(Modifier.height(Space.sm))
        Text(
            "Targets: ${routine.targets}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            routine.whenToDo,
            style = MaterialTheme.typography.bodySmall,
            color = StrideColors.Posture
        )
        Spacer(Modifier.height(Space.md))
        Text(
            "${routine.steps.size} drills",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Guided player. One drill at a time, with the coaching points on screen while you
 * are actually doing it, which is the only moment they are any use.
 */
@Composable
private fun RoutinePlayer(vm: PostureViewModel) {
    val player by vm.player.collectAsStateWithLifecycle()
    val routine = player.routine ?: return

    LaunchedEffect(player.running) {
        while (player.running) {
            delay(1000)
            vm.tick()
        }
    }

    if (player.finished) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(Space.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Routine complete", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(Space.sm))
                Text(
                    "${Fmt.clockDuration(player.elapsedSeconds)} of posture work logged.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Space.xl))
                Button(
                    onClick = { vm.dismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Posture)
                ) { Text("Done") }
            }
        }
        return
    }

    val step = routine.steps.getOrNull(player.stepIndex) ?: return
    val exercise = step.exercise
    val totalSteps = routine.steps.size

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(Space.lg)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Drill ${player.stepIndex + 1} of $totalSteps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { vm.stopEarly() }) { Text("End") }
        }

        Spacer(Modifier.height(Space.lg))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = if (step.seconds > 0)
                    1f - player.secondsLeft.toFloat() / step.seconds else 0f,
                size = 180.dp,
                strokeWidth = 14.dp,
                color = StrideColors.Posture
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Fmt.clockDuration(player.secondsLeft),
                        style = MaterialTheme.typography.displayMedium
                    )
                    if (step.perSide) {
                        Text(
                            if (player.onSecondSide) "Left side" else "Right side",
                            style = MaterialTheme.typography.bodySmall,
                            color = StrideColors.Posture
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Space.lg))
        Text(exercise.name, style = MaterialTheme.typography.headlineMedium)
        if (step.reps > 0) {
            Text(
                "${step.reps} reps",
                style = MaterialTheme.typography.titleMedium,
                color = StrideColors.Posture
            )
        }
        if (step.note.isNotEmpty()) {
            Spacer(Modifier.height(Space.xs))
            Text(
                step.note,
                style = MaterialTheme.typography.bodyMedium,
                color = StrideColors.Posture
            )
        }

        Spacer(Modifier.height(Space.md))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            exercise.steps.forEachIndexed { index, line ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(
                        "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = Space.sm)
                    )
                    Text(line, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (exercise.cues.isNotEmpty()) {
                Spacer(Modifier.height(Space.md))
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
                Spacer(Modifier.height(Space.sm))
                exercise.mistakes.forEach {
                    Text(
                        "- $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = StrideColors.Warning,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            OutlinedButton(onClick = { vm.back() }, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            OutlinedButton(onClick = { vm.togglePause() }, modifier = Modifier.weight(1f)) {
                Text(if (player.running) "Pause" else "Resume")
            }
            Button(
                onClick = { vm.advance() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Posture)
            ) { Text("Next") }
        }
    }
}

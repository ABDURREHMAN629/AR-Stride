package com.stride.fit.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.fit.training.BreathPattern
import com.stride.fit.training.Grounding
import com.stride.fit.training.PresenceExercise
import com.stride.fit.ui.MindViewModel
import com.stride.fit.ui.components.Pill
import com.stride.fit.ui.components.SectionTitle
import com.stride.fit.ui.components.StatRow
import com.stride.fit.ui.components.StrideCard
import com.stride.fit.ui.theme.Space
import com.stride.fit.ui.theme.StrideColors
import com.stride.fit.util.Fmt
import kotlinx.coroutines.delay

@Composable
fun MindScreen(
    vm: MindViewModel,
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    val breath by vm.breath.collectAsStateWithLifecycle()
    val grounding by vm.grounding.collectAsStateWithLifecycle()

    when {
        breath.pattern != null -> BreathingPlayer(vm)
        grounding.active -> GroundingPlayer(vm)
        else -> MindList(vm, onBack, contentPadding)
    }
}


@Composable
private fun MindList(
    vm: MindViewModel,
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    val todayLogs by vm.todayLogs.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<PresenceExercise?>(null) }

    detail?.let { exercise ->
        PresenceDialog(
            exercise = exercise,
            onDone = {
                vm.logPresence(exercise.id, exercise.name, exercise.minutes)
                detail = null
            },
            onDismiss = { detail = null }
        )
    }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.sm, vertical = Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Mind", style = MaterialTheme.typography.titleLarge)
            }
        }

        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Text("Why this is in a fitness app", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xs))
                Text(
                    "Breathing and attention are trainable in exactly the way a squat is. " +
                        "A long, controlled exhale raises vagal tone, which slows the heart and " +
                        "pulls you out of a stress response - that is mechanical, not mystical.\n\n" +
                        "Everything logged here counts towards the same daily streak as your " +
                        "posture work.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionTitle("Breathing") }
        items(vm.patterns, key = { it.id }) { pattern ->
            BreathPatternCard(
                pattern = pattern,
                onStart = { vm.startBreathing(pattern) },
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        item { SectionTitle("Grounding") }
        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(Grounding.NAME, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Five senses, two minutes each",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Pill("${Grounding.totalMinutes} min", StrideColors.Posture)
                }
                Spacer(Modifier.height(Space.sm))
                Text(
                    "Anxiety and rumination live in the past and the future. Naming what is " +
                        "physically around you right now forces attention into the present, " +
                        "where neither of those exists.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.md))
                Grounding.steps.forEach { step ->
                    StatRow("${step.count} - ${step.sense}", "${step.seconds / 60} min")
                }
                Spacer(Modifier.height(Space.sm))
                Text(
                    "Two minutes a sense rather than the usual quick run-through. The speed is " +
                        "what makes most people say it does not work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrideColors.Posture
                )
                Spacer(Modifier.height(Space.md))
                Button(
                    onClick = { vm.startGrounding() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Posture)
                ) { Text("Start grounding") }
            }
        }

        item { SectionTitle("Presence of mind") }
        items(vm.presence, key = { it.id }) { exercise ->
            StrideCard(
                Modifier.padding(horizontal = Space.lg),
                onClick = { detail = exercise }
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            exercise.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Pill("${exercise.minutes} min", StrideColors.Run)
                }
                Spacer(Modifier.height(Space.xs))
                Text(
                    exercise.whenToUse,
                    style = MaterialTheme.typography.bodySmall,
                    color = StrideColors.Run
                )
            }
        }

        if (todayLogs.isNotEmpty()) {
            item { SectionTitle("Logged today") }
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
private fun BreathPatternCard(
    pattern: BreathPattern,
    onStart: () -> Unit,
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
                Text(pattern.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    pattern.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Pill(
                "${pattern.totalMinutes(pattern.defaultRounds)} min",
                StrideColors.Steps
            )
        }

        Spacer(Modifier.height(Space.sm))
        Text(
            pattern.benefit,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (expanded) {
            Spacer(Modifier.height(Space.md))
            Text("Sit like this", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(Space.xs))
            pattern.posture.forEach {
                Text(
                    "- $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrideColors.Posture,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
            if (pattern.notes.isNotEmpty()) {
                Spacer(Modifier.height(Space.sm))
                pattern.notes.forEach {
                    Text(
                        "- $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Button(
                onClick = onStart,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Steps)
            ) { Text("Start") }
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) { Text(if (expanded) "Less" else "How to sit") }
        }
    }
}


/**
 * The pacer. The circle grows over the whole inhale, holds its size through a hold and
 * shrinks over the exhale, so it can be followed with the eyes half shut and the
 * countdown never read.
 */
@Composable
private fun BreathingPlayer(vm: MindViewModel) {
    val ui by vm.breath.collectAsStateWithLifecycle()
    val pattern = ui.pattern ?: return
    val phase = ui.phase
    val context = LocalContext.current

    LaunchedEffect(ui.running) {
        while (ui.running) {
            delay(1000)
            vm.tickBreath()
        }
    }

    // A short buzz on each phase change means you can do this with your eyes closed.
    LaunchedEffect(ui.phaseIndex, ui.currentRound) {
        if (ui.running && ui.elapsedSeconds > 0) pulse(context)
    }

    if (ui.finished) {
        CompletionPanel(
            title = "Breathing complete",
            body = "${ui.rounds} rounds of ${pattern.name}, " +
                "${Fmt.clockDuration(ui.elapsedSeconds)} total.\n\n" +
                "Notice your heart rate and your shoulders before you get up.",
            accent = StrideColors.Steps,
            onDone = { vm.dismissBreathing() }
        )
        return
    }

    val targetScale = phase?.targetScale ?: 0.5f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = (phase?.seconds ?: 4) * 1000),
        label = "breath"
    )

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
                Text(pattern.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Round ${ui.currentRound} of ${ui.rounds}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { vm.stopBreathing() }) { Text("End") }
        }

        Spacer(Modifier.height(Space.xl))

        Box(
            Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer ring shows progress through the current phase.
            Canvas(Modifier.size(260.dp)) {
                val stroke = 6.dp.toPx()
                drawArc(
                    color = StrideColors.Steps.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = StrideColors.Steps,
                    startAngle = -90f,
                    sweepAngle = 360f * ui.phaseProgress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Box(
                Modifier
                    .size(220.dp)
                    .scale(animatedScale)
                    .clip(CircleShape)
                    .background(StrideColors.Steps.copy(alpha = 0.22f))
                    .border(2.dp, StrideColors.Steps.copy(alpha = 0.6f), CircleShape)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    ui.secondsLeft.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    phase?.label ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = StrideColors.Steps
                )
            }
        }

        Spacer(Modifier.height(Space.lg))
        Text(
            phase?.instruction ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Space.lg))
        StrideCard {
            Text("Posture", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(Space.xs))
            pattern.posture.take(3).forEach {
                Text(
                    "- $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            OutlinedButton(
                onClick = { vm.toggleBreathPause() },
                modifier = Modifier.weight(1f)
            ) { Text(if (ui.running) "Pause" else "Resume") }
            Button(
                onClick = { vm.stopBreathing() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Steps)
            ) { Text("Finish") }
        }
        Spacer(Modifier.height(Space.md))
    }
}


@Composable
private fun GroundingPlayer(vm: MindViewModel) {
    val ui by vm.grounding.collectAsStateWithLifecycle()
    val step = ui.step
    val context = LocalContext.current

    LaunchedEffect(ui.running) {
        while (ui.running) {
            delay(1000)
            vm.tickGrounding()
        }
    }

    LaunchedEffect(ui.stepIndex) {
        if (ui.elapsedSeconds > 0) pulse(context)
    }

    if (ui.finished) {
        CompletionPanel(
            title = "Grounding complete",
            body = "Ten minutes across all five senses.\n\n" +
                "Compare how you feel now with how you felt when you started. That " +
                "difference is the whole point, and it is worth actually noticing.",
            accent = StrideColors.Posture,
            onDone = { vm.dismissGrounding() }
        )
        return
    }

    if (step == null) return

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
                Text(Grounding.NAME, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Step ${ui.stepIndex + 1} of ${Grounding.steps.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { vm.stopGrounding() }) { Text("End") }
        }

        Spacer(Modifier.height(Space.lg))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                step.count.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = StrideColors.Posture
            )
            Spacer(Modifier.width(Space.md))
            Column {
                Text(step.sense, style = MaterialTheme.typography.headlineMedium)
                Text(
                    Fmt.clockDuration(ui.secondsLeft),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(Space.sm))
        Text(step.prompt, style = MaterialTheme.typography.titleLarge)

        if (step.eyesClosed) {
            Spacer(Modifier.height(Space.xs))
            Pill("Eyes closed", StrideColors.Posture)
        }

        // Tap a dot as you find each thing. The count belongs to you, not the timer.
        Spacer(Modifier.height(Space.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            repeat(step.count) { index ->
                val found = index < ui.ticked
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (found) StrideColors.Posture.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            2.dp,
                            if (found) StrideColors.Posture
                            else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                        .clickable { vm.tickGroundingItem() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (found) StrideColors.Posture
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            "Tap each one as you find it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(Space.md))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            step.guidance.forEach { line ->
                Text(
                    "- $line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }

        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            OutlinedButton(
                onClick = { vm.backGrounding() },
                modifier = Modifier.weight(1f)
            ) { Text("Back") }
            OutlinedButton(
                onClick = { vm.toggleGroundingPause() },
                modifier = Modifier.weight(1f)
            ) { Text(if (ui.running) "Pause" else "Resume") }
            Button(
                onClick = { vm.advanceGrounding() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Posture)
            ) { Text("Next") }
        }
        Spacer(Modifier.height(Space.md))
    }
}


@Composable
private fun CompletionPanel(
    title: String,
    body: String,
    accent: androidx.compose.ui.graphics.Color,
    onDone: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(Space.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(Space.md))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.xl))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) { Text("Done") }
        }
    }
}

@Composable
private fun PresenceDialog(
    exercise: PresenceExercise,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Pill("${exercise.minutes} min", StrideColors.Run)
                }
                Spacer(Modifier.height(Space.md))
                Text(
                    exercise.whenToUse,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StrideColors.Run
                )
                Spacer(Modifier.height(Space.md))
                exercise.steps.forEachIndexed { index, line ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text(
                            "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(Space.md))
                Text("Why it works", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xs))
                Text(
                    exercise.why,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text("Mark as done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/** Short buzz to mark a phase change, so the eyes can stay shut. */
private fun pulse(context: android.content.Context) {
    runCatching {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.os.Vibrator::class.java)
        }
        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(60, 90))
    }
}

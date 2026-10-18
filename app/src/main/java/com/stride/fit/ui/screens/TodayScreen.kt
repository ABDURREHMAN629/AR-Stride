package com.stride.fit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.fit.Brand
import com.stride.fit.data.repo.Profile
import com.stride.fit.data.repo.StepComparison
import com.stride.fit.health.HealthMath
import com.stride.fit.sensors.GateState
import com.stride.fit.sensors.MotionClass
import com.stride.fit.ui.TodayViewModel
import com.stride.fit.ui.components.BarChart
import com.stride.fit.ui.components.MetricTile
import com.stride.fit.ui.components.Pill
import com.stride.fit.ui.components.ProgressRing
import com.stride.fit.ui.components.SectionTitle
import com.stride.fit.ui.components.StatRow
import com.stride.fit.ui.components.StrideCard
import com.stride.fit.ui.components.ThinProgress
import com.stride.fit.ui.theme.Space
import com.stride.fit.ui.theme.StrideColors
import com.stride.fit.util.Fmt
import com.stride.fit.util.Time
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    vm: TodayViewModel,
    onOpenTraining: () -> Unit,
    onOpenPosture: () -> Unit,
    onOpenRun: () -> Unit,
    onOpenAccuracy: () -> Unit,
    contentPadding: PaddingValues
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val gate by vm.gateState.collectAsStateWithLifecycle()
    val serviceRunning by vm.serviceRunning.collectAsStateWithLifecycle()
    val hasPedometer by vm.hasPedometer.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()

    val summary = ui.summary
    val stat = summary?.stat
    val slot = vm.todaySlot

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        item { GreetingHeader(profile) }

        item {
            StepRingCard(
                steps = stat?.steps ?: 0,
                goal = profile.stepGoal,
                progress = (summary?.stepProgress ?: 0.0).toFloat(),
                distanceM = stat?.distanceM ?: 0.0,
                kcal = summary?.totalBurnKcal ?: 0.0,
                activeMinutes = stat?.activeMinutes ?: 0,
                metric = profile.useMetric,
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        if (!serviceRunning) {
            item {
                StrideCard(Modifier.padding(horizontal = Space.lg)) {
                    Text(
                        "Step tracking is not running",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "Grant the physical activity permission and ${Brand.NAME} will count in the " +
                            "background, even with the screen off.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            AccuracyCard(
                gate = gate,
                rejectedRiding = stat?.rejectedRiding ?: 0,
                hasPedometer = hasPedometer,
                onOpen = onOpenAccuracy,
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        item { SectionTitle("Today by hour") }
        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                BarChart(
                    values = ui.hourlySteps.map { it.toFloat() },
                    labels = listOf("00", "", "", "06", "", "", "12", "", "", "18", "", ""),
                    color = StrideColors.Steps,
                    height = 110.dp
                )
                Spacer(Modifier.height(Space.sm))
                val busiest = ui.hourlySteps.withIndex().maxByOrNull { it.value }
                Text(
                    text = if (busiest != null && busiest.value > 0)
                        "Most active hour: ${busiest.index}:00 with ${Fmt.steps(busiest.value)} steps"
                    else "No steps recorded yet today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionTitle("Today's plan") }
        item {
            PlanCard(
                slot = slot,
                strengthDone = ui.strengthSessionsThisWeek,
                strengthGoal = profile.strengthDaysGoal,
                postureStreak = ui.postureStreak,
                onOpenTraining = onOpenTraining,
                onOpenPosture = onOpenPosture,
                onOpenRun = onOpenRun,
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        item { SectionTitle("Compared to before") }
        item {
            ComparisonCard(
                comparison = ui.comparison,
                metric = profile.useMetric,
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        item { SectionTitle("This week") }
        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                BarChart(
                    values = ui.week.map { it.steps.toFloat() },
                    labels = ui.week.map { Time.shortWeekday(it.epochDay).take(1) },
                    color = StrideColors.Steps,
                    goal = profile.stepGoal.toFloat(),
                    height = 110.dp
                )
                Spacer(Modifier.height(Space.md))
                val total = ui.week.sumOf { it.steps }
                val avg = if (ui.week.isNotEmpty()) total / ui.week.size else 0
                StatRow("Weekly total", Fmt.steps(total))
                StatRow("Daily average", Fmt.steps(avg))
                StatRow(
                    "Days at goal",
                    "${ui.week.count { it.steps >= profile.stepGoal }} of ${ui.week.size}"
                )
            }
        }

        item { SectionTitle("Energy") }
        item {
            EnergyCard(
                profile = profile,
                restingKcal = profile.bmr,
                activityKcal = stat?.kcal ?: 0.0,
                exerciseKcal = (stat?.runKcal ?: 0.0) + (stat?.strengthKcal ?: 0.0) +
                    (stat?.mobilityKcal ?: 0.0),
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        item { Spacer(Modifier.height(Space.xl)) }
    }
}

@Composable
private fun GreetingHeader(profile: Profile) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    Column(Modifier.padding(horizontal = Space.lg, vertical = Space.sm)) {
        Text(
            text = Brand.NAME.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (profile.name.isBlank()) greeting else "$greeting, ${profile.name}",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = LocalDate.now(Time.zone())
                .format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM")),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepRingCard(
    steps: Int,
    goal: Int,
    progress: Float,
    distanceM: Double,
    kcal: Double,
    activeMinutes: Int,
    metric: Boolean,
    modifier: Modifier = Modifier
) {
    StrideCard(modifier) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = progress,
                size = 210.dp,
                strokeWidth = 18.dp,
                color = StrideColors.Steps
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Fmt.steps(steps),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "of ${Fmt.steps(goal)} steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (steps >= goal) {
                        Spacer(Modifier.height(Space.xs))
                        Pill("Goal reached", StrideColors.Strength)
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.lg))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricTile(
                "Distance",
                Fmt.distanceValue(distanceM, metric),
                Fmt.distanceUnit(metric),
                StrideColors.Run
            )
            MetricTile("Burned", kcal.roundToInt().toString(), "kcal", StrideColors.Strength)
            MetricTile("Active", activeMinutes.toString(), "min", StrideColors.Posture)
        }
    }
}

/**
 * The card that makes the step filter visible. Rejected steps are shown rather than
 * hidden, because the only way to trust a pedometer that claims to ignore cycling is to
 * watch it doing so.
 */
@Composable
private fun AccuracyCard(
    gate: GateState,
    rejectedRiding: Int,
    hasPedometer: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motionColor = when (gate.motion) {
        MotionClass.WALKING, MotionClass.RUNNING -> StrideColors.Steps
        MotionClass.CYCLING, MotionClass.VEHICLE -> StrideColors.Warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    StrideCard(modifier, onClick = onOpen) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Step accuracy", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (gate.ridingLatched)
                        "Riding detected. Steps are being ignored."
                    else "Right now: ${gate.motion.label.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Pill(if (gate.open) "Counting" else "Paused", motionColor)
        }

        if (rejectedRiding > 0) {
            Spacer(Modifier.height(Space.md))
            Text(
                text = "${Fmt.steps(rejectedRiding)} false steps rejected today while riding.",
                style = MaterialTheme.typography.bodySmall,
                color = StrideColors.Warning
            )
        }

        if (!hasPedometer) {
            Spacer(Modifier.height(Space.sm))
            Text(
                text = "This phone has no hardware pedometer, so ${Brand.NAME} is detecting steps " +
                    "from the accelerometer instead.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanCard(
    slot: com.stride.fit.training.Programs.WeekSlot,
    strengthDone: Int,
    strengthGoal: Int,
    postureStreak: Int,
    onOpenTraining: () -> Unit,
    onOpenPosture: () -> Unit,
    onOpenRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    StrideCard(modifier) {
        if (slot.strength != null) {
            Text(slot.strength.name + " - " + slot.strength.focus,
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                "${slot.strength.workingSets} working sets, about ${slot.strength.estimatedMinutes} minutes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Space.md))
            Button(onClick = onOpenTraining, modifier = Modifier.fillMaxWidth()) {
                Text("Start strength session")
            }
        } else {
            Text(slot.cardioLabel, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                "No lifting scheduled today. Keep the legs moving instead.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Space.md))
            Button(onClick = onOpenRun, modifier = Modifier.fillMaxWidth()) {
                Text("Start a run")
            }
        }

        Spacer(Modifier.height(Space.sm))
        OutlinedButton(onClick = onOpenPosture, modifier = Modifier.fillMaxWidth()) {
            Text("Posture: ${slot.posture.name} (${slot.posture.minutes} min)")
        }

        Spacer(Modifier.height(Space.lg))
        Text(
            "Strength sessions this week",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.xs))
        ThinProgress(
            progress = if (strengthGoal > 0) strengthDone.toFloat() / strengthGoal else 0f,
            color = StrideColors.Strength
        )
        Spacer(Modifier.height(Space.xs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "$strengthDone of $strengthGoal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (postureStreak > 0) {
                Text(
                    "Posture streak: $postureStreak ${if (postureStreak == 1) "day" else "days"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrideColors.Posture
                )
            }
        }
    }
}

/**
 * Today against yesterday, this week against last. A step count on its own is a number
 * without meaning; the direction you are moving in is the part that is useful.
 */
@Composable
private fun ComparisonCard(
    comparison: StepComparison,
    metric: Boolean,
    modifier: Modifier = Modifier
) {
    StrideCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Today vs yesterday", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${Fmt.steps(comparison.today)} against ${Fmt.steps(comparison.yesterday)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DeltaPill(comparison.dayDelta)
        }

        Spacer(Modifier.height(Space.lg))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Last week",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.xs))
                BarChart(
                    values = comparison.lastWeek.map { it.steps.toFloat() },
                    color = StrideColors.Rejected,
                    height = 64.dp
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "This week",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.xs))
                BarChart(
                    values = comparison.thisWeek.map { it.steps.toFloat() },
                    color = StrideColors.Steps,
                    height = 64.dp
                )
            }
        }

        Spacer(Modifier.height(Space.md))
        StatRow("This week", Fmt.steps(comparison.thisWeekTotal), valueColor = StrideColors.Steps)
        StatRow("Last week", Fmt.steps(comparison.lastWeekTotal))
        StatRow(
            "Daily average",
            "${Fmt.steps(comparison.thisWeekAvg)} vs ${Fmt.steps(comparison.lastWeekAvg)}"
        )
        StatRow(
            "Distance",
            "${Fmt.distance(comparison.thisWeekDistanceM, metric)} vs " +
                Fmt.distance(comparison.lastWeekDistanceM, metric)
        )
        if (comparison.bestDaySteps > 0) {
            StatRow(
                "Best day this week",
                "${comparison.bestDayLabel}, ${Fmt.steps(comparison.bestDaySteps)}"
            )
        }

        Spacer(Modifier.height(Space.sm))
        val percent = comparison.weekChangePercent
        Text(
            text = when {
                !comparison.hasLastWeek ->
                    "Once there is a full week behind you, this compares the two directly."
                percent == null -> ""
                percent > 5 -> "You are up $percent% on last week. That is the trend that matters."
                percent < -5 -> "You are down ${-percent}% on last week. One quiet week is not a problem, two is worth noticing."
                else -> "Holding steady within ${kotlin.math.abs(percent)}% of last week."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeltaPill(delta: Int) {
    val color = when {
        delta > 0 -> StrideColors.Steps
        delta < 0 -> StrideColors.Warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val text = when {
        delta > 0 -> "+${Fmt.steps(delta)}"
        delta < 0 -> "-${Fmt.steps(-delta)}"
        else -> "level"
    }
    Pill(text, color)
}

@Composable
private fun EnergyCard(
    profile: Profile,
    restingKcal: Double,
    activityKcal: Double,
    exerciseKcal: Double,
    modifier: Modifier = Modifier
) {
    val tdee = HealthMath.tdee(restingKcal, activityKcal, exerciseKcal)
    val deficit = HealthMath.recommendedDailyDeficitKcal(profile.weightKg)

    StrideCard(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricTile("Resting", restingKcal.roundToInt().toString(), "kcal")
            MetricTile("Movement", activityKcal.roundToInt().toString(), "kcal", StrideColors.Steps)
            MetricTile("Exercise", exerciseKcal.roundToInt().toString(), "kcal", StrideColors.Strength)
        }
        Spacer(Modifier.height(Space.md))
        StatRow("Estimated burn today", "${tdee.roundToInt()} kcal", valueColor = StrideColors.Run)
        StatRow(
            "To lose ~0.5 kg a week",
            "${(tdee - deficit).roundToInt()} kcal intake",
            valueColor = MaterialTheme.colorScheme.onSurface
        )
        StatRow("Protein target", "${HealthMath.proteinTargetG(profile.weightKg, profile.heightCm, profile.male)} g")
        StatRow("Water target", "${HealthMath.waterTargetMl(profile.weightKg, 45) / 1000.0} L")
        Spacer(Modifier.height(Space.sm))
        Text(
            "Resting burn uses the Mifflin-St Jeor equation. Movement burn comes from your " +
                "actual step cadence, not a flat per-step figure.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

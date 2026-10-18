package com.stride.fit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.fit.Brand
import com.stride.fit.BuildConfig
import com.stride.fit.data.repo.Profile
import com.stride.fit.health.HealthMath
import com.stride.fit.ui.ProfileViewModel
import com.stride.fit.ui.components.LineChart
import com.stride.fit.ui.components.MetricTile
import com.stride.fit.ui.components.Pill
import com.stride.fit.ui.components.SectionTitle
import com.stride.fit.ui.components.StatRow
import com.stride.fit.ui.components.StrideCard
import com.stride.fit.ui.components.ThinProgress
import com.stride.fit.ui.theme.Space
import com.stride.fit.ui.theme.StrideColors
import com.stride.fit.util.Fmt
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    vm: ProfileViewModel,
    onOpenAccuracy: () -> Unit,
    contentPadding: PaddingValues
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val weights by vm.weightHistory.collectAsStateWithLifecycle()
    val week by vm.week.collectAsStateWithLifecycle()

    var editProfile by remember { mutableStateOf(false) }
    var editGoals by remember { mutableStateOf(false) }
    var logWeight by remember { mutableStateOf(false) }

    if (editProfile) {
        EditProfileDialog(
            profile = profile,
            onSave = { vm.updateProfile { _ -> it }; editProfile = false },
            onDismiss = { editProfile = false }
        )
    }
    if (editGoals) {
        EditGoalsDialog(
            profile = profile,
            onSave = { vm.updateProfile { _ -> it }; editGoals = false },
            onDismiss = { editGoals = false }
        )
    }
    if (logWeight) {
        LogWeightDialog(
            current = profile.weightKg,
            onSave = { vm.logWeight(it); logWeight = false },
            onDismiss = { logWeight = false }
        )
    }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        item {
            Column(Modifier.padding(horizontal = Space.lg, vertical = Space.sm)) {
                Text("You", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${if (profile.male) "Male" else "Female"}, ${profile.age} - " +
                        "${profile.heightCm.roundToInt()} cm - ${Fmt.weight(profile.weightKg)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            BodyCard(
                profile = profile,
                startWeightKg = weights.firstOrNull()?.weightKg,
                modifier = Modifier.padding(horizontal = Space.lg)
            )
        }

        item { SectionTitle("Weight", trailing = "Log", onTrailingClick = { logWeight = true }) }
        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                if (weights.size >= 2) {
                    LineChart(
                        values = weights.map { it.weightKg.toFloat() },
                        color = StrideColors.Run,
                        height = 130.dp
                    )
                    Spacer(Modifier.height(Space.md))
                    val first = weights.first().weightKg
                    val last = weights.last().weightKg
                    val change = last - first
                    StatRow("Latest", Fmt.weight(last, profile.useMetric))
                    StatRow(
                        "Change over ${weights.size} entries",
                        "${if (change > 0) "+" else ""}${Fmt.oneDecimal(change)} kg",
                        valueColor = if (change <= 0) StrideColors.Steps else StrideColors.Warning
                    )
                } else {
                    Text(
                        "Log your weight a few times and a trend line will appear here. " +
                            "Weigh yourself at the same time of day, ideally first thing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Space.md))
                    Button(onClick = { logWeight = true }) { Text("Log weight") }
                }
            }
        }

        item { SectionTitle("Energy and targets") }
        item { EnergyTargetsCard(profile, week.sumOf { it.steps } / 7, Modifier.padding(horizontal = Space.lg)) }

        item { SectionTitle("Heart rate zones") }
        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Text(
                    "Maximum heart rate about ${profile.maxHr} bpm, estimated with the Tanaka " +
                        "formula. Zones use heart rate reserve from a resting rate of " +
                        "${profile.restingHr} bpm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.md))
                HealthMath.zones(profile.age, profile.restingHr).forEach { zone ->
                    StatRow("${zone.name}  ${zone.purpose}", "${zone.lowBpm}-${zone.highBpm}")
                }
            }
        }

        item { SectionTitle("Settings") }
        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                SettingRow("Profile details", "Age, height, sex, name") { editProfile = true }
                SettingRow("Goals", "Steps, calories, sessions, posture") { editGoals = true }
                SettingRow(
                    "Step accuracy",
                    "Sensitivity and the live motion classifier",
                    onClick = onOpenAccuracy
                )
                Spacer(Modifier.height(Space.sm))
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Metric units", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (profile.useMetric) "Kilometres and kilograms" else "Miles and pounds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = profile.useMetric,
                        onCheckedChange = { checked ->
                            vm.updateProfile { it.copy(useMetric = checked) }
                        }
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-pause runs", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Stops the clock when you stop moving",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = profile.autoPauseRuns,
                        onCheckedChange = { checked ->
                            vm.updateProfile { it.copy(autoPauseRuns = checked) }
                        }
                    )
                }
            }
        }

        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Text("Your data stays here", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xs))
                Text(
                    "${Brand.NAME} has no account and no server. Your steps, runs, workouts and " +
                        "body data are stored only on this phone, and GPS works from satellites " +
                        "without any network connection. The internet is used for one thing " +
                        "only: downloading map tiles when you ask for an offline area.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Text("About ${Brand.NAME}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xs))
                Text(
                    "Version " + BuildConfig.VERSION_NAME + "\n" +
                        "Built by Abdur Rehman (AR)\n" +
                        "gabdurehman493@gmail.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(Modifier.height(Space.xl)) }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onClick) { Text("Edit") }
    }
}

@Composable
private fun BodyCard(
    profile: Profile,
    startWeightKg: Double?,
    modifier: Modifier = Modifier
) {
    val bmi = profile.bmi
    val range = profile.healthyRange
    val category = HealthMath.bmiCategory(bmi)
    val categoryColor = when {
        bmi < 18.5 -> StrideColors.Run
        bmi < 25 -> StrideColors.Steps
        bmi < 30 -> StrideColors.Strength
        else -> StrideColors.Warning
    }

    StrideCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricTile("Body mass index", Fmt.oneDecimal(bmi), "", categoryColor)
            Pill(category, categoryColor)
        }

        Spacer(Modifier.height(Space.md))
        Text(
            "Healthy range for ${profile.heightCm.roundToInt()} cm is " +
                "${Fmt.oneDecimal(range.start)} to ${Fmt.oneDecimal(range.endInclusive)} kg.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(Space.md))
        val toTarget = profile.weightKg - profile.targetWeightKg
        if (abs(toTarget) > 0.2) {
            val weeks = HealthMath.weeksToTarget(profile.weightKg, profile.targetWeightKg)
            StatRow("Target weight", Fmt.weight(profile.targetWeightKg, profile.useMetric))
            StatRow(
                "To go",
                "${Fmt.oneDecimal(abs(toTarget))} kg" +
                    if (weeks > 0) " - about $weeks weeks at 0.5 kg a week" else ""
            )
            // Progress only means something measured against where you started, so the
            // bar appears once there is a first logged weight to measure from.
            val startingGap = startWeightKg?.minus(profile.targetWeightKg)
            if (startingGap != null && startingGap > 0.1) {
                Spacer(Modifier.height(Space.xs))
                ThinProgress(
                    progress = (1.0 - (toTarget / startingGap)).toFloat().coerceIn(0f, 1f),
                    color = StrideColors.Steps
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    "Measured from your first logged weight of " +
                        "${Fmt.oneDecimal(startWeightKg)} kg.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EnergyTargetsCard(profile: Profile, avgSteps: Int, modifier: Modifier = Modifier) {
    val bmr = profile.bmr
    // Approximate the daily movement burn from the recent step average.
    val movementKcal = HealthMath.kcalFromSteps(
        avgSteps, 105.0, profile.weightKg, profile.heightCm, profile.male
    )
    val tdee = HealthMath.tdee(bmr, movementKcal, 0.0)
    val deficit = HealthMath.recommendedDailyDeficitKcal(profile.weightKg)

    StrideCard(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricTile("Resting burn", bmr.roundToInt().toString(), "kcal")
            MetricTile("Typical day", tdee.roundToInt().toString(), "kcal", StrideColors.Run)
            MetricTile("Deficit", deficit.toString(), "kcal", StrideColors.Strength)
        }
        Spacer(Modifier.height(Space.md))
        StatRow("Intake to lose ~0.5 kg a week", "${(tdee - deficit).roundToInt()} kcal")
        StatRow(
            "Protein",
            "${HealthMath.proteinTargetG(profile.weightKg, profile.heightCm, profile.male)} g a day"
        )
        StatRow(
            "Lean mass estimate",
            Fmt.weight(HealthMath.leanBodyMassKg(profile.weightKg, profile.heightCm, profile.male))
        )
        Spacer(Modifier.height(Space.sm))
        Text(
            "These are estimates, not measurements. Use them as a starting point and adjust " +
                "based on what the scale actually does over two or three weeks.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun EditProfileDialog(
    profile: Profile,
    onSave: (Profile) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var age by remember { mutableStateOf(profile.age.toString()) }
    var height by remember { mutableStateOf(Fmt.oneDecimal(profile.heightCm)) }
    var weight by remember { mutableStateOf(Fmt.oneDecimal(profile.weightKg)) }
    var target by remember { mutableStateOf(Fmt.oneDecimal(profile.targetWeightKg)) }
    var resting by remember { mutableStateOf(profile.restingHr.toString()) }
    var male by remember { mutableStateOf(profile.male) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile details") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    NumberField(age, { age = it }, "Age", Modifier.weight(1f))
                    NumberField(resting, { resting = it }, "Resting HR", Modifier.weight(1f))
                }
                Spacer(Modifier.height(Space.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    NumberField(height, { height = it }, "Height cm", Modifier.weight(1f), decimal = true)
                    NumberField(weight, { weight = it }, "Weight kg", Modifier.weight(1f), decimal = true)
                }
                Spacer(Modifier.height(Space.sm))
                NumberField(target, { target = it }, "Target weight kg", Modifier.fillMaxWidth(), decimal = true)
                Spacer(Modifier.height(Space.md))
                Text("Sex", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Space.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    FilterChip(male, { male = true }, { Text("Male") })
                    FilterChip(!male, { male = false }, { Text("Female") })
                }
                Spacer(Modifier.height(Space.sm))
                Text(
                    "Sex, age, height and weight all feed the resting burn equation and your " +
                        "stride length, so keeping them current keeps the numbers honest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    profile.copy(
                        name = name.trim(),
                        age = age.toIntOrNull()?.coerceIn(10, 100) ?: profile.age,
                        heightCm = height.toDoubleOrNull()?.coerceIn(120.0, 230.0) ?: profile.heightCm,
                        weightKg = weight.toDoubleOrNull()?.coerceIn(30.0, 300.0) ?: profile.weightKg,
                        targetWeightKg = target.toDoubleOrNull()?.coerceIn(30.0, 300.0)
                            ?: profile.targetWeightKg,
                        restingHr = resting.toIntOrNull()?.coerceIn(35, 110) ?: profile.restingHr,
                        male = male
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditGoalsDialog(
    profile: Profile,
    onSave: (Profile) -> Unit,
    onDismiss: () -> Unit
) {
    var steps by remember { mutableStateOf(profile.stepGoal.toString()) }
    var calories by remember { mutableStateOf(profile.calorieBurnGoal.toString()) }
    var sessions by remember { mutableStateOf(profile.strengthDaysGoal.toString()) }
    var posture by remember { mutableStateOf(profile.postureMinutesGoal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Goals") },
        text = {
            Column {
                NumberField(steps, { steps = it }, "Daily steps", Modifier.fillMaxWidth())
                Spacer(Modifier.height(Space.sm))
                NumberField(calories, { calories = it }, "Daily burn kcal", Modifier.fillMaxWidth())
                Spacer(Modifier.height(Space.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    NumberField(sessions, { sessions = it }, "Strength days", Modifier.weight(1f))
                    NumberField(posture, { posture = it }, "Posture min", Modifier.weight(1f))
                }
                Spacer(Modifier.height(Space.sm))
                Text(
                    "The step goal also moves on its own: once your seven-day median clears it, " +
                        "${Brand.NAME} raises it by 500, and lowers it if you fall well short.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    profile.copy(
                        stepGoal = steps.toIntOrNull()?.coerceIn(1000, 40000) ?: profile.stepGoal,
                        calorieBurnGoal = calories.toIntOrNull()?.coerceIn(100, 3000)
                            ?: profile.calorieBurnGoal,
                        strengthDaysGoal = sessions.toIntOrNull()?.coerceIn(1, 7)
                            ?: profile.strengthDaysGoal,
                        postureMinutesGoal = posture.toIntOrNull()?.coerceIn(1, 120)
                            ?: profile.postureMinutesGoal
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LogWeightDialog(
    current: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(Fmt.oneDecimal(current)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log weight") },
        text = {
            Column {
                NumberField(value, { value = it }, "Weight kg", Modifier.fillMaxWidth(), decimal = true)
                Spacer(Modifier.height(Space.sm))
                Text(
                    "Day-to-day swings of a kilo or two are water, not fat. The trend over " +
                        "weeks is the only part worth reacting to.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                value.toDoubleOrNull()?.let { onSave(it.coerceIn(30.0, 300.0)) }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(
                if (decimal) input.filter { it.isDigit() || it == '.' }.take(6)
                else input.filter { it.isDigit() }.take(6)
            )
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = modifier
    )
}

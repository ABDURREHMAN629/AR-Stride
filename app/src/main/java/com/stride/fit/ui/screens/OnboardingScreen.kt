package com.stride.fit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stride.fit.Brand
import com.stride.fit.data.repo.Profile
import com.stride.fit.health.HealthMath
import com.stride.fit.ui.components.StatRow
import com.stride.fit.ui.components.StrideCard
import com.stride.fit.ui.theme.Space
import com.stride.fit.util.Fmt
import kotlin.math.roundToInt

/**
 * First launch. The profile is already seeded with the numbers this build was made
 * for, so this screen exists to confirm them and to collect the one value the maths
 * genuinely cannot guess: age.
 */
@Composable
fun OnboardingScreen(
    profile: Profile,
    onFinish: (Profile) -> Unit
) {
    var age by remember { mutableStateOf(profile.age.toString()) }
    var height by remember { mutableStateOf(Fmt.oneDecimal(profile.heightCm)) }
    var weight by remember { mutableStateOf(Fmt.oneDecimal(profile.weightKg)) }
    var target by remember { mutableStateOf(Fmt.oneDecimal(profile.targetWeightKg)) }
    var male by remember { mutableStateOf(profile.male) }
    var name by remember { mutableStateOf(profile.name) }

    val heightValue = height.toDoubleOrNull() ?: profile.heightCm
    val weightValue = weight.toDoubleOrNull() ?: profile.weightKg
    val ageValue = age.toIntOrNull() ?: profile.age
    val bmi = HealthMath.bmi(weightValue, heightValue)
    val range = HealthMath.healthyWeightRangeKg(heightValue)
    val bmr = HealthMath.bmr(weightValue, heightValue, ageValue, male)

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(Space.lg)
    ) {
        Spacer(Modifier.height(Space.xl))
        Text("Welcome to ${Brand.NAME}", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(Space.sm))
        Text(
            "Steps that refuse to count while you ride, GPS runs that work with no signal, " +
                "a full body strength plan and posture work that actually holds.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(Space.xl))
        StrideCard {
            Text("Your details", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.md))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                label = { Text("Name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                OnboardField(age, { age = it }, "Age", Modifier.weight(1f))
                OnboardField(height, { height = it }, "Height cm", Modifier.weight(1f), true)
            }
            Spacer(Modifier.height(Space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                OnboardField(weight, { weight = it }, "Weight kg", Modifier.weight(1f), true)
                OnboardField(target, { target = it }, "Target kg", Modifier.weight(1f), true)
            }
            Spacer(Modifier.height(Space.md))
            Text(
                "Sex",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                FilterChip(male, { male = true }, { Text("Male") })
                FilterChip(!male, { male = false }, { Text("Female") })
            }
            Spacer(Modifier.height(Space.sm))
            Text(
                "Age is the one number the equations cannot infer, and it changes your resting " +
                    "burn by about 5 kcal per year.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Space.md))
        StrideCard {
            Text("What that gives us", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.sm))
            StatRow("Body mass index", "${Fmt.oneDecimal(bmi)}  ${HealthMath.bmiCategory(bmi)}")
            StatRow(
                "Healthy weight range",
                "${Fmt.oneDecimal(range.start)} - ${Fmt.oneDecimal(range.endInclusive)} kg"
            )
            StatRow("Resting burn", "${bmr.roundToInt()} kcal a day")
            StatRow(
                "Walking stride",
                "${(HealthMath.walkingStrideM(heightValue, male) * 100).roundToInt()} cm"
            )
            StatRow(
                "Protein target",
                "${HealthMath.proteinTargetG(weightValue, heightValue, male)} g a day"
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                "Stride length is derived from your height, so step distance is personal rather " +
                    "than a generic 0.75 m per step.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Space.md))
        StrideCard {
            Text("Permissions, and your data", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.sm))
            Text(
                "Physical activity lets the pedometer run in the background. Location traces " +
                    "your runs, and is only asked for when you record one.\n\n" +
                    "The network is used for exactly one thing: downloading map tiles when you " +
                    "ask for an offline area. There is no account and no server, so your steps, " +
                    "runs, workouts and weight never leave this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Space.xl))
        Button(
            onClick = {
                onFinish(
                    profile.copy(
                        name = name.trim(),
                        age = ageValue.coerceIn(10, 100),
                        heightCm = heightValue.coerceIn(120.0, 230.0),
                        weightKg = weightValue.coerceIn(30.0, 300.0),
                        targetWeightKg = (target.toDoubleOrNull() ?: profile.targetWeightKg)
                            .coerceIn(30.0, 300.0),
                        male = male
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Start tracking") }
        Spacer(Modifier.height(Space.xxl))
    }
}

@Composable
private fun OnboardField(
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
                else input.filter { it.isDigit() }.take(3)
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

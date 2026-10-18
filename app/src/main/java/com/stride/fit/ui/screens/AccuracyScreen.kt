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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.fit.Brand
import com.stride.fit.sensors.MotionClass
import com.stride.fit.sensors.RecognisedActivity
import com.stride.fit.sensors.Sensitivity
import com.stride.fit.ui.ProfileViewModel
import com.stride.fit.ui.components.MetricTile
import com.stride.fit.ui.components.Pill
import com.stride.fit.ui.components.SectionTitle
import com.stride.fit.ui.components.StatRow
import com.stride.fit.ui.components.StrideCard
import com.stride.fit.ui.theme.Space
import com.stride.fit.ui.theme.StrideColors
import com.stride.fit.util.Fmt

/**
 * Everything the step filter is doing, laid out so it can be checked rather than
 * trusted. Take the phone on a bike ride with this screen open and the classifier's
 * decisions are visible in real time.
 */
@Composable
fun AccuracyScreen(
    vm: ProfileViewModel,
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    val gate by vm.gateState.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val running by vm.serviceRunning.collectAsStateWithLifecycle()
    val hasPedometer by vm.hasPedometer.collectAsStateWithLifecycle()
    val week by vm.week.collectAsStateWithLifecycle()

    val f = gate.features
    val motionColor = when (gate.motion) {
        MotionClass.WALKING, MotionClass.RUNNING -> StrideColors.Steps
        MotionClass.CYCLING, MotionClass.VEHICLE -> StrideColors.Warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                Text("Step accuracy", style = MaterialTheme.typography.titleLarge)
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
                        Text(
                            "Right now",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            gate.motion.label,
                            style = MaterialTheme.typography.headlineMedium,
                            color = motionColor
                        )
                    }
                    Pill(
                        if (gate.open) "Counting steps" else "Not counting",
                        if (gate.open) StrideColors.Steps else StrideColors.Rejected
                    )
                }

                if (gate.ridingLatched) {
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        "Riding detected. The gate stays shut for 90 seconds after the last " +
                            "vehicle or bicycle reading.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StrideColors.Warning
                    )
                }

                if (gate.recognised != RecognisedActivity.UNKNOWN) {
                    Spacer(Modifier.height(Space.sm))
                    StatRow("Play services thinks", gate.recognised.name.lowercase().replace('_', ' '))
                }

                if (!running) {
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        "The tracking service is not running. Grant the physical activity " +
                            "permission to start it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StrideColors.Warning
                    )
                }
            }
        }

        item { SectionTitle("Live motion signals") }
        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricTile("Cadence", f.cadenceSpm.toInt().toString(), "spm", StrideColors.Steps)
                    MetricTile("Rhythm", Fmt.oneDecimal(f.dominantHz), "Hz", StrideColors.Run)
                    MetricTile("Regularity", Fmt.percent(f.periodicity), "", StrideColors.Posture)
                }
                Spacer(Modifier.height(Space.md))
                StatRow("Movement intensity", "${Fmt.oneDecimal(f.sd)} m/s2")
                StatRow("Peak to peak", "${Fmt.oneDecimal(f.p2p)} m/s2")
                StatRow("Impact sharpness", Fmt.oneDecimal(f.kurtosis))
                StatRow("Sample rate", "${f.sampleHz.toInt()} Hz")
                Spacer(Modifier.height(Space.sm))
                Text(
                    "Walking shakes a phone hard and sharply: peak to peak above about " +
                        "4.5 m/s2, with sharp heel-strike impacts. Sitting on a bicycle produces a " +
                        "gentle, near-sinusoidal wobble at a similar rhythm, which is exactly why " +
                        "rhythm alone is not enough to tell them apart.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionTitle("Rejections") }
        item {
            val todayStat = week.lastOrNull()
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                StatRow("Counted today", Fmt.steps(todayStat?.steps ?: 0),
                    valueColor = StrideColors.Steps)
                StatRow(
                    "Rejected as riding",
                    Fmt.steps(todayStat?.rejectedRiding ?: 0),
                    valueColor = StrideColors.Warning
                )
                StatRow(
                    "Rejected as unclear",
                    Fmt.steps(todayStat?.rejectedUnclear ?: 0),
                    valueColor = StrideColors.Rejected
                )
                Spacer(Modifier.height(Space.sm))
                val weekRejected = week.sumOf { it.rejectedRiding }
                if (weekRejected > 0) {
                    Text(
                        "${Fmt.steps(weekRejected)} phantom steps rejected this week. Without this " +
                            "filter every one of them would have counted towards your daily total.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Nothing rejected as riding this week.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { SectionTitle("Sensitivity") }
        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Sensitivity.entries.forEach { option ->
                        FilterChip(
                            selected = profile.sensitivity == option,
                            onClick = { vm.setSensitivity(option) },
                            label = { Text(option.label) }
                        )
                    }
                }
                Spacer(Modifier.height(Space.md))
                Text(
                    profile.sensitivity.blurb,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    "If your rides are still adding steps, move to Strict. If slow walking " +
                        "around the house is being missed, move to Lenient.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionTitle("How it works") }
        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                ExplainRow(
                    "1. Hardware pedometer",
                    if (hasPedometer)
                        "Your phone has a dedicated step counter chip. It runs at almost no " +
                            "battery cost and keeps counting with the screen off - but it cannot " +
                            "tell walking from a bumpy ride."
                    else
                        "This phone has no step counter chip, so ${Brand.NAME} detects footfalls from " +
                            "the accelerometer using adaptive peak detection."
                )
                ExplainRow(
                    "2. Motion classifier",
                    "The accelerometer is sampled at 50 Hz in batches, and every second ${Brand.NAME} " +
                        "judges four things: how hard you are moving, at what rhythm, how " +
                        "metronomic that rhythm is, and how sharp each impact is. Cycling defeats " +
                        "any one of those tests on its own. It does not defeat all four."
                )
                ExplainRow(
                    "3. Activity recognition",
                    "Google Play services provides a second opinion. A confident bicycle or " +
                        "vehicle reading shuts the gate for 90 seconds, which catches the hard " +
                        "case of a motorbike on a rough road."
                )
                ExplainRow(
                    "4. Hysteresis",
                    "Two consecutive walking windows open the gate; three non-walking windows " +
                        "close it. Without this the count would flicker every time you paused."
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    "Steps are held for twelve seconds before being credited, so the batched " +
                        "accelerometer data covering them has definitely arrived. That is why the " +
                        "counter can lag slightly behind your feet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(Modifier.height(Space.xl)) }
    }
}

@Composable
private fun ExplainRow(title: String, body: String) {
    Column(Modifier.padding(vertical = Space.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(2.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

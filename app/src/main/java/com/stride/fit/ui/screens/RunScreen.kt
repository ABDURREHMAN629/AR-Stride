package com.stride.fit.ui.screens

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.fit.Brand
import com.stride.fit.data.db.RunSession
import com.stride.fit.run.RunService
import com.stride.fit.ui.RunViewModel
import com.stride.fit.ui.components.EmptyState
import com.stride.fit.ui.components.MetricTile
import com.stride.fit.ui.components.Pill
import com.stride.fit.map.RouteMap
import com.stride.fit.map.TileStore
import com.stride.fit.ui.components.SectionTitle
import com.stride.fit.ui.components.StatRow
import com.stride.fit.ui.components.StrideCard
import com.stride.fit.ui.theme.Space
import com.stride.fit.ui.theme.StrideColors
import com.stride.fit.util.Fmt
import com.stride.fit.util.Time
import kotlin.math.roundToInt

@Composable
fun RunScreen(
    vm: RunViewModel,
    tileStore: TileStore,
    onRequestLocation: () -> Unit,
    onOpenMaps: () -> Unit,
    hasLocationPermission: Boolean,
    contentPadding: PaddingValues
) {
    val live by vm.liveState.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val bests by vm.personalBests.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    var activity by remember { mutableStateOf("RUN") }
    var confirmFinish by remember { mutableStateOf(false) }

    detail?.let { d ->
        RunDetailDialog(
            detail = d,
            tileStore = tileStore,
            metric = profile.useMetric,
            onDelete = { vm.delete(d.session.id) },
            onDismiss = { vm.clearDetail() }
        )
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish this run?") },
            text = {
                Text(
                    "Recorded ${Fmt.distance(live.distanceM, profile.useMetric)} in " +
                        "${Fmt.duration(live.elapsedMs)}."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    RunService.finish(context)
                    vm.refreshBests()
                }) { Text("Save run") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    RunService.discard(context)
                }) { Text("Discard") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        if (live.active) {
            item {
                LiveRunCard(
                    live = live,
                    tileStore = tileStore,
                    metric = profile.useMetric,
                    onPause = { RunService.pause(context) },
                    onResume = { RunService.resume(context) },
                    onFinish = { confirmFinish = true },
                    modifier = Modifier.padding(horizontal = Space.lg)
                )
            }
        } else {
            item {
                StartRunCard(
                    activity = activity,
                    onOpenMaps = onOpenMaps,
                    onActivityChange = { activity = it },
                    hasPermission = hasLocationPermission,
                    onRequestPermission = onRequestLocation,
                    onStart = { RunService.start(context, activity) },
                    modifier = Modifier.padding(horizontal = Space.lg)
                )
            }

            if (bests.isNotEmpty()) {
                item { SectionTitle("Personal bests") }
                item {
                    StrideCard(Modifier.padding(horizontal = Space.lg)) {
                        bests.forEach { (label, value) -> StatRow(label, value) }
                    }
                }
            }
        }

        item { SectionTitle("History", trailing = if (history.isNotEmpty()) "${history.size} runs" else null) }

        if (history.isEmpty()) {
            item {
                StrideCard(Modifier.padding(horizontal = Space.lg)) {
                    EmptyState(
                        title = "No runs recorded yet",
                        message = "${Brand.NAME} traces your route with GPS, which needs no signal " +
                            "or data, and measures from your steps whenever GPS can't. Step " +
                            "outside, wait for a fix, and press start."
                    )
                }
            }
        } else {
            items(history, key = { it.id }) { session ->
                RunHistoryRow(
                    session = session,
                    metric = profile.useMetric,
                    onClick = { vm.loadDetail(session.id) },
                    modifier = Modifier.padding(horizontal = Space.lg)
                )
            }
        }

        item { Spacer(Modifier.height(Space.xl)) }
    }
}

@Composable
private fun StartRunCard(
    activity: String,
    onOpenMaps: () -> Unit,
    onActivityChange: (String) -> Unit,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    StrideCard(modifier) {
        Text("Record an activity", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Space.sm))
        Text(
            "GPS distance, pace, splits, elevation and the route you actually followed. " +
                "Positioning is satellite-based, so recording works with no signal and no data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.md))

        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf("RUN" to "Run", "WALK" to "Walk", "HIKE" to "Hike").forEach { (key, label) ->
                FilterChip(
                    selected = activity == key,
                    onClick = { onActivityChange(key) },
                    label = { Text(label) }
                )
            }
        }

        // Location is often switched off to save battery. Starting a run like that used
        // to record a timer and nothing else, with no warning at all.
        val context = LocalContext.current
        var locationOn by remember { mutableStateOf(isLocationOn(context)) }
        LifecycleResumeEffect(Unit) {
            locationOn = isLocationOn(context)
            onPauseOrDispose { }
        }

        Spacer(Modifier.height(Space.lg))
        if (hasPermission && !locationOn) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(StrideColors.Warning.copy(alpha = 0.10f))
                    .padding(Space.md)
            ) {
                Text(
                    "Location is switched off",
                    style = MaterialTheme.typography.titleMedium,
                    color = StrideColors.Warning
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    "Turn it on for a GPS route, a map and the most accurate distance. You " +
                        "can still start now: distance and pace will be measured from your " +
                        "steps instead, but there will be no map.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.sm))
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Warning)
                ) { Text("Turn on location") }
            }
            Spacer(Modifier.height(Space.sm))
            OutlinedButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text("Start without GPS")
            }
        } else if (hasPermission) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Run)
            ) { Text("Start") }
        } else {
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                Text("Allow location access")
            }
            Spacer(Modifier.height(Space.xs))
            Text(
                "Location is used only on this device to trace your route.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Space.sm))
        OutlinedButton(onClick = onOpenMaps, modifier = Modifier.fillMaxWidth()) {
            Text("Offline maps")
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            "Download the streets around you once and your routes draw on a real map " +
                "afterwards, with no connection needed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LiveRunCard(
    live: com.stride.fit.run.RunState,
    tileStore: TileStore,
    metric: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    StrideCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    live.autoPaused -> "Auto-paused"
                    live.paused -> "Paused"
                    else -> "Recording"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Pill(
                text = when {
                    !live.locationOn -> "Location off - using steps"
                    live.hasFix -> "GPS ${live.gpsAccuracyM.roundToInt()} m"
                    else -> "No GPS fix - using steps"
                },
                color = if (live.locationOn && live.hasFix) StrideColors.Steps else StrideColors.Warning
            )
        }

        Spacer(Modifier.height(Space.lg))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Fmt.distanceValue(live.distanceM, metric),
                    style = MaterialTheme.typography.displayLarge,
                    color = StrideColors.Run
                )
                Text(
                    text = Fmt.distanceUnit(metric),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(Space.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricTile("Time", Fmt.duration(live.elapsedMs))
            MetricTile(
                "Pace",
                Fmt.pace(live.currentPaceSecPerKm, metric),
                Fmt.paceUnit(metric),
                StrideColors.Steps
            )
            MetricTile("Burned", live.kcal.roundToInt().toString(), "kcal", StrideColors.Strength)
        }

        Spacer(Modifier.height(Space.sm))
        StatRow("Steps", Fmt.steps(live.steps))
        StatRow("Cadence", if (live.cadenceSpm > 0) "${live.cadenceSpm} spm" else "--")
        if (live.source != com.stride.fit.run.DistanceSource.NONE) {
            StatRow("Distance from", live.source.label)
        }

        Spacer(Modifier.height(Space.md))
        if (live.route.size > 2) {
            RouteMap(
                points = live.route.map { it.lat to it.lon },
                tileStore = tileStore,
                routeColor = StrideColors.Run,
                height = 190.dp
            )
        } else {
            Text(
                text = if (!live.locationOn)
                    "No map while location is off. Turn location on and the route starts " +
                        "drawing from that point."
                else "The map appears once GPS has a fix. Outdoors and away from tall " +
                    "buildings this usually takes under a minute.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (live.splits.isNotEmpty()) {
            Spacer(Modifier.height(Space.md))
            live.splits.takeLast(3).forEach { split ->
                StatRow(
                    "Km ${split.index}",
                    "${Fmt.pace(split.paceSecPerKm, metric)} ${Fmt.paceUnit(metric)}"
                )
            }
        }

        Spacer(Modifier.height(Space.lg))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            OutlinedButton(
                onClick = if (live.paused) onResume else onPause,
                modifier = Modifier.weight(1f)
            ) { Text(if (live.paused) "Resume" else "Pause") }
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Warning)
            ) { Text("Finish") }
        }
    }
}

@Composable
private fun RunHistoryRow(
    session: RunSession,
    metric: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    StrideCard(modifier, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Fmt.distance(session.distanceM, metric),
                    style = MaterialTheme.typography.titleLarge,
                    color = StrideColors.Run
                )
                Text(
                    text = Time.dateTimeOf(session.startMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${Fmt.pace(session.avgPaceSecPerKm, metric)} ${Fmt.paceUnit(metric)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${Fmt.duration(session.movingMs)}  ${session.kcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RunDetailDialog(
    detail: com.stride.fit.ui.RunDetail,
    tileStore: TileStore,
    metric: Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = detail.session
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Time.dateTimeOf(s.startMs)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (detail.points.size > 2) {
                    RouteMap(
                        points = detail.points.map { it.lat to it.lon },
                        tileStore = tileStore,
                        routeColor = StrideColors.Run,
                        height = 200.dp
                    )
                    Spacer(Modifier.height(Space.md))
                } else {
                    Text(
                        "No route map for this one: GPS never got a usable fix, usually " +
                            "because location was switched off. Distance and pace were " +
                            "measured from your steps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Space.md))
                }
                if (s.note.isNotEmpty()) {
                    Pill(s.note, StrideColors.Run)
                    Spacer(Modifier.height(Space.sm))
                }
                StatRow("Distance", Fmt.distance(s.distanceM, metric))
                if (s.steps > 0) {
                    StatRow("Steps", Fmt.steps(s.steps))
                    if (s.avgCadenceSpm > 0) StatRow("Average cadence", "${s.avgCadenceSpm} spm")
                }
                StatRow("Moving time", Fmt.duration(s.movingMs))
                StatRow("Elapsed", Fmt.duration(s.elapsedMs))
                StatRow(
                    "Average pace",
                    "${Fmt.pace(s.avgPaceSecPerKm, metric)} ${Fmt.paceUnit(metric)}"
                )
                if (s.bestKmPaceSec > 0) {
                    StatRow(
                        "Best kilometre",
                        "${Fmt.pace(s.bestKmPaceSec, metric)} ${Fmt.paceUnit(metric)}"
                    )
                }
                StatRow("Energy", "${s.kcal.roundToInt()} kcal")
                StatRow("Elevation gain", "${s.elevGainM.roundToInt()} m")

                if (detail.splits.isNotEmpty()) {
                    Spacer(Modifier.height(Space.md))
                    Text("Splits", style = MaterialTheme.typography.titleMedium)
                    detail.splits.forEach { split ->
                        StatRow(
                            "Km ${split.index}",
                            "${Fmt.pace(split.paceSecPerKm, metric)} ${Fmt.paceUnit(metric)}"
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

private fun isLocationOn(context: Context): Boolean =
    runCatching { context.getSystemService(LocationManager::class.java)?.isLocationEnabled == true }
        .getOrDefault(false)

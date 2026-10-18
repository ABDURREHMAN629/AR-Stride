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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.fit.Brand
import com.stride.fit.data.db.MapRegion
import com.stride.fit.map.TileDownloader
import com.stride.fit.ui.MapViewModel
import com.stride.fit.ui.components.Pill
import com.stride.fit.ui.components.SectionTitle
import com.stride.fit.ui.components.StatRow
import com.stride.fit.ui.components.StrideCard
import com.stride.fit.ui.components.ThinProgress
import com.stride.fit.ui.theme.Space
import com.stride.fit.ui.theme.StrideColors
import com.stride.fit.util.Time
import java.util.Locale

/**
 * Download a map area once, then use it forever with no connection.
 *
 * Deliberately explicit about what is being fetched and how big it is, because a
 * silent multi-megabyte download on someone's mobile data is not a nice surprise.
 */
@Composable
fun OfflineMapsScreen(
    vm: MapViewModel,
    onBack: () -> Unit,
    onRequestLocation: () -> Unit,
    hasLocationPermission: Boolean,
    contentPadding: PaddingValues
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val regions by vm.regions.collectAsStateWithLifecycle()
    var areaName by remember { mutableStateOf("") }

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
                Text("Offline maps", style = MaterialTheme.typography.titleLarge)
            }
        }

        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                Text("How this works", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xs))
                Text(
                    "Download the streets around you once, over wifi. After that the map " +
                        "draws from your phone and never touches the network again - your runs " +
                        "show on a real map even with no signal and no data.\n\n" +
                        "New city? Download that area once when you arrive, and it works offline " +
                        "for the rest of the trip.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    "Map tiles are the only thing ${Brand.NAME} ever downloads. No step, run, workout " +
                        "or body data leaves this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrideColors.Steps
                )
            }
        }

        item { SectionTitle("Download an area") }
        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                if (!hasLocationPermission) {
                    Text(
                        "Location access is needed to know which area to fetch.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Space.md))
                    Button(onClick = onRequestLocation, modifier = Modifier.fillMaxWidth()) {
                        Text("Allow location access")
                    }
                    return@StrideCard
                }

                if (!ui.hasLocation) {
                    Text(
                        "Find where you are, then choose how much of the surrounding area to save.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Space.md))
                    Button(
                        onClick = { vm.locate() },
                        enabled = !ui.locating,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (ui.locating) "Finding you..." else "Use my location") }
                    if (ui.error.isNotEmpty()) {
                        Spacer(Modifier.height(Space.sm))
                        Text(
                            ui.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = StrideColors.Warning
                        )
                    }
                    return@StrideCard
                }

                StatRow(
                    "Centre",
                    String.format(
                        Locale.getDefault(), "%.4f, %.4f",
                        ui.currentLat ?: 0.0, ui.currentLon ?: 0.0
                    )
                )
                if (ui.alreadyCovered) {
                    Spacer(Modifier.height(Space.xs))
                    Pill("Already covered by a saved area", StrideColors.Steps)
                }

                Spacer(Modifier.height(Space.md))
                Text(
                    "Radius",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    listOf(2.0, 5.0, 10.0, 15.0).forEach { km ->
                        FilterChip(
                            selected = ui.radiusKm == km,
                            onClick = { vm.setRadius(km) },
                            label = { Text("${km.toInt()} km") }
                        )
                    }
                }

                Spacer(Modifier.height(Space.md))
                Text(
                    "Detail level",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    listOf(
                        15 to "Roads",
                        16 to "Streets",
                        17 to "Buildings"
                    ).forEach { (zoom, label) ->
                        FilterChip(
                            selected = ui.maxZoom == zoom,
                            onClick = { vm.setMaxZoom(zoom) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(Space.md))
                val tiles = ui.plannedTiles
                val overLimit = tiles > TileDownloader.MAX_TILES_PER_REGION
                StatRow("Tiles to fetch", "$tiles")
                StatRow("Approximate size", formatBytes(ui.plannedBytes))
                if (overLimit) {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "That is over the ${TileDownloader.MAX_TILES_PER_REGION} tile limit. " +
                            "Reduce the radius or the detail level.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StrideColors.Warning
                    )
                }

                Spacer(Modifier.height(Space.md))
                OutlinedTextField(
                    value = areaName,
                    onValueChange = { areaName = it.take(30) },
                    label = { Text("Name this area") },
                    placeholder = { Text("Home, Work, Lahore...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(Space.md))
                if (ui.progress.running) {
                    ThinProgress(ui.progress.fraction, color = StrideColors.Run)
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "${ui.progress.done + ui.progress.skipped} of ${ui.progress.total} tiles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Space.sm))
                    OutlinedButton(
                        onClick = { vm.cancelDownload() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cancel") }
                } else {
                    Button(
                        onClick = { vm.download(areaName) },
                        enabled = !overLimit && tiles > 0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StrideColors.Run)
                    ) { Text("Download this area") }

                    if (ui.progress.message.isNotEmpty()) {
                        Spacer(Modifier.height(Space.sm))
                        Text(
                            ui.progress.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ui.progress.failed > 0) StrideColors.Warning
                            else StrideColors.Steps
                        )
                    }
                    Spacer(Modifier.height(Space.sm))
                    OutlinedButton(
                        onClick = { vm.locate() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Re-centre on my location") }
                }

                Spacer(Modifier.height(Space.md))
                Text(
                    "Tiles come from OpenStreetMap, whose volunteers make the map free for " +
                        "everyone. Downloads are rate-limited and capped out of respect for their " +
                        "servers - please keep areas modest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SectionTitle(
                "Saved areas",
                trailing = if (regions.isNotEmpty()) "Clear all" else null,
                onTrailingClick = { vm.clearAll() }
            )
        }

        if (regions.isEmpty()) {
            item {
                StrideCard(Modifier.padding(horizontal = Space.lg)) {
                    Text(
                        "Nothing saved yet. Runs will still record and still draw their route - " +
                            "they just will not have streets behind them until you download an area.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(regions, key = { it.id }) { region ->
                RegionCard(
                    region = region,
                    onDelete = { vm.deleteRegion(region) },
                    modifier = Modifier.padding(horizontal = Space.lg)
                )
            }
        }

        item {
            StrideCard(Modifier.padding(horizontal = Space.lg)) {
                StatRow("Tiles on this phone", "${ui.storedTiles}")
                StatRow("Storage used", formatBytes(ui.storedBytes))
            }
        }

        item { Spacer(Modifier.height(Space.xl)) }
    }
}

@Composable
private fun RegionCard(
    region: MapRegion,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    StrideCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(region.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${region.radiusKm.toInt()} km radius  -  ${region.tileCount} tiles  -  " +
                        Time.dateTimeOf(region.downloadedAtMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}

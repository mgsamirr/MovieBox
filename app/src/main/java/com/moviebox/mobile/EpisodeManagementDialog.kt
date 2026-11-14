package com.moviebox.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moviebox.mobile.data.CatalogEpisode
import com.moviebox.mobile.data.CatalogTitle
import com.moviebox.mobile.data.EpisodeStore

/**
 * Read-only season and episode catalogue.
 * Episode ordering and metadata are catalogue-owned; users can only play or download.
 */
@Composable
internal fun EpisodeManagementDialog(
    series: CatalogTitle,
    onDismiss: () -> Unit,
    onPlay: (CatalogEpisode) -> Unit,
    onDownload: (CatalogEpisode) -> Unit
) {
    val context = LocalContext.current
    val store = remember(series.id) { EpisodeStore(context) }
    val episodes = remember(series.id) { store.all(series.id) }
    val accent = Color(0xFF9B72FF)
    val white = Color(0xFFF7F5FA)
    val muted = Color(0xFF9A9AA7)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111116),
        title = { Text(series.title, color = white) },
        text = {
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Seasons & episodes", color = accent)
                    Text("Official episode order • Read only", color = muted)
                }
                val groups = episodes.groupBy { it.seasonNumber }.toSortedMap()
                if (groups.isEmpty()) {
                    item {
                        Text(
                            "Is series ke episodes catalogue mein abhi available nahi hain.",
                            color = muted
                        )
                    }
                }
                groups.forEach { (seasonNumber, seasonEpisodes) ->
                    item {
                        Text(
                            "Season $seasonNumber",
                            color = white,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    items(seasonEpisodes.sortedBy { it.episodeNumber }, key = { it.id }) { episode ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B23))) {
                            Row(
                                Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Episode ${episode.episodeNumber} • ${episode.title}",
                                        color = white
                                    )
                                    if (episode.durationMinutes > 0) {
                                        Text("${episode.durationMinutes} min", color = muted)
                                    }
                                    if (episode.synopsis.isNotBlank()) {
                                        Text(episode.synopsis, color = muted)
                                    }
                                }
                                IconButton(
                                    onClick = { onPlay(episode) },
                                    enabled = episode.playbackUrl.isNotBlank()
                                ) {
                                    Icon(Icons.Default.PlayArrow, "Play episode", tint = accent)
                                }
                                IconButton(
                                    onClick = { onDownload(episode) },
                                    enabled = episode.downloadUrl.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Download, "Download episode", tint = accent)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

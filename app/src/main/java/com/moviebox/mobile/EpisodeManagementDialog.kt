package com.moviebox.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moviebox.mobile.data.CatalogEpisode
import com.moviebox.mobile.data.CatalogTitle
import com.moviebox.mobile.data.EpisodeStore
import java.util.UUID

@Composable
internal fun EpisodeManagementDialog(series: CatalogTitle, onDismiss: () -> Unit, onPlay: (CatalogEpisode) -> Unit) {
    val context = LocalContext.current
    val store = remember(series.id) { EpisodeStore(context) }
    var episodes by remember(series.id) { mutableStateOf(store.all(series.id)) }
    var season by remember { mutableStateOf("1") }
    var number by remember { mutableStateOf("1") }
    var title by remember { mutableStateOf("") }
    var synopsis by remember { mutableStateOf("") }
    var playback by remember { mutableStateOf("") }
    var download by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val accent = Color(0xFF9B72FF)
    val white = Color(0xFFF7F5FA)
    val muted = Color(0xFF9A9AA7)
    fun reload() { episodes = store.all(series.id) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color(0xFF111116),
        title = { Text("Seasons & episodes", color = white) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(series.title, color = accent) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(season, { season = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Season #") }, singleLine = true)
                    OutlinedTextField(number, { number = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Episode #") }, singleLine = true)
                } }
                item { OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Episode name") }, singleLine = true) }
                item { OutlinedTextField(synopsis, { synopsis = it }, Modifier.fillMaxWidth(), label = { Text("Description") }, minLines = 2) }
                item { OutlinedTextField(playback, { playback = it }, Modifier.fillMaxWidth(), label = { Text("Authorized HTTPS playback URL") }, singleLine = true) }
                item { OutlinedTextField(download, { download = it }, Modifier.fillMaxWidth(), label = { Text("Authorized HTTPS download URL (optional)") }, singleLine = true) }
                item { OutlinedTextField(duration, { duration = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Duration in minutes") }, singleLine = true) }
                item { Button(onClick = {
                    val s = season.toIntOrNull(); val n = number.toIntOrNull()
                    when {
                        s == null || s < 1 || n == null || n < 1 -> error = "Season aur episode number 1 ya usse zyada hona chahiye."
                        title.isBlank() -> error = "Episode ka naam likho."
                        playback.isNotBlank() && !playback.startsWith("https://") -> error = "Playback URL HTTPS hona chahiye."
                        download.isNotBlank() && !download.startsWith("https://") -> error = "Download URL HTTPS hona chahiye."
                        else -> {
                            store.save(series.id, CatalogEpisode(UUID.randomUUID().toString(), s, n, title.trim(), synopsis.trim(), playback.trim(), download.trim(), duration.toIntOrNull() ?: 0))
                            title = ""; synopsis = ""; playback = ""; download = ""; duration = ""; error = null; reload()
                        }
                    }
                }, Modifier.fillMaxWidth()) { Text("Save episode") } }
                error?.let { message -> item { Text(message, color = Color(0xFFFF7777)) } }
                val groups = episodes.groupBy { it.seasonNumber }.toSortedMap()
                if (groups.isEmpty()) item { Text("Abhi koi episode add nahi hua.", color = muted) }
                groups.forEach { (seasonNumber, list) ->
                    item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Season $seasonNumber", color = white)
                        TextButton(onClick = { store.deleteSeason(series.id, seasonNumber); reload() }) { Text("Delete season") }
                    } }
                    items(list, key = { it.id }) { episode ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B23))) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text("E${episode.episodeNumber} • ${episode.title}", color = white)
                                    if (episode.durationMinutes > 0) Text("${episode.durationMinutes} min", color = muted)
                                    if (episode.synopsis.isNotBlank()) Text(episode.synopsis, color = muted)
                                    Text(if (episode.playbackUrl.isBlank()) "Playback link missing" else "Online playback ready", color = accent)
                                }
                                Column {
                                    IconButton(onClick = { onPlay(episode) }, enabled = episode.playbackUrl.isNotBlank()) { Icon(Icons.Default.PlayArrow, "Play episode", tint = accent) }
                                    IconButton(onClick = { store.delete(series.id, episode.id); reload() }) { Icon(Icons.Default.Delete, "Delete episode", tint = muted) }
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

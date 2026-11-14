package com.moviebox.mobile

import android.net.Uri
import android.app.DownloadManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.moviebox.mobile.data.CatalogStore
import com.moviebox.mobile.data.CatalogTitle
import com.moviebox.mobile.data.ContentCategory
import com.moviebox.mobile.data.DownloadRepository
import com.moviebox.mobile.data.DownloadStatus
import com.moviebox.mobile.data.PlaybackSourceType
import com.moviebox.mobile.data.SavedDownload
import com.moviebox.mobile.data.VodMedia
import com.moviebox.mobile.data.WatchHistoryStore
import com.moviebox.mobile.data.RecentTitle
import kotlinx.coroutines.delay
import java.util.UUID

private val Black = Color(0xFF000000)
private val Panel = Color(0xFF111116)
private val Accent = Color(0xFF9B72FF)
private val Muted = Color(0xFF9A9AA7)
private val White = Color(0xFFF7F5FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MovieBoxApp() }
    }
}

private enum class AppTab(val label: String) {
    HOME("Home"), SEARCH("Search"), WATCHLIST("Saved"), DOWNLOADS("Downloads"), SETTINGS("Settings")
}

@Composable
private fun MovieBoxApp() {
    val context = LocalContext.current
    val catalog = remember { CatalogStore(context) }
    val downloads = remember { DownloadRepository(context) }
    val history = remember { WatchHistoryStore(context) }
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var titles by remember { mutableStateOf(catalog.all()) }
    var queue by remember { mutableStateOf(downloads.all()) }
    var recent by remember { mutableStateOf(history.all(catalog)) }
    var refresh by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<ContentCategory?>(null) }
    var selectedTitle by remember { mutableStateOf<CatalogTitle?>(null) }
    var editingTitle by remember { mutableStateOf<CatalogTitle?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var playerMedia by remember { mutableStateOf<VodMedia?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun reload() {
        titles = catalog.all()
        queue = downloads.all()
        recent = history.all(catalog)
        refresh++
    }

    val localPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            playerMedia = VodMedia("local-${uri.lastPathSegment}", "Offline video", uri, PlaybackSourceType.DOWNLOADED_FILE)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            queue = downloads.all()
            refresh++
            delay(2000)
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Black) {
            if (playerMedia != null) {
                MoviePlayer(playerMedia!!, onClose = { playerMedia = null })
            } else {
                Scaffold(
                    containerColor = Black,
                    bottomBar = {
                        NavigationBar(containerColor = Panel) {
                            AppTab.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = tab == item,
                                    onClick = { tab = item },
                                    icon = {
                                        Icon(when (item) {
                                            AppTab.HOME -> Icons.Default.Home
                                            AppTab.SEARCH -> Icons.Default.Search
                                            AppTab.WATCHLIST -> Icons.Default.Bookmark
                                            AppTab.DOWNLOADS -> Icons.Default.Download
                                            AppTab.SETTINGS -> Icons.Default.Settings
                                        }, contentDescription = item.label)
                                    },
                                    label = { Text(item.label, fontSize = 9.sp) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).background(Black).padding(horizontal = 18.dp)) {
                        Spacer(Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("MOVIEBOX", color = White, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                Text("Movies • Anime • Web Series", color = Muted, fontSize = 12.sp)
                            }
                            Box(Modifier.size(42.dp).background(Panel, CircleShape), contentAlignment = Alignment.Center) {
                                Text("MB", color = Accent, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        notice?.let { Text(it, color = Accent, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }
                        AnimatedContent(targetState = tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { current ->
                            when (current) {
                                AppTab.HOME -> HomeScreen(
                                    titles = titles,
                                    recent = recent,
                                    onAdd = { editingTitle = null; showEditor = true },
                                    onSelect = { selectedTitle = it },
                                    onCategory = { category = it; tab = AppTab.SEARCH },
                                    onOpenLocal = { localPicker.launch(arrayOf("video/*")) }
                                )
                                AppTab.SEARCH -> SearchScreen(
                                    titles = titles,
                                    query = query,
                                    onQuery = { query = it },
                                    category = category,
                                    onCategory = { category = it },
                                    onAdd = { editingTitle = null; showEditor = true },
                                    onSelect = { selectedTitle = it }
                                )
                                AppTab.WATCHLIST -> TitleListScreen(
                                    heading = "Your watchlist",
                                    items = titles.filter { it.isWatchlisted },
                                    empty = "Titles you save will appear here.",
                                    onSelect = { selectedTitle = it }
                                )
                                AppTab.DOWNLOADS -> DownloadsScreen(
                                    records = queue,
                                    repository = downloads,
                                    onRefresh = { reload() },
                                    onOpenLocal = { localPicker.launch(arrayOf("video/*")) }
                                )
                                AppTab.SETTINGS -> SettingsScreen(onOpenLocal = { localPicker.launch(arrayOf("video/*")) }, onAdd = { editingTitle = null; showEditor = true })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        TitleEditorDialog(
            initial = editingTitle,
            onDismiss = { showEditor = false },
            onSave = { item ->
                catalog.save(item)
                reload()
                showEditor = false
                notice = "Title saved to your MovieBox library."
            }
        )
    }

    selectedTitle?.let { item ->
        TitleDetailsDialog(
            item = item,
            onDismiss = { selectedTitle = null },
            onPlay = {
                val uri = Uri.parse(item.playbackUrl)
                if (uri.scheme == "https" && !uri.host.isNullOrBlank()) {
                    history.markPlayed(item)
                    recent = history.all(catalog)
                    playerMedia = VodMedia(item.id, item.title, uri, PlaybackSourceType.ONLINE_VOD)
                    selectedTitle = null
                } else notice = "This title has no authorized online playback link yet."
            },
            onDownload = {
                runCatching {
                    downloads.enqueue(item.downloadUrl, item.title, item.title + ".mp4")
                }.onSuccess {
                    reload()
                    notice = "Download added. Progress is shown in Downloads."
                    selectedTitle = null
                }.onFailure { notice = it.message ?: "Download could not start." }
            },
            onWatchlist = {
                catalog.toggleWatchlist(item.id)
                reload()
                selectedTitle = catalog.all().firstOrNull { it.id == item.id }
            },
            onEdit = { editingTitle = item; selectedTitle = null; showEditor = true },
            onDelete = {
                catalog.delete(item.id)
                reload()
                selectedTitle = null
                notice = "Title removed from your library."
            }
        )
    }
}

@Composable
private fun HomeScreen(
    titles: List<CatalogTitle>,
    recent: List<RecentTitle>,
    onAdd: () -> Unit,
    onSelect: (CatalogTitle) -> Unit,
    onCategory: (ContentCategory) -> Unit,
    onOpenLocal: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("YOUR ENTERTAINMENT SPACE", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Find your next story.", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Hindi-dubbed movies, anime and web series in one place.", color = Muted, fontSize = 13.sp)
                    Button(onClick = onAdd) { Text("＋ Add a title") }
                }
            }
        }
        item { Text("Explore categories", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContentCategory.entries.forEach { c ->
                    FilterChip(selected = false, onClick = { onCategory(c) }, label = { Text(c.label) })
                }
            }
        }
        if (recent.isNotEmpty()) {
            item { Text("Recently watched", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            items(recent.take(5), key = { "recent-${it.title.id}" }) { TitleCard(it.title, onSelect) }
        }
        item { Text("Recently added", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        if (titles.isEmpty()) {
            item { EmptyState("Your library is ready", "Add a title with provider-approved details and links. MovieBox does not invent a catalogue.") }
        } else {
            items(titles.sortedByDescending { it.id }.take(8), key = { it.id }) { TitleCard(it, onSelect) }
        }
        item { OutlinedButton(onClick = onOpenLocal, modifier = Modifier.fillMaxWidth()) { Text("Open a video already on this phone") } }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SearchScreen(
    titles: List<CatalogTitle>,
    query: String,
    onQuery: (String) -> Unit,
    category: ContentCategory?,
    onCategory: (ContentCategory?) -> Unit,
    onAdd: () -> Unit,
    onSelect: (CatalogTitle) -> Unit
) {
    val filtered = titles.filter {
        (category == null || it.category == category) &&
            (query.isBlank() || it.title.contains(query, true) || it.language.contains(query, true) || it.synopsis.contains(query, true))
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Search your library", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search titles") })
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = category == null, onClick = { onCategory(null) }, label = { Text("All") })
            ContentCategory.entries.forEach { c -> FilterChip(selected = category == c, onClick = { onCategory(if (category == c) null else c) }, label = { Text(c.label) }) }
        }
        if (filtered.isEmpty()) {
            EmptyState("No matching titles", "Add a provider-authorized title to start building your catalogue.")
            Button(onClick = onAdd) { Text("Add title") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(filtered, key = { it.id }) { TitleCard(it, onSelect) } }
        }
    }
}

@Composable
private fun TitleListScreen(heading: String, items: List<CatalogTitle>, empty: String, onSelect: (CatalogTitle) -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(heading, color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        if (items.isEmpty()) EmptyState("Nothing saved yet", empty)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(items, key = { it.id }) { TitleCard(it, onSelect) } }
    }
}

@Composable
private fun TitleCard(item: CatalogTitle, onSelect: (CatalogTitle) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(item) },
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(Color(0xFF211A32), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                if (item.posterUrl.startsWith("https://")) {
                    AsyncImage(model = item.posterUrl, contentDescription = "${item.title} poster", modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(item.title, color = White, fontWeight = FontWeight.SemiBold)
                Text(listOf(item.category.label, item.language, item.releaseYear).filter { it.isNotBlank() }.joinToString(" • "), color = Muted, fontSize = 11.sp)
                if (item.genre.isNotBlank()) Text(item.genre, color = Accent, fontSize = 10.sp)
                if (item.synopsis.isNotBlank()) Text(item.synopsis, color = Muted, fontSize = 11.sp, maxLines = 2)
            }
            if (item.isWatchlisted) Icon(Icons.Default.Bookmark, contentDescription = "Saved", tint = Accent)
        }
    }
}

@Composable
private fun TitleDetailsDialog(
    item: CatalogTitle,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onWatchlist: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(item.title, color = White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${item.category.label} • ${item.language}", color = Accent, fontSize = 12.sp)
                Text(item.synopsis.ifBlank { "No description added." }, color = Muted)
                Text(if (item.playbackUrl.isBlank()) "Online link: not added" else "Online link: available", color = Muted, fontSize = 12.sp)
                Text(if (item.downloadUrl.isBlank()) "Download link: not added" else "Download link: available", color = Muted, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onPlay, enabled = item.playbackUrl.isNotBlank()) { Text("Play online") }
                TextButton(onClick = onDownload, enabled = item.downloadUrl.isNotBlank()) { Text("Download offline") }
                TextButton(onClick = onWatchlist) { Text(if (item.isWatchlisted) "Remove from saved" else "Add to watchlist") }
                TextButton(onClick = onEdit) { Text("Edit title") }
                TextButton(onClick = onDelete) { Text("Remove title") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun TitleEditorDialog(initial: CatalogTitle?, onDismiss: () -> Unit, onSave: (CatalogTitle) -> Unit) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: ContentCategory.MOVIE) }
    var language by remember(initial?.id) { mutableStateOf(initial?.language ?: "Hindi Dubbed") }
    var synopsis by remember(initial?.id) { mutableStateOf(initial?.synopsis.orEmpty()) }
    var genre by remember(initial?.id) { mutableStateOf(initial?.genre.orEmpty()) }
    var releaseYear by remember(initial?.id) { mutableStateOf(initial?.releaseYear.orEmpty()) }
    var posterUrl by remember(initial?.id) { mutableStateOf(initial?.posterUrl.orEmpty()) }
    var playback by remember(initial?.id) { mutableStateOf(initial?.playbackUrl.orEmpty()) }
    var download by remember(initial?.id) { mutableStateOf(initial?.downloadUrl.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(if (initial == null) "Add MovieBox title" else "Edit title", color = White) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(500.dp)) {
                item { OutlinedTextField(title, { title = it }, label = { Text("Movie / series / anime name") }, singleLine = true) }
                item { Text("Category", color = Muted) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ContentCategory.entries.forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label, fontSize = 10.sp) }) }
                    }
                }
                item { OutlinedTextField(language, { language = it }, label = { Text("Audio language") }, singleLine = true) }
                item { OutlinedTextField(genre, { genre = it }, label = { Text("Genres (e.g. Action, Comedy)") }, singleLine = true) }
                item { OutlinedTextField(releaseYear, { releaseYear = it }, label = { Text("Release year") }, singleLine = true) }
                item { OutlinedTextField(posterUrl, { posterUrl = it }, label = { Text("Poster image HTTPS URL (optional)") }, singleLine = true) }
                item { OutlinedTextField(synopsis, { synopsis = it }, label = { Text("Description") }, minLines = 2) }
                item { OutlinedTextField(playback, { playback = it }, label = { Text("Authorized HTTPS playback URL") }, singleLine = true) }
                item { OutlinedTextField(download, { download = it }, label = { Text("Authorized HTTPS download URL") }, singleLine = true) }
                item { Text("Only add links supplied by a provider that authorizes playback or downloading.", color = Muted, fontSize = 11.sp) }
                error?.let { item { Text(it, color = Color(0xFFFF7777), fontSize = 12.sp) } }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    title.isBlank() -> error = "Please enter a title."
                    (playback.isNotBlank() && !playback.startsWith("https://")) -> error = "Playback link must use HTTPS."
                    (download.isNotBlank() && !download.startsWith("https://")) -> error = "Download link must use HTTPS."
                    (posterUrl.isNotBlank() && !posterUrl.startsWith("https://")) -> error = "Poster URL must use HTTPS."
                    else -> onSave(CatalogTitle(initial?.id ?: UUID.randomUUID().toString(), title.trim(), category, language.trim().ifBlank { "Hindi Dubbed" }, synopsis.trim(), playback.trim(), download.trim(), initial?.isWatchlisted ?: false, genre.trim(), releaseYear.trim(), posterUrl.trim()))
                }
            }) { Text("Save title") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DownloadsScreen(records: List<SavedDownload>, repository: DownloadRepository, onRefresh: () -> Unit, onOpenLocal: () -> Unit) {
    var statuses by remember { mutableStateOf<Map<Long, DownloadStatus?>>(emptyMap()) }
    LaunchedEffect(records, records.size) {
        statuses = records.associate { it.id to repository.status(it.id) }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Downloads", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Your offline download queue", color = Muted, fontSize = 12.sp)
        if (records.isEmpty()) EmptyState("No downloads yet", "Open a title and choose Download offline, or browse a video already saved on your phone.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(records, key = { it.id }) { record ->
                val status = statuses[record.id]
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(record.title, color = White, fontWeight = FontWeight.SemiBold)
                        Text(status?.label() ?: "Checking status", color = Accent, fontSize = 12.sp)
                        if (status != null && status.totalBytes > 0 && status.status == DownloadManager.STATUS_RUNNING) {
                            val progress = (status.downloadedBytes.toFloat() / status.totalBytes).coerceIn(0f, 1f)
                            CircularProgressIndicator(progress = { progress }, color = Accent, trackColor = Color.DarkGray)
                            Text("${(progress * 100).toInt()}% downloaded", color = Muted, fontSize = 11.sp)
                        }
                        Text(record.fileName, color = Muted, fontSize = 10.sp)
                        TextButton(onClick = { repository.remove(record.id); onRefresh() }) { Text("Remove download") }
                    }
                }
            }
        }
        OutlinedButton(onClick = onOpenLocal, modifier = Modifier.fillMaxWidth()) { Text("Browse phone videos") }
    }
}

@Composable
private fun SettingsScreen(onOpenLocal: () -> Unit, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MovieBox", color = White, fontWeight = FontWeight.Bold)
                Text("AMOLED dark • Android phone • On-demand movies, anime and web series", color = Muted, fontSize = 12.sp)
                Text("Hindi-dubbed titles are prioritized. The catalogue is stored on this device.", color = Muted, fontSize = 12.sp)
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add a catalogue title") }
        OutlinedButton(onClick = onOpenLocal, modifier = Modifier.fillMaxWidth()) { Text("Open local video") }
        Text("No Live TV, IPTV, M3U, browser or WebView features.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyState(title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Movie, contentDescription = null, tint = Accent, modifier = Modifier.size(32.dp))
            Text(title, color = White, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MoviePlayer(media: VodMedia, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember(media.id, media.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(media.toMediaItem())
            prepare()
            playWhenReady = true
        }
    }
    androidx.compose.runtime.DisposableEffect(player) { onDispose { player.release() } }
    Column(Modifier.fillMaxSize().background(Black)) {
        Text("←  Back to MovieBox", color = White, modifier = Modifier.clickable(onClick = onClose).padding(16.dp))
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player } }, update = { it.player = player }, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

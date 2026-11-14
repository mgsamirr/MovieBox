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
import androidx.compose.material3.Switch
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.CaptionStyleCompat
import com.moviebox.mobile.data.CatalogStore
import com.moviebox.mobile.data.AppPreferencesStore
import com.moviebox.mobile.data.ParentalControlStore
import com.moviebox.mobile.data.CatalogTitle
import com.moviebox.mobile.data.ContentCategory
import com.moviebox.mobile.data.DownloadRepository
import com.moviebox.mobile.data.DownloadStatus
import com.moviebox.mobile.data.PlaybackSourceType
import com.moviebox.mobile.data.PlaybackProgressStore
import com.moviebox.mobile.data.SavedDownload
import com.moviebox.mobile.data.VodMedia
import com.moviebox.mobile.data.WatchHistoryStore
import com.moviebox.mobile.data.RecentTitle
import kotlinx.coroutines.delay

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

private enum class PinDialogMode { CREATE, VERIFY_TAB, VERIFY_FOR_CHANGE, VERIFY_FOR_DISABLE }

private enum class AppTab(val label: String) {
    HOME("Home"), SEARCH("Search"), WATCHLIST("Saved"), DOWNLOADS("Downloads"), SETTINGS("Settings")
}

@Composable
private fun MovieBoxApp() {
    val context = LocalContext.current
    val catalog = remember { CatalogStore(context) }
    val downloads = remember { DownloadRepository(context) }
    val history = remember { WatchHistoryStore(context) }
    val playbackProgress = remember { PlaybackProgressStore(context) }
    val parentalControls = remember { ParentalControlStore(context) }
    val appPreferences = remember { AppPreferencesStore(context) }
    var resumePlaybackEnabled by remember { mutableStateOf(appPreferences.resumePlayback()) }
    var showRecentlyWatched by remember { mutableStateOf(appPreferences.showRecentlyWatched()) }
    var parentalPinEnabled by remember { mutableStateOf(parentalControls.isEnabled()) }
    var pinDialogMode by remember { mutableStateOf<PinDialogMode?>(null) }
    var pinTargetTab by remember { mutableStateOf<AppTab?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var titles by remember { mutableStateOf(catalog.all()) }
    var queue by remember { mutableStateOf(downloads.all()) }
    var recent by remember { mutableStateOf(history.all(catalog)) }
    var refresh by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<ContentCategory?>(null) }
    var selectedTitle by remember { mutableStateOf<CatalogTitle?>(null) }
    var managingEpisodesFor by remember { mutableStateOf<CatalogTitle?>(null) }
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
                MoviePlayer(playerMedia!!, progressStore = playbackProgress, resumeEnabled = resumePlaybackEnabled, onClose = { playerMedia = null })
            } else {
                Scaffold(
                    containerColor = Black,
                    bottomBar = {
                        NavigationBar(containerColor = Panel) {
                            AppTab.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = tab == item,
                                    onClick = {
                                        if (parentalPinEnabled && item in listOf(AppTab.WATCHLIST, AppTab.DOWNLOADS, AppTab.SETTINGS)) {
                                            pinTargetTab = item
                                            pinInput = ""
                                            pinError = null
                                            pinDialogMode = PinDialogMode.VERIFY_TAB
                                        } else tab = item
                                    },
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
                                    showRecentlyWatched = showRecentlyWatched,
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
                                    onPlayDownloaded = { uri -> playerMedia = VodMedia("download-${uri.lastPathSegment}", "Offline download", uri, PlaybackSourceType.DOWNLOADED_FILE) },
                                    onOpenLocal = { localPicker.launch(arrayOf("video/*")) }
                                )
                                AppTab.SETTINGS -> SettingsScreen(
                                    onOpenLocal = { localPicker.launch(arrayOf("video/*")) },
                                    pinEnabled = parentalPinEnabled,
                                    onSetOrChangePin = {
                                        pinInput = ""
                                        pinError = null
                                        pinDialogMode = if (parentalPinEnabled) PinDialogMode.VERIFY_FOR_CHANGE else PinDialogMode.CREATE
                                    },
                                    onDisablePin = {
                                        pinInput = ""
                                        pinError = null
                                        pinDialogMode = PinDialogMode.VERIFY_FOR_DISABLE
                                    },
                                    resumePlayback = resumePlaybackEnabled,
                                    showRecentlyWatched = showRecentlyWatched,
                                    onResumePlaybackChange = { enabled ->
                                        resumePlaybackEnabled = enabled
                                        appPreferences.setResumePlayback(enabled)
                                    },
                                    onRecentlyWatchedChange = { enabled ->
                                        showRecentlyWatched = enabled
                                        appPreferences.setShowRecentlyWatched(enabled)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    pinDialogMode?.let { mode ->
        val dialogTitle = when (mode) {
            PinDialogMode.CREATE -> if (parentalPinEnabled) "Set new parental PIN" else "Create parental PIN"
            PinDialogMode.VERIFY_TAB -> "Parental PIN required"
            PinDialogMode.VERIFY_FOR_CHANGE -> "Confirm current PIN"
            PinDialogMode.VERIFY_FOR_DISABLE -> "Turn off parental PIN"
        }
        val dialogMessage = when (mode) {
            PinDialogMode.CREATE -> "Create a 4-digit PIN. It will protect Saved, Downloads and Settings."
            PinDialogMode.VERIFY_TAB -> "Enter your 4-digit PIN to open " + (pinTargetTab?.label ?: "this section") + "."
            PinDialogMode.VERIFY_FOR_CHANGE -> "Enter your current PIN before creating a new one."
            PinDialogMode.VERIFY_FOR_DISABLE -> "Enter your current PIN to turn off parental protection."
        }
        ParentalPinDialog(
            title = dialogTitle,
            message = dialogMessage,
            value = pinInput,
            error = pinError,
            confirmLabel = if (mode == PinDialogMode.CREATE) "Save PIN" else "Continue",
            onValueChange = { pinInput = it; pinError = null },
            onDismiss = { pinDialogMode = null; pinTargetTab = null },
            onConfirm = {
                when (mode) {
                    PinDialogMode.CREATE -> {
                        if (pinInput.length != 4) pinError = "Enter exactly 4 digits."
                        else {
                            parentalControls.setPin(pinInput)
                            parentalPinEnabled = true
                            pinDialogMode = null
                            pinError = null
                        }
                    }
                    PinDialogMode.VERIFY_TAB -> {
                        if (parentalControls.verifyPin(pinInput)) {
                            tab = pinTargetTab ?: AppTab.SETTINGS
                            pinDialogMode = null
                            pinTargetTab = null
                        } else pinError = "Incorrect PIN. Try again."
                    }
                    PinDialogMode.VERIFY_FOR_CHANGE -> {
                        if (parentalControls.verifyPin(pinInput)) {
                            pinInput = ""
                            pinError = null
                            pinDialogMode = PinDialogMode.CREATE
                        } else pinError = "Incorrect current PIN."
                    }
                    PinDialogMode.VERIFY_FOR_DISABLE -> {
                        if (parentalControls.verifyPin(pinInput)) {
                            parentalControls.clearPin()
                            parentalPinEnabled = false
                            pinDialogMode = null
                            pinError = null
                        } else pinError = "Incorrect current PIN."
                    }
                }
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
            onManageEpisodes = { managingEpisodesFor = item; selectedTitle = null },
        )
    }

    managingEpisodesFor?.let { series ->
        EpisodeManagementDialog(
            series = series,
            onDismiss = { managingEpisodesFor = null },
            onDownload = { episode ->
                runCatching { downloads.enqueue(episode.downloadUrl, "${series.title} • ${episode.title}", "${series.title}-S${episode.seasonNumber}E${episode.episodeNumber}.mp4") }
                    .onSuccess { reload(); notice = "Episode download queue mein add ho gaya." }
                    .onFailure { notice = it.message ?: "Episode download start nahi hua." }
            },
            onPlay = { episode ->
                val uri = Uri.parse(episode.playbackUrl)
                if (uri.scheme == "https" && !uri.host.isNullOrBlank()) {
                    playerMedia = VodMedia(episode.id, "${series.title} • ${episode.title}", uri, PlaybackSourceType.ONLINE_VOD)
                    managingEpisodesFor = null
                } else notice = "Is episode ka authorized HTTPS playback link add nahi hai."
            }
        )
    }
}

@Composable
private fun HomeScreen(
    titles: List<CatalogTitle>,
    recent: List<RecentTitle>,
    showRecentlyWatched: Boolean,
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
        if (showRecentlyWatched && recent.isNotEmpty()) {
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
    onManageEpisodes: () -> Unit
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
                if (item.category != ContentCategory.MOVIE) TextButton(onClick = onManageEpisodes) { Text("View seasons & episodes") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun DownloadsScreen(records: List<SavedDownload>, repository: DownloadRepository, onRefresh: () -> Unit, onPlayDownloaded: (Uri) -> Unit, onOpenLocal: () -> Unit) {
    var statuses by remember { mutableStateOf<Map<Long, DownloadStatus?>>(emptyMap()) }
    LaunchedEffect(records) {
        while (true) {
            statuses = records.associate { it.id to repository.status(it.id) }
            delay(1500)
        }
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
                        if (status?.status == DownloadManager.STATUS_SUCCESSFUL) {
                            TextButton(onClick = { repository.downloadedUri(record.id)?.let(onPlayDownloaded) }) { Text("Play offline") }
                        }
                        TextButton(onClick = { repository.remove(record.id); onRefresh() }) { Text("Delete offline download") }
                    }
                }
            }
        }
        OutlinedButton(onClick = onOpenLocal, modifier = Modifier.fillMaxWidth()) { Text("Browse phone videos") }
    }
}

@Composable
private fun SettingsScreen(
    onOpenLocal: () -> Unit,
    pinEnabled: Boolean,
    onSetOrChangePin: () -> Unit,
    onDisablePin: () -> Unit,
    resumePlayback: Boolean,
    showRecentlyWatched: Boolean,
    onResumePlaybackChange: (Boolean) -> Unit,
    onRecentlyWatchedChange: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MovieBox", color = White, fontWeight = FontWeight.Bold)
                Text("AMOLED dark • Android phone • On-demand movies, anime and web series", color = Muted, fontSize = 12.sp)
                Text("Hindi-dubbed titles are prioritized. The catalogue is stored on this device.", color = Muted, fontSize = 12.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("App Preferences", color = White, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Resume playback", color = White, fontSize = 14.sp)
                        Text("Continue videos from the last saved position.", color = Muted, fontSize = 11.sp)
                    }
                    Switch(checked = resumePlayback, onCheckedChange = onResumePlaybackChange)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recently watched row", color = White, fontSize = 14.sp)
                        Text("Show your recent titles on the Home screen.", color = Muted, fontSize = 11.sp)
                    }
                    Switch(checked = showRecentlyWatched, onCheckedChange = onRecentlyWatchedChange)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Parental Controls", color = White, fontWeight = FontWeight.Bold)
                Text(
                    if (pinEnabled) "PIN protection is on for Saved, Downloads and Settings."
                    else "Add a 4-digit PIN to protect Saved, Downloads and Settings.",
                    color = Muted,
                    fontSize = 12.sp
                )
                OutlinedButton(onClick = onSetOrChangePin, modifier = Modifier.fillMaxWidth()) {
                    Text(if (pinEnabled) "Change parental PIN" else "Set parental PIN")
                }
                if (pinEnabled) {
                    TextButton(onClick = onDisablePin) { Text("Turn off PIN protection") }
                }
            }
        }
        OutlinedButton(onClick = onOpenLocal, modifier = Modifier.fillMaxWidth()) { Text("Open local video") }
        Text("No Live TV, IPTV, M3U, browser or WebView features.", color = Muted, fontSize = 12.sp)
    }
}


@Composable
private fun ParentalPinDialog(
    title: String,
    message: String,
    value: String,
    error: String?,
    confirmLabel: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(title, color = White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message, color = Muted, fontSize = 13.sp)
                OutlinedTextField(
                    value = value,
                    onValueChange = { input -> onValueChange(input.filter(Char::isDigit).take(4)) },
                    label = { Text("4-digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
private fun MoviePlayer(media: VodMedia, progressStore: PlaybackProgressStore, resumeEnabled: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember(media.id, media.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(media.toMediaItem())
            val resumeAt = if (resumeEnabled) progressStore.positionMs(media.id) else 0L
            if (resumeAt > 0L) seekTo(resumeAt)
            prepare()
            playWhenReady = true
        }
    }
    var availableTracks by remember(player) { mutableStateOf(player.currentTracks) }
    var trackPickerType by remember { mutableStateOf<Int?>(null) }
    var subtitleMessage by remember { mutableStateOf<String?>(null) }
    var showSubtitleAppearance by remember { mutableStateOf(false) }
    val playerPrefs = remember { context.getSharedPreferences("moviebox_player_settings", android.content.Context.MODE_PRIVATE) }
    var subtitleTextSize by remember { mutableStateOf(playerPrefs.getFloat("subtitle_text_size", 0.055f)) }
    var subtitleDarkBackground by remember { mutableStateOf(playerPrefs.getBoolean("subtitle_dark_background", true)) }
    var subtitleView by remember { mutableStateOf<SubtitleView?>(null) }
    LaunchedEffect(subtitleView, subtitleTextSize, subtitleDarkBackground) {
        subtitleView?.setFractionalTextSize(subtitleTextSize, false)
        subtitleView?.setStyle(
            CaptionStyleCompat(
                android.graphics.Color.WHITE,
                if (subtitleDarkBackground) android.graphics.Color.argb(210, 0, 0, 0) else android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                android.graphics.Color.BLACK,
                null
            )
        )
    }
    val externalSubtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
            val mime = context.contentResolver.getType(uri)?.lowercase()
            val subtitleMime = when {
                extension == "srt" || mime == "application/x-subrip" || mime == "application/srt" -> MimeTypes.APPLICATION_SUBRIP
                extension == "vtt" || mime == "text/vtt" -> MimeTypes.TEXT_VTT
                else -> null
            }
            if (subtitleMime == null) {
                subtitleMessage = "Please select an .srt or .vtt subtitle file."
            } else {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val subtitle = MediaItem.SubtitleConfiguration.Builder(uri)
                    .setMimeType(subtitleMime)
                    .setLanguage("und")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                val position = player.currentPosition
                val wasPlaying = player.playWhenReady
                player.setMediaItem(media.toMediaItem().buildUpon().setSubtitleConfigurations(listOf(subtitle)).build(), position)
                player.prepare()
                player.playWhenReady = wasPlaying
                subtitleMessage = "External subtitle added."
            }
        }
    }
    androidx.compose.runtime.DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) { availableTracks = tracks }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player, media.id, resumeEnabled) {
        if (!resumeEnabled) progressStore.clear(media.id)
        while (true) {
            delay(5_000)
            if (resumeEnabled && player.isPlaying && player.currentPosition > 0L) {
                progressStore.save(media.id, player.currentPosition)
            }
            if (player.playbackState == Player.STATE_ENDED) {
                progressStore.clear(media.id)
                break
            }
        }
    }
    androidx.compose.runtime.DisposableEffect(player, media.id, resumeEnabled) {
        onDispose {
            if (resumeEnabled && player.currentPosition > 0L && player.playbackState != Player.STATE_ENDED) {
                progressStore.save(media.id, player.currentPosition)
            }
            player.release()
        }
    }
    Column(Modifier.fillMaxSize().background(Black)) {
        Text("←  Back to MovieBox", color = White, modifier = Modifier.clickable(onClick = onClose).padding(16.dp))
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true; subtitleView = this.subtitleView } },
            update = { view -> view.player = player; subtitleView = view.subtitleView },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { trackPickerType = C.TRACK_TYPE_AUDIO }, modifier = Modifier.weight(1f)) { Text("Audio track") }
            OutlinedButton(onClick = { trackPickerType = C.TRACK_TYPE_TEXT }, modifier = Modifier.weight(1f)) { Text("Subtitles") }
        }
        OutlinedButton(
            onClick = { externalSubtitlePicker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) { Text("Load external subtitle (.SRT / .VTT)") }
        OutlinedButton(
            onClick = { showSubtitleAppearance = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) { Text("Subtitle appearance") }
        subtitleMessage?.let { message ->
            Text(message, color = if (message.startsWith("External")) Accent else Muted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
    }
    if (showSubtitleAppearance) {
        SubtitleAppearanceDialog(
            textSize = subtitleTextSize,
            darkBackground = subtitleDarkBackground,
            onTextSizeChange = { value ->
                subtitleTextSize = value
                playerPrefs.edit().putFloat("subtitle_text_size", value).apply()
            },
            onBackgroundChange = { enabled ->
                subtitleDarkBackground = enabled
                playerPrefs.edit().putBoolean("subtitle_dark_background", enabled).apply()
            },
            onDismiss = { showSubtitleAppearance = false }
        )
    }
    trackPickerType?.let { type ->
        TrackPickerDialog(
            tracks = availableTracks,
            player = player,
            trackType = type,
            onDismiss = { trackPickerType = null }
        )
    }
}


@Composable
private fun SubtitleAppearanceDialog(
    textSize: Float,
    darkBackground: Boolean,
    onTextSizeChange: (Float) -> Unit,
    onBackgroundChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("Subtitle appearance", color = White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Text size", color = White, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = textSize,
                    onValueChange = onTextSizeChange,
                    valueRange = 0.035f..0.09f
                )
                Text(
                    when {
                        textSize < 0.048f -> "Small"
                        textSize > 0.072f -> "Large"
                        else -> "Medium"
                    },
                    color = Muted,
                    fontSize = 12.sp
                )
                Text("Subtitle background", color = White, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = darkBackground,
                        onClick = { onBackgroundChange(true) },
                        label = { Text("Dark") }
                    )
                    FilterChip(
                        selected = !darkBackground,
                        onClick = { onBackgroundChange(false) },
                        label = { Text("Transparent") }
                    )
                }
                Text("Changes are saved on this device.", color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun TrackPickerDialog(
    tracks: Tracks,
    player: ExoPlayer,
    trackType: Int,
    onDismiss: () -> Unit
) {
    val isSubtitle = trackType == C.TRACK_TYPE_TEXT
    val groups = tracks.groups.filter { it.type == trackType && it.isSupported }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(if (isSubtitle) "Subtitle track" else "Audio track", color = White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isSubtitle) {
                    TextButton(onClick = {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .clearOverridesOfType(trackType)
                            .setTrackTypeDisabled(trackType, true)
                            .build()
                        onDismiss()
                    }) { Text("Off  •  No subtitles") }
                }
                if (groups.isEmpty()) {
                    Text(if (isSubtitle) "No supported subtitle tracks in this video." else "No supported alternate audio tracks in this video.", color = Muted)
                } else {
                    groups.forEach { group ->
                        for (index in 0 until group.length) {
                            if (group.isTrackSupported(index)) {
                                val format = group.getTrackFormat(index)
                                val language = format.language?.takeIf { it.isNotBlank() && it != "und" }
                                val label = format.label?.takeIf { it.isNotBlank() }
                                    ?: language
                                    ?: format.codecs?.takeIf { it.isNotBlank() }
                                    ?: "Track ${index + 1}"
                                val selected = group.isTrackSelected(index)
                                TextButton(onClick = {
                                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                        .clearOverridesOfType(trackType)
                                        .setTrackTypeDisabled(trackType, false)
                                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index))
                                        .build()
                                    onDismiss()
                                }) {
                                    Text((if (selected) "✓  " else "") + label + (if (language != null && format.label != null) " • $language" else ""), color = if (selected) Accent else White)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

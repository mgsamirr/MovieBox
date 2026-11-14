package com.moviebox.mobile

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.moviebox.mobile.data.DirectDownload
import com.moviebox.mobile.data.PlaybackSourceType
import com.moviebox.mobile.data.VodMedia

private val AmoledBlack = Color(0xFF000000)
private val SurfaceBlack = Color(0xFF101014)
private val Accent = Color(0xFF8B5CF6)
private val Muted = Color(0xFF9696A3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MovieBoxApp() }
    }
}

private enum class Tab(val label: String) {
    HOME("Home"), MOVIES("Movies"), ANIME("Anime"), SERIES("Series"), DOWNLOADS("Downloads")
}

@Composable
private fun MovieBoxApp() {
    var selected by remember { mutableStateOf(Tab.HOME) }
    var mediaToPlay by remember { mutableStateOf<VodMedia?>(null) }
    var showOnlineDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var onlineUrl by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var downloadTitle by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            mediaToPlay = VodMedia(
                id = "local-${uri.lastPathSegment ?: "video"}",
                title = "Downloaded video",
                uri = uri,
                sourceType = PlaybackSourceType.DOWNLOADED_FILE
            )
        }
    }

    MaterialTheme {
        Surface(color = AmoledBlack, modifier = Modifier.fillMaxSize()) {
            if (mediaToPlay != null) {
                VodPlayer(media = mediaToPlay!!, onClose = { mediaToPlay = null })
            } else {
                Scaffold(
                    containerColor = AmoledBlack,
                    bottomBar = {
                        NavigationBar(containerColor = SurfaceBlack) {
                            Tab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selected == tab,
                                    onClick = { selected = tab },
                                    icon = {
                                        Icon(
                                            imageVector = when (tab) {
                                                Tab.DOWNLOADS -> Icons.Default.Download
                                                Tab.MOVIES, Tab.ANIME, Tab.SERIES -> Icons.Default.Movie
                                                else -> Icons.Default.Home
                                            },
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, fontSize = 9.sp) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding)
                            .background(AmoledBlack).padding(horizontal = 20.dp)
                    ) {
                        Spacer(Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("MOVIEBOX", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                Text("Stream online • Download • Watch offline", color = Muted, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.height(26.dp))
                        AnimatedContent(targetState = selected, label = "tab-content") { tab ->
                            when (tab) {
                                Tab.DOWNLOADS -> DownloadsScreen(onOpenLocal = { picker.launch(arrayOf("video/*")) })
                                Tab.HOME -> HomeScreen(
                                    onOpenLocal = { picker.launch(arrayOf("video/*")) },
                                    onPlayOnline = { showOnlineDialog = true },
                                    onDownload = { showDownloadDialog = true }
                                )
                                Tab.MOVIES -> CategoryScreen("Movies", { showOnlineDialog = true }, { showDownloadDialog = true })
                                Tab.ANIME -> CategoryScreen("Anime", { showOnlineDialog = true }, { showDownloadDialog = true })
                                Tab.SERIES -> CategoryScreen("Web Series", { showOnlineDialog = true }, { showDownloadDialog = true })
                            }
                        }
                        message?.let { text ->
                            Text(text, color = Accent, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showOnlineDialog) {
        AlertDialog(
            onDismissRequest = { showOnlineDialog = false },
            containerColor = SurfaceBlack,
            title = { Text("Play online", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste an authorized HTTPS on-demand video URL. Live streams are not supported.", color = Muted, fontSize = 12.sp)
                    OutlinedTextField(
                        value = onlineUrl,
                        onValueChange = { onlineUrl = it },
                        label = { Text("HTTPS video URL") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val uri = Uri.parse(onlineUrl.trim())
                    if (uri.scheme == "https" && !uri.host.isNullOrBlank()) {
                        mediaToPlay = VodMedia(
                            id = "online-${uri.lastPathSegment ?: "vod"}",
                            title = "Online video",
                            uri = uri,
                            sourceType = PlaybackSourceType.ONLINE_VOD
                        )
                        message = null
                        showOnlineDialog = false
                        onlineUrl = ""
                    } else {
                        message = "Please enter a valid HTTPS video URL."
                    }
                }) { Text("Play") }
            },
            dismissButton = { TextButton(onClick = { showOnlineDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            containerColor = SurfaceBlack,
            title = { Text("Download for offline", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use a direct HTTPS download link supplied by an authorized content source.", color = Muted, fontSize = 12.sp)
                    OutlinedTextField(value = downloadTitle, onValueChange = { downloadTitle = it }, label = { Text("Movie / episode name") }, singleLine = true)
                    OutlinedTextField(value = downloadUrl, onValueChange = { downloadUrl = it }, label = { Text("Direct HTTPS download URL") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    runCatching {
                        DirectDownload(context).enqueue(
                            url = downloadUrl.trim(),
                            fileName = Uri.parse(downloadUrl.trim()).lastPathSegment ?: "movie-download.mp4",
                            title = downloadTitle.trim().ifBlank { "MovieBox download" }
                        )
                    }.onSuccess {
                        message = "Download started. Android will show its progress notification."
                        showDownloadDialog = false
                        downloadUrl = ""
                        downloadTitle = ""
                    }.onFailure {
                        message = it.message ?: "Could not start download."
                    }
                }) { Text("Start download") }
            },
            dismissButton = { TextButton(onClick = { showDownloadDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HomeScreen(
    onOpenLocal: () -> Unit,
    onPlayOnline: () -> Unit,
    onDownload: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(190.dp)
                .background(SurfaceBlack, RoundedCornerShape(24.dp)).padding(20.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Text("YOUR ENTERTAINMENT SPACE", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("Stream it now.\nDownload for later.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
            }
        }
        Text("Watch your way", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Movies, anime and web series. Hindi-dubbed discovery is the priority.", color = Muted, fontSize = 13.sp)
        ActionCard("Play an online movie or episode", "Play an authorized on-demand HTTPS video", Icons.Default.PlayArrow, onPlayOnline)
        ActionCard("Download for offline", "Save a direct-download video to this phone", Icons.Default.Download, onDownload)
        ActionCard("Open a downloaded video", "Choose a video already saved on your phone", Icons.Default.Movie, onOpenLocal)
    }
}

@Composable
private fun DownloadsScreen(onOpenLocal: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Downloads", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Offline videos saved on this phone", color = Muted, fontSize = 13.sp)
        Box(
            modifier = Modifier.fillMaxWidth().height(150.dp)
                .background(SurfaceBlack, RoundedCornerShape(20.dp)).padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Accent, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(10.dp))
                Text("Choose a saved video to play", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("The Android file picker opens your local videos.", color = Muted, fontSize = 12.sp)
            }
        }
        ActionCard("Browse offline videos", "Select a downloaded movie or episode", Icons.Default.Movie, onOpenLocal)
    }
}

@Composable
private fun CategoryScreen(name: String, onPlayOnline: () -> Unit, onDownload: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(name, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("On-demand titles only • Hindi dubbed preferred", color = Muted, fontSize = 13.sp)
        Text("When a provider supplies an authorized title link, play it online or download it for offline viewing.", color = Muted, fontSize = 13.sp)
        ActionCard("Play an online title", "Paste an authorized HTTPS VOD link", Icons.Default.PlayArrow, onPlayOnline)
        ActionCard("Download a title", "Paste an authorized direct-download link", Icons.Default.Download, onDownload)
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceBlack, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(30.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun VodPlayer(media: VodMedia, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember(media.id, media.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(media.toMediaItem())
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Text("←  Back to MovieBox", color = Color.White, modifier = Modifier.clickable(onClick = onClose).padding(16.dp))
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
            update = { it.player = player },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

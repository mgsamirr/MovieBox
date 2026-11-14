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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView

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
    var localVideo by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) localVideo = uri
    }

    MaterialTheme {
        Surface(color = AmoledBlack, modifier = Modifier.fillMaxSize()) {
            if (localVideo != null) {
                VodPlayer(uri = localVideo!!, isOnline = false, onClose = { localVideo = null })
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
                                Tab.HOME -> HomeScreen(onOpenLocal = { picker.launch(arrayOf("video/*")) })
                                Tab.MOVIES -> EmptyCategory("Movies")
                                Tab.ANIME -> EmptyCategory("Anime")
                                Tab.SERIES -> EmptyCategory("Web Series")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onOpenLocal: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(190.dp)
                .background(SurfaceBlack, RoundedCornerShape(24.dp)).padding(20.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Text("WATCH YOUR WAY", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("Stream it now.\nDownload for later.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
            }
        }
        Text("Your library", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Play available movies and episodes online, or download them to watch offline.", color = Muted, fontSize = 13.sp)
        OpenFileCard(onClick = onOpenLocal)
    }
}

@Composable
private fun DownloadsScreen(onOpenLocal: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Downloads", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Your offline collection", color = Muted, fontSize = 13.sp)
        Box(
            modifier = Modifier.fillMaxWidth().height(170.dp)
                .background(SurfaceBlack, RoundedCornerShape(20.dp)).padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Accent, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(10.dp))
                Text("No downloads yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Downloaded videos will appear here.", color = Muted, fontSize = 12.sp)
            }
        }
        OpenFileCard(onClick = onOpenLocal)
    }
}

@Composable
private fun OpenFileCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceBlack, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Accent, modifier = Modifier.size(30.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Play a downloaded video", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Choose an MP4, MKV, or other local video file", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EmptyCategory(name: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(name, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Choose an available title to stream online or download for offline viewing.", color = Muted, fontSize = 13.sp)
        Text("On-demand movies and episodes only. No Live TV or live channels.", color = Accent, fontSize = 12.sp)
    }
}

@Composable
private fun VodPlayer(uri: Uri, isOnline: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.Builder().setUri(uri).setMediaId(if (isOnline) "online-vod" else "downloaded-vod").build())
            prepare()
            playWhenReady = true
        }
    }
    androidx.compose.runtime.DisposableEffect(player) {
        onDispose { player.release() }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Text("←  Back to MovieBox", color = Color.White, modifier = Modifier.clickable(onClick = onClose).padding(16.dp))
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

package com.mgsamirr.moviebox

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView

/**
 * Plays a video the user explicitly selects through Android's document picker.
 * SAF grants access to that URI only; the app requests no broad storage access.
 */
@Composable
fun LocalPlaybackScreen(
    uri: Uri,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var playbackMessage by remember(uri) { mutableStateOf("Checking the file's Hindi audio track…") }
    val trackSelector = remember(uri) {
        DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setPreferredAudioLanguage("hi")
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
        }
    }
    val player = remember(uri) {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true
                )
                setMediaItem(MediaItem.fromUri(uri))
                playWhenReady = false
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                playbackMessage = when (Media3HindiAudioPolicy.apply(trackSelector, tracks)) {
                    true -> "Verified Hindi audio track selected."
                    false -> "No supported Hindi audio track was found. Audio is disabled."
                    null -> "Checking the file's Hindi audio track…"
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackMessage = "This file could not be played by the device's media decoder."
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        player.prepare()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("← BACK", color = Color(0xFFE8C477)) }
        Text("Local video", style = MaterialTheme.typography.titleLarge, color = Color.White)
        AndroidView(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    setShowSubtitleButton(true)
                }
            },
            update = { it.player = player }
        )
        Text(playbackMessage, color = Color(0xFF929292), style = MaterialTheme.typography.bodySmall)
    }
}

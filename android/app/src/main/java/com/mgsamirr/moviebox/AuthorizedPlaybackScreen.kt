package com.mgsamirr.moviebox

import android.content.Context
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Native Media3 playback surface. A provider must supply the media and license
 * endpoints; this screen never resolves or manufactures either URL.
 */
@Composable
fun AuthorizedPlaybackScreen(
    config: AuthorizedDrmPlaybackConfig,
    onBack: () -> Unit,
    onProgress: (ResumePoint) -> Unit = {},
    onCompleted: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val resumeStore = remember(context) {
        ContinueWatchingStore(context.getSharedPreferences("movie_box_resume", Context.MODE_PRIVATE))
    }
    val result = remember(config) { AuthorizedDrmMediaItemFactory.create(config) }
    var playbackError by remember(config) { mutableStateOf<String?>(null) }
    var resumeApplied by remember(config) { mutableStateOf(false) }
    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnCompleted by rememberUpdatedState(onCompleted)

    val readyItem = (result as? DrmPlaybackResult.Ready)?.mediaItem
    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setPreferredAudioLanguage("hi")
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                .build()
        }
    }
    val player = remember(config, readyItem) {
        readyItem?.let {
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
                    setMediaItem(it)
                    playWhenReady = false
                }
        }
    }

    DisposableEffect(player, config.title.id) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val hindiTrackState = Media3HindiAudioPolicy.apply(trackSelector, tracks)
                if (hindiTrackState != true) player?.pause()
                playbackError = when (hindiTrackState) {
                    true -> null
                    false -> "Hindi audio is not present in this stream. Playback is disabled."
                    null -> if (player?.playbackState == Player.STATE_READY) {
                        "No Hindi audio track metadata was exposed. Playback is disabled."
                    } else null
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !resumeApplied) {
                    val resumePoint = resumeStore.load().resumePoints[config.title.id]
                    val duration = player?.duration ?: 0L
                    if (resumePoint != null && duration > resumePoint.positionMs) {
                        player?.seekTo(resumePoint.positionMs)
                    }
                    resumeApplied = true
                }
                if (playbackState == Player.STATE_ENDED) {
                    resumeStore.remove(config.title.id)
                    currentOnCompleted(config.title.id)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = "Playback could not start. Check the provider stream, license access, and network."
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.let { activePlayer ->
                val duration = activePlayer.duration
                val position = activePlayer.currentPosition
                if (activePlayer.playbackState != Player.STATE_ENDED && duration > 0L && position in 1 until duration) {
                    val point = ResumePoint(config.title.id, position, duration, System.currentTimeMillis())
                    resumeStore.record(point)
                    currentOnProgress(point)
                }
                activePlayer.removeListener(listener)
                activePlayer.release()
            }
        }
    }

    LaunchedEffect(player, config.title.id) {
        val activePlayer = player ?: return@LaunchedEffect
        activePlayer.prepare()
        while (true) {
            delay(10_000L)
            val duration = activePlayer.duration
            val position = activePlayer.currentPosition
            if (activePlayer.isPlaying && duration > 0L && position in 1 until duration) {
                val point = ResumePoint(config.title.id, position, duration, System.currentTimeMillis())
                resumeStore.record(point)
                currentOnProgress(point)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("← BACK", color = Color(0xFFE8C477)) }
        Text(config.title.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
        when (result) {
            is DrmPlaybackResult.Rejected -> Text(result.reason, color = Color(0xFFFF8A80))
            is DrmPlaybackResult.Ready -> {
                if (player != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                this.player = player
                                setShowSubtitleButton(false)
                            }
                        },
                        update = { it.player = player }
                    )
                    playbackError?.let { Text(it, color = Color(0xFFFF8A80)) }
                    Text(
                        "Hindi audio preference is enabled. The provider must supply accurate Hindi track metadata.",
                        color = Color(0xFF929292),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text("Player could not be initialized.", color = Color(0xFFFF8A80))
                }
            }
        }
    }
}

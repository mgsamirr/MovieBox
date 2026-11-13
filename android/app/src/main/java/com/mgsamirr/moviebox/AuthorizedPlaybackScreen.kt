package com.mgsamirr.moviebox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView

/**
 * Native Media3 playback surface. A provider must supply the media and license
 * endpoints; this screen never resolves or manufactures either URL.
 */
@Composable
fun AuthorizedPlaybackScreen(
    config: AuthorizedDrmPlaybackConfig,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val result = remember(config) { AuthorizedDrmMediaItemFactory.create(config) }
    var playbackError by remember(config) { mutableStateOf<String?>(null) }

    val readyItem = (result as? DrmPlaybackResult.Ready)?.mediaItem
    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setPreferredAudioLanguage("hi")
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
        }
    }
    val player = remember(readyItem) {
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

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                val hindiTrack = tracks.groups.asSequence()
                    .filter { it.type == C.TRACK_TYPE_AUDIO }
                    .flatMap { group ->
                        (0 until group.length).asSequence()
                            .filter { index ->
                                group.isTrackSupported(index) &&
                                    HindiTrackSelector.isHindiLanguage(group.getTrackFormat(index).language)
                            }
                            .map { index -> group to index }
                    }
                    .firstOrNull()

                val parameters = trackSelector.buildUponParameters()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, hindiTrack == null)
                if (hindiTrack != null) {
                    parameters.addOverride(
                        TrackSelectionOverride(hindiTrack.first.mediaTrackGroup, listOf(hindiTrack.second))
                    )
                    playbackError = null
                } else {
                    playbackError = "Hindi audio is not present in this stream. Audio has been disabled."
                }
                trackSelector.setParameters(parameters)
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = "Playback could not start. Check the provider stream, license access, and network."
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
            player?.release()
        }
    }

    LaunchedEffect(player) {
        player?.prepare()
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

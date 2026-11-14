package com.moviebox.mobile.data

import android.net.Uri
import androidx.media3.common.MediaItem

/**
 * A catalogue item is on-demand content, never a live channel.
 * The same item can be played from its HTTPS VOD URL or from a downloaded file.
 */
enum class PlaybackSourceType { ONLINE_VOD, DOWNLOADED_FILE }

data class VodMedia(
    val id: String,
    val title: String,
    val uri: Uri,
    val sourceType: PlaybackSourceType,
    val isLive: Boolean = false
) {
    fun toMediaItem(): MediaItem {
        require(!isLive) { "Live channels are not supported in MovieBox." }
        when (sourceType) {
            PlaybackSourceType.ONLINE_VOD ->
                require(uri.scheme == "https" && uri.host != null) {
                    "Online on-demand playback requires a valid HTTPS media URL."
                }
            PlaybackSourceType.DOWNLOADED_FILE ->
                require(uri.scheme == "content" || uri.scheme == "file") {
                    "Offline playback must use a local file URI."
                }
        }
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .build()
    }
}

package com.mgsamirr.moviebox

import android.net.Uri
import java.net.URI
import androidx.media3.common.C
import androidx.media3.common.MediaItem

/**
 * DRM settings supplied by an authorized content provider.
 *
 * The license endpoint and media URL are provider-owned. This app does not
 * discover, scrape, proxy, decrypt, or bypass DRM licenses.
 */
data class AuthorizedDrmPlaybackConfig(
    val mediaUrl: String,
    val licenseUrl: String,
    val title: CatalogTitle,
    val drmScheme: DrmScheme = DrmScheme.WIDEVINE
)

enum class DrmScheme { WIDEVINE }

sealed interface DrmPlaybackResult {
    data class Ready(val mediaItem: MediaItem) : DrmPlaybackResult
    data class Rejected(val reason: String) : DrmPlaybackResult
}

/**
 * Builds a Media3 MediaItem only when provider authorization and verified Hindi
 * audio are present. No background license-renewal or app auto-update task is
 * configured by Movie Box; playback uses the provider's normal DRM session flow.
 */
object AuthorizedDrmMediaItemFactory {
    fun create(config: AuthorizedDrmPlaybackConfig): DrmPlaybackResult {
        if (!config.title.fromAuthorizedProvider) {
            return DrmPlaybackResult.Rejected("Content provider is not authorized")
        }
        if (HindiTrackSelector.selectVerifiedHindiTrack(config.title) == null) {
            return DrmPlaybackResult.Rejected("Verified Hindi audio is unavailable")
        }
        if (!isHttps(config.mediaUrl) || !isHttps(config.licenseUrl)) {
            return DrmPlaybackResult.Rejected("Provider media and license URLs must use HTTPS")
        }
        if (config.drmScheme != DrmScheme.WIDEVINE) {
            return DrmPlaybackResult.Rejected("Unsupported DRM scheme")
        }

        val drm = MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
            .setLicenseUri(Uri.parse(config.licenseUrl))
            .setMultiSession(false)
            .build()

        val item = MediaItem.Builder()
            .setUri(Uri.parse(config.mediaUrl))
            .setDrmConfiguration(drm)
            .build()
        return DrmPlaybackResult.Ready(item)
    }

    internal fun isHttps(value: String): Boolean =
        value.length <= 2048 &&
            runCatching {
                val uri = URI(value)
                uri.scheme.equals("https", ignoreCase = true) &&
                    !uri.host.isNullOrBlank() &&
                    uri.userInfo == null &&
                    uri.fragment == null
            }.getOrDefault(false)
}

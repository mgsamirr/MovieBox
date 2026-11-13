package com.mgsamirr.moviebox

/**
 * Selects a Hindi audio track only when provider metadata confirms it is available.
 * No default-language fallback is permitted by the app's Hindi-only policy.
 */
object HindiTrackSelector {
    fun selectVerifiedHindiTrack(title: CatalogTitle): AudioTrack? {
        if (!title.fromAuthorizedProvider) return null
        return title.audioTracks.firstOrNull { track ->
            val language = track.languageCode.trim().lowercase()
                .substringBefore('-').substringBefore('_')
            track.available && track.verified && language in setOf("hi", "hin", "hindi")
        }
    }
}

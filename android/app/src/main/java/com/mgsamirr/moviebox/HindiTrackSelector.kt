package com.mgsamirr.moviebox

/**
 * Selects Hindi audio only when provider metadata confirms it is available.
 * The player also checks the actual manifest tracks and mutes audio when Hindi
 * is not present, rather than falling back to another language.
 */
object HindiTrackSelector {
    private val hindiLanguageCodes = setOf("hi", "hin", "hindi")

    fun isHindiLanguage(languageCode: String?): Boolean {
        if (languageCode.isNullOrBlank()) return false
        val primaryLanguage = languageCode.trim().lowercase()
            .substringBefore('-').substringBefore('_')
        return primaryLanguage in hindiLanguageCodes
    }

    fun selectVerifiedHindiTrack(title: CatalogTitle): AudioTrack? {
        if (!title.fromAuthorizedProvider) return null
        return title.audioTracks.firstOrNull { track ->
            track.available && track.verified && isHindiLanguage(track.languageCode)
        }
    }
}

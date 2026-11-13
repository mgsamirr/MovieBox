package com.mgsamirr.moviebox

enum class CatalogCategory(val label: String) {
    MOVIES("Movies"),
    SERIES("Web Series"),
    ANIME("Anime")
}

data class AudioTrack(
    val languageCode: String,
    val available: Boolean
)

data class CatalogTitle(
    val id: String,
    val name: String,
    val category: CatalogCategory,
    val audioTracks: List<AudioTrack>
)

object HindiAudioPolicy {
    private val hindiLanguageCodes = setOf("hi", "hin", "hindi")

    fun hasVerifiedHindiAudio(title: CatalogTitle): Boolean =
        title.audioTracks.any { track ->
            track.available && track.languageCode.trim().lowercase() in hindiLanguageCodes
        }

    fun filterVerifiedHindiTitles(titles: List<CatalogTitle>): List<CatalogTitle> =
        titles.filter(::hasVerifiedHindiAudio)
}

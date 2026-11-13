package com.mgsamirr.moviebox

enum class CatalogCategory(val label: String) {
    MOVIES("Movies"),
    SERIES("Web Series"),
    ANIME("Anime")
}

/**
 * Audio metadata must come from the connected provider. A track is display-eligible
 * only when the provider confirms both availability and verification.
 */
data class AudioTrack(
    val languageCode: String,
    val available: Boolean,
    val verified: Boolean = false
)

data class CatalogTitle(
    val id: String,
    val name: String,
    val category: CatalogCategory,
    val audioTracks: List<AudioTrack>,
    /** True only when the catalog provider has confirmed its distribution rights. */
    val fromAuthorizedProvider: Boolean = false,
    val description: String = "",
    val releaseYear: Int? = null,
    val durationMinutes: Int? = null,
    val posterUrl: String? = null
) {
    val safeDescription: String
        get() = description.trim().take(1200)

    val safePosterUrl: String?
        get() = posterUrl?.trim()?.takeIf(::isSafePosterUrl)

    val validReleaseYear: Int?
        get() = releaseYear?.takeIf { it in 1888..2100 }

    val validDurationMinutes: Int?
        get() = durationMinutes?.takeIf { it in 1..1440 }

    private fun isSafePosterUrl(value: String): Boolean =
        value.length <= 2048 && runCatching {
            val uri = java.net.URI(value)
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null
        }.getOrDefault(false)
}

object HindiAudioPolicy {
    private val hindiLanguageCodes = setOf("hi", "hin", "hindi")

    fun hasVerifiedHindiAudio(title: CatalogTitle): Boolean =
        title.fromAuthorizedProvider && title.audioTracks.any { track ->
            val primaryLanguage = track.languageCode
                .trim()
                .lowercase()
                .substringBefore('-')
                .substringBefore('_')
            track.available && track.verified && primaryLanguage in hindiLanguageCodes
        }

    fun filterVerifiedHindiTitles(titles: List<CatalogTitle>): List<CatalogTitle> =
        titles.filter(::hasVerifiedHindiAudio)
}

package com.mgsamirr.moviebox

/**
 * Adapter boundary for a catalog service that the app owner is authorized to use.
 *
 * A provider must supply rights and audio metadata from its own trusted service.
 * The Android client never guesses Hindi availability from a title or filename.
 */
interface AuthorizedCatalogSource {
    suspend fun loadTitles(): List<CatalogTitle>
}

/** Safe default until a real, licensed provider is configured. */
object EmptyCatalogSource : AuthorizedCatalogSource {
    override suspend fun loadTitles(): List<CatalogTitle> = emptyList()
}

/**
 * Centralizes catalog eligibility so every screen uses the same fail-closed policy.
 */
class CatalogRepository(
    private val source: AuthorizedCatalogSource = EmptyCatalogSource
) {
    suspend fun getHindiReadyTitles(): List<CatalogTitle> =
        HindiAudioPolicy.filterVerifiedHindiTitles(source.loadTitles())
}

/**
 * Playback is allowed only for an eligible title and an explicitly selected Hindi track.
 * Stream URLs and provider credentials remain outside the UI model.
 */
object PlaybackEligibility {
    fun canStartHindiPlayback(title: CatalogTitle): Boolean =
        HindiAudioPolicy.hasVerifiedHindiAudio(title)
}

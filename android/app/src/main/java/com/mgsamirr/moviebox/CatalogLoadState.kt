package com.mgsamirr.moviebox

/**
 * UI-facing catalog state. Provider errors are represented without exposing
 * credentials, request URLs, or raw server responses to the interface.
 */
sealed interface CatalogLoadState {
    data object Loading : CatalogLoadState
    data class Ready(val titles: List<CatalogTitle>) : CatalogLoadState
    data object ProviderNotConfigured : CatalogLoadState
    data class Failed(val message: String) : CatalogLoadState
}

object CatalogLoadReducer {
    fun ready(titles: List<CatalogTitle>): CatalogLoadState =
        CatalogLoadState.Ready(HindiAudioPolicy.filterVerifiedHindiTitles(titles))

    /**
     * Do not surface exception messages: HTTP/client exceptions can contain
     * signed URLs, query tokens, or provider response fragments.
     */
    fun failed(@Suppress("UNUSED_PARAMETER") message: String?): CatalogLoadState =
        CatalogLoadState.Failed("The catalog could not be loaded. Please try again.")
}

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

    fun failed(message: String?): CatalogLoadState =
        CatalogLoadState.Failed(
            message?.trim()?.takeIf { it.isNotEmpty() }?.take(160)
                ?: "The catalog could not be loaded. Please try again."
        )
}

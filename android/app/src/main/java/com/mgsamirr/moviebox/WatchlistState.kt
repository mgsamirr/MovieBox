package com.mgsamirr.moviebox

/**
 * Small immutable watchlist model. Persistence is deliberately kept behind the
 * Android UI boundary so the policy can be tested without Android framework mocks.
 */
data class WatchlistState private constructor(val titleIds: Set<String>) {
    fun contains(titleId: String): Boolean = titleId in titleIds

    fun toggle(titleId: String): WatchlistState {
        if (titleId.isBlank()) return this
        return copy(titleIds = if (contains(titleId)) titleIds - titleId else titleIds + titleId)
    }

    fun filter(titles: List<CatalogTitle>): List<CatalogTitle> =
        titles.filter { contains(it.id) && HindiAudioPolicy.hasVerifiedHindiAudio(it) }

    companion object {
        fun empty(): WatchlistState = WatchlistState(emptySet())
        fun fromIds(ids: Set<String>): WatchlistState =
            WatchlistState(ids.filter(String::isNotBlank).toSet())
    }
}

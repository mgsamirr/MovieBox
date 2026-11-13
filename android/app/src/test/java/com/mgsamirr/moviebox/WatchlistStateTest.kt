package com.mgsamirr.moviebox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchlistStateTest {
    private fun title(id: String, verified: Boolean = true) = CatalogTitle(
        id = id,
        name = id,
        category = CatalogCategory.MOVIES,
        audioTracks = listOf(AudioTrack("hi", available = true, verified = verified)),
        fromAuthorizedProvider = true
    )

    @Test fun toggleAddsAndRemovesIds() {
        val added = WatchlistState.empty().toggle("movie-1")
        assertTrue(added.contains("movie-1"))
        assertFalse(added.toggle("movie-1").contains("movie-1"))
    }

    @Test fun blankIdsAreIgnored() {
        assertEquals(WatchlistState.empty(), WatchlistState.empty().toggle(" "))
        assertEquals(emptySet<String>(), WatchlistState.fromIds(setOf("", " ")).titleIds)
    }

    @Test fun filterKeepsOnlySavedHindiVerifiedAuthorizedTitles() {
        val state = WatchlistState.fromIds(setOf("good", "unverified"))
        val result = state.filter(listOf(title("good"), title("unverified", false), title("not-saved")))
        assertEquals(listOf("good"), result.map { it.id })
    }
}

package com.mgsamirr.moviebox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogLoadReducerTest {
    private val verifiedHindi = CatalogTitle(
        id = "licensed-1",
        name = "Licensed title",
        category = CatalogCategory.MOVIES,
        audioTracks = listOf(AudioTrack("hi-IN", available = true, verified = true)),
        fromAuthorizedProvider = true
    )
    private val unverified = verifiedHindi.copy(
        id = "unknown-2",
        name = "Unverified title",
        audioTracks = listOf(AudioTrack("hi", available = true, verified = false))
    )

    @Test
    fun readyStateContainsOnlyAuthorizedVerifiedHindiTitles() {
        val state = CatalogLoadReducer.ready(listOf(verifiedHindi, unverified))
        assertTrue(state is CatalogLoadState.Ready)
        assertEquals(listOf(verifiedHindi), (state as CatalogLoadState.Ready).titles)
    }

    @Test
    fun failedStateUsesSafeFallbackForBlankMessage() {
        assertEquals(
            "The catalog could not be loaded. Please try again.",
            (CatalogLoadReducer.failed("  ") as CatalogLoadState.Failed).message
        )
    }

    @Test
    fun failedStateNeverLeaksProviderDetails() {
        val state = CatalogLoadReducer.failed("https://provider.example/?token=secret") as CatalogLoadState.Failed
        assertEquals("The catalog could not be loaded. Please try again.", state.message)
    }
}

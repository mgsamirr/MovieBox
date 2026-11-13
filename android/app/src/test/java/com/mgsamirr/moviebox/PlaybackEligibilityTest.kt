package com.mgsamirr.moviebox

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackEligibilityTest {
    @Test
    fun allowsOnlyAuthorizedVerifiedHindiPlayback() {
        val eligible = title(
            language = "hi-IN",
            available = true,
            verified = true,
            authorized = true
        )
        assertTrue(PlaybackEligibility.canStartHindiPlayback(eligible))
    }

    @Test
    fun blocksUnverifiedUnavailableAndUnauthorizedPlayback() {
        assertFalse(PlaybackEligibility.canStartHindiPlayback(title(
            language = "hi", available = true, verified = false, authorized = true
        )))
        assertFalse(PlaybackEligibility.canStartHindiPlayback(title(
            language = "hi", available = false, verified = true, authorized = true
        )))
        assertFalse(PlaybackEligibility.canStartHindiPlayback(title(
            language = "hi", available = true, verified = true, authorized = false
        )))
    }

    private fun title(
        language: String,
        available: Boolean,
        verified: Boolean,
        authorized: Boolean
    ) = CatalogTitle(
        id = "test",
        name = "Test",
        category = CatalogCategory.MOVIES,
        audioTracks = listOf(AudioTrack(language, available, verified)),
        fromAuthorizedProvider = authorized
    )
}

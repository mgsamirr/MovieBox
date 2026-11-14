package com.mgsamirr.moviebox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HindiTrackSelectorTest {
    @Test fun selectsOnlyVerifiedAvailableHindiTrack() {
        val title = CatalogTitle(
            id = "movie",
            name = "Movie",
            category = CatalogCategory.MOVIES,
            audioTracks = listOf(
                AudioTrack("en", available = true, verified = true),
                AudioTrack("hi-IN", available = true, verified = true)
            ),
            fromAuthorizedProvider = true
        )
        assertEquals("hi-IN", HindiTrackSelector.selectVerifiedHindiTrack(title)?.languageCode)
    }

    @Test fun doesNotFallBackToEnglishOrUnverifiedHindi() {
        val title = CatalogTitle(
            id = "movie",
            name = "Movie",
            category = CatalogCategory.MOVIES,
            audioTracks = listOf(
                AudioTrack("en", available = true, verified = true),
                AudioTrack("hi", available = true, verified = false)
            ),
            fromAuthorizedProvider = true
        )
        assertNull(HindiTrackSelector.selectVerifiedHindiTrack(title))
    }

    @Test fun recognizesHindiManifestLanguageTagsOnly() {
        assertEquals(true, HindiTrackSelector.isHindiLanguage("hi"))
        assertEquals(true, HindiTrackSelector.isHindiLanguage("hi-IN"))
        assertEquals(true, HindiTrackSelector.isHindiLanguage("hin"))
        assertEquals(false, HindiTrackSelector.isHindiLanguage("en-US"))
        assertEquals(false, HindiTrackSelector.isHindiLanguage("und"))
        assertEquals(false, HindiTrackSelector.isHindiLanguage(null))
    }

    @Test fun rejectsUnauthorizedProviderMetadata() {
        val title = CatalogTitle(
            id = "movie",
            name = "Movie",
            category = CatalogCategory.MOVIES,
            audioTracks = listOf(AudioTrack("hi", available = true, verified = true)),
            fromAuthorizedProvider = false
        )
        assertNull(HindiTrackSelector.selectVerifiedHindiTrack(title))
    }
}

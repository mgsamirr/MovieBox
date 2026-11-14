package com.mgsamirr.moviebox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HindiAudioPolicyTest {
    @Test
    fun acceptsVerifiedHindiLanguageCodesAndLocaleTags() {
        listOf("hi", "HIN", " Hindi ", "hi-IN", "hin_IN").forEach { code ->
            assertTrue(HindiAudioPolicy.hasVerifiedHindiAudio(titleWith(
                AudioTrack(code, available = true, verified = true)
            )))
        }
    }

    @Test
    fun rejectsUnavailableHindiTrack() {
        val title = titleWith(AudioTrack("hi", available = false, verified = true))
        assertFalse(HindiAudioPolicy.hasVerifiedHindiAudio(title))
    }

    @Test
    fun rejectsUnverifiedHindiTrack() {
        val title = titleWith(AudioTrack("hi", available = true, verified = false))
        assertFalse(HindiAudioPolicy.hasVerifiedHindiAudio(title))
    }

    @Test
    fun rejectsTitlesWithoutAuthorizedProvider() {
        val title = titleWith(
            AudioTrack("hi", available = true, verified = true),
            authorized = false
        )
        assertFalse(HindiAudioPolicy.hasVerifiedHindiAudio(title))
    }

    @Test
    fun rejectsOtherLanguagesAndUnknownAudio() {
        val title = titleWith(AudioTrack("en", available = true, verified = true))
        assertFalse(HindiAudioPolicy.hasVerifiedHindiAudio(title))
        assertFalse(HindiAudioPolicy.hasVerifiedHindiAudio(titleWith()))
    }

    @Test
    fun filtersOutEveryTitleWithoutAuthorizedVerifiedHindiAudio() {
        val hindi = titleWith(
            AudioTrack("hi-IN", available = true, verified = true),
            id = "hindi"
        )
        val english = titleWith(
            AudioTrack("en", available = true, verified = true),
            id = "english"
        )
        val unverified = titleWith(
            AudioTrack("hi", available = true, verified = false),
            id = "unverified"
        )
        val unauthorized = titleWith(
            AudioTrack("hi", available = true, verified = true),
            id = "unauthorized",
            authorized = false
        )
        assertEquals(
            listOf(hindi),
            HindiAudioPolicy.filterVerifiedHindiTitles(listOf(hindi, english, unverified, unauthorized))
        )
    }

    private fun titleWith(
        vararg tracks: AudioTrack,
        id: String = "sample",
        authorized: Boolean = true
    ) = CatalogTitle(
        id = id,
        name = id,
        category = CatalogCategory.MOVIES,
        audioTracks = tracks.toList(),
        fromAuthorizedProvider = authorized
    )
}

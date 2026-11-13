package com.mgsamirr.moviebox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HindiAudioPolicyTest {
    @Test
    fun acceptsAvailableHindiLanguageCodes() {
        listOf("hi", "HIN", " Hindi ").forEach { code ->
            val title = titleWith(AudioTrack(code, available = true))
            assertTrue(HindiAudioPolicy.hasVerifiedHindiAudio(title))
        }
    }

    @Test
    fun rejectsUnavailableHindiTrack() {
        val title = titleWith(AudioTrack("hi", available = false))
        assertFalse(HindiAudioPolicy.hasVerifiedHindiAudio(title))
    }

    @Test
    fun rejectsOtherLanguagesAndUnknownAudio() {
        val title = titleWith(AudioTrack("en", available = true))
        assertFalse(HindiAudioPolicy.hasVerifiedHindiAudio(title))
        assertFalse(HindiAudioPolicy.hasVerifiedHindiAudio(titleWith()))
    }

    @Test
    fun filtersOutEveryTitleWithoutConfirmedHindiAudio() {
        val hindi = titleWith(AudioTrack("hi", available = true), id = "hindi")
        val english = titleWith(AudioTrack("en", available = true), id = "english")
        val pending = titleWith(AudioTrack("hi", available = false), id = "pending")
        assertEquals(listOf(hindi), HindiAudioPolicy.filterVerifiedHindiTitles(listOf(hindi, english, pending)))
    }

    private fun titleWith(vararg tracks: AudioTrack, id: String = "sample") = CatalogTitle(
        id = id,
        name = id,
        category = CatalogCategory.MOVIES,
        audioTracks = tracks.toList()
    )
}

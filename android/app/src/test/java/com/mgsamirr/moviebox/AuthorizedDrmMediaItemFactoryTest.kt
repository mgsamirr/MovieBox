package com.mgsamirr.moviebox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthorizedDrmMediaItemFactoryTest {
    private fun title(authorized: Boolean = true, hindiVerified: Boolean = true) =
        CatalogTitle(
            id = "movie-1",
            name = "Example",
            category = CatalogCategory.MOVIES,
            audioTracks = listOf(AudioTrack("hi-IN", available = true, verified = hindiVerified)),
            fromAuthorizedProvider = authorized
        )

    @Test fun rejectsUnauthorizedContent() {
        val result = AuthorizedDrmMediaItemFactory.create(
            AuthorizedDrmPlaybackConfig("https://media.example/movie.mpd", "https://license.example", title(false))
        )
        assertTrue(result is DrmPlaybackResult.Rejected)
        assertEquals("Content provider is not authorized", (result as DrmPlaybackResult.Rejected).reason)
    }

    @Test fun rejectsUnverifiedHindiAudio() {
        val result = AuthorizedDrmMediaItemFactory.create(
            AuthorizedDrmPlaybackConfig("https://media.example/movie.mpd", "https://license.example", title(hindiVerified = false))
        )
        assertTrue(result is DrmPlaybackResult.Rejected)
    }

    @Test fun rejectsInsecureEndpoints() {
        val result = AuthorizedDrmMediaItemFactory.create(
            AuthorizedDrmPlaybackConfig("http://media.example/movie.mpd", "https://license.example", title())
        )
        assertTrue(result is DrmPlaybackResult.Rejected)
    }

    @Test fun rejectsHostlessOrCredentialedHttpsEndpoints() {
        assertFalse(AuthorizedDrmMediaItemFactory.isHttps("https:relative-path"))
        assertTrue(!AuthorizedDrmMediaItemFactory.isHttps("https://user:password@media.example/movie.mpd"))
        assertTrue(!AuthorizedDrmMediaItemFactory.isHttps("https://media.example/movie.mpd#fragment"))
    }

    @Test fun acceptsAuthorizedWidevineConfiguration() {
        val result = AuthorizedDrmMediaItemFactory.create(
            AuthorizedDrmPlaybackConfig("https://media.example/movie.mpd", "https://license.example", title())
        )
        assertTrue(result is DrmPlaybackResult.Ready)
    }
}

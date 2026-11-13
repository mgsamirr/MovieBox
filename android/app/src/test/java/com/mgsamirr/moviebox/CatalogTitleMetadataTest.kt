package com.mgsamirr.moviebox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogTitleMetadataTest {
    @Test
    fun acceptsSafeProviderMetadata() {
        val title = CatalogTitle(
            id = "movie-1",
            name = "Example",
            category = CatalogCategory.MOVIES,
            audioTracks = emptyList(),
            description = "  A short synopsis.  ",
            releaseYear = 2024,
            durationMinutes = 121,
            posterUrl = "https://cdn.example.org/poster.jpg"
        )
        assertEquals("A short synopsis.", title.safeDescription)
        assertEquals(2024, title.validReleaseYear)
        assertEquals(121, title.validDurationMinutes)
        assertEquals("https://cdn.example.org/poster.jpg", title.safePosterUrl)
    }

    @Test
    fun rejectsInvalidOrInsecureMetadata() {
        val title = CatalogTitle(
            id = "movie-2",
            name = "Example",
            category = CatalogCategory.MOVIES,
            audioTracks = emptyList(),
            description = "x".repeat(1500),
            releaseYear = 1800,
            durationMinutes = 5000,
            posterUrl = "http://example.org/poster.jpg"
        )
        assertEquals(1200, title.safeDescription.length)
        assertNull(title.validReleaseYear)
        assertNull(title.validDurationMinutes)
        assertNull(title.safePosterUrl)
    }
}

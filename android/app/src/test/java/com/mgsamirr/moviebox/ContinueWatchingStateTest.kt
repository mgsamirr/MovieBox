package com.mgsamirr.moviebox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class ContinueWatchingStateTest {
    private fun title(id: String, authorized: Boolean = true) = CatalogTitle(
        id = id,
        name = id,
        category = CatalogCategory.MOVIES,
        audioTracks = listOf(AudioTrack("hi-IN", available = true, verified = true)),
        fromAuthorizedProvider = authorized
    )

    @Test fun rejectsInvalidResumePoints() {
        val empty = ContinueWatchingState.empty()
        assertEquals(empty, empty.record(ResumePoint("movie", 0, 1000)))
        assertEquals(empty, empty.record(ResumePoint("movie", 1200, 1000)))
        assertEquals(empty, empty.record(ResumePoint("", 100, 1000)))
        assertEquals(empty, empty.record(ResumePoint("x".repeat(257), 100, 1000)))
        assertEquals(empty, empty.record(ResumePoint("movie", 100, 1000, -1)))
    }

    @Test fun onlyAuthorizedHindiTitlesAppear() {
        val state = ContinueWatchingState.from(listOf(
            ResumePoint("allowed", 300, 1000),
            ResumePoint("blocked", 500, 1000)
        ))
        assertEquals(
            listOf("allowed"),
            state.eligibleTitles(listOf(title("allowed"), title("blocked", authorized = false)))
                .map { it.id }
        )
    }

    @Test fun mostRecentlyUpdatedTitleAppearsFirst() {
        val state = ContinueWatchingState.from(listOf(
            ResumePoint("watched-longer", 800, 1000, updatedAtEpochMs = 100),
            ResumePoint("watched-later", 200, 1000, updatedAtEpochMs = 200)
        ))
        assertEquals(
            listOf("watched-later", "watched-longer"),
            state.eligibleTitles(listOf(title("watched-longer"), title("watched-later")))
                .map { it.id }
        )
    }

    @Test fun newerRecordReplacesOlderProgressForSameTitle() {
        val state = ContinueWatchingState.from(listOf(
            ResumePoint("movie", 300, 1000, updatedAtEpochMs = 100)
        )).record(ResumePoint("movie", 600, 1000, updatedAtEpochMs = 200))
        assertEquals(600L, state.resumePoints.getValue("movie").positionMs)
    }

    @Test fun removeClearsResumePoint() {
        val state = ContinueWatchingState.from(listOf(ResumePoint("movie", 300, 1000)))
        assertNotSame(state, state.remove("movie"))
        assertEquals(emptyList<CatalogTitle>(), state.remove("movie").eligibleTitles(listOf(title("movie"))))
    }
}

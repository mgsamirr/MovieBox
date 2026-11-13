package com.mgsamirr.moviebox

import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContinueWatchingStoreTest {
    private lateinit var preferences: android.content.SharedPreferences
    private lateinit var store: ContinueWatchingStore

    @Before
    fun setUp() {
        val application = RuntimeEnvironment.getApplication<android.app.Application>()
        preferences = application.getSharedPreferences("movie_box_resume_test", 0)
        preferences.edit().clear().commit()
        store = ContinueWatchingStore(preferences)
    }

    @Test
    fun recordPersistsAndReloadsResumePoint() {
        val expected = ResumePoint("movie-1", 12_000L, 90_000L, 1_700_000_000_000L)

        store.record(expected)

        assertEquals(expected, store.load().resumePoints["movie-1"])
    }

    @Test
    fun removeDeletesPersistedResumePoint() {
        store.record(ResumePoint("movie-1", 12_000L, 90_000L, 10L))

        store.remove("movie-1")

        assertTrue(store.load().resumePoints.isEmpty())
    }

    @Test
    fun malformedOrUnsupportedDataFailsClosed() {
        preferences.edit().putString("resume_points_json", "{not-json").commit()
        assertTrue(store.load().resumePoints.isEmpty())

        preferences.edit().putString(
            "resume_points_json",
            """{"version":99,"points":[{"titleId":"movie-1","positionMs":1,"durationMs":10,"updatedAtEpochMs":1}]}"""
        ).commit()
        assertTrue(store.load().resumePoints.isEmpty())
    }

    @Test
    fun invalidEntriesAreIgnoredWithoutDiscardingValidOnes() {
        preferences.edit().putString(
            "resume_points_json",
            """{"version":1,"points":[{"titleId":"good","positionMs":5,"durationMs":10,"updatedAtEpochMs":1},{"titleId":"bad","positionMs":20,"durationMs":10,"updatedAtEpochMs":1}]}"""
        ).commit()

        assertEquals(setOf("good"), store.load().resumePoints.keys)
    }

    @Test
    fun saveCapsResumeHistoryAtFiveHundredEntries() {
        val points = (1..600).map { index ->
            ResumePoint("movie-$index", 5L, 10L, index.toLong())
        }
        store.save(ContinueWatchingState.from(points))

        assertEquals(500, store.load().resumePoints.size)
    }

    @Test
    fun oversizedPayloadFailsClosed() {
        preferences.edit().putString("resume_points_json", "x".repeat(512 * 1024 + 1)).commit()

        assertTrue(store.load().resumePoints.isEmpty())
    }

    @Test
    fun resumeHistoryDoesNotWriteIntoWatchlistBackupPreferences() {
        val application = RuntimeEnvironment.getApplication<android.app.Application>()
        store.record(ResumePoint("movie-1", 5L, 10L, 1L))

        assertEquals(null, application.getSharedPreferences("movie_box", 0).getString("resume_points_json", null))
    }
}

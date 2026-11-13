package com.mgsamirr.moviebox

import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class Media3HindiAudioPolicyTest {
    @Test
    fun missingAudioTrackMetadataKeepsVideoAndAudioDisabled() {
        val context = RuntimeEnvironment.getApplication<android.app.Application>()
        val selector = DefaultTrackSelector(context)

        val result = Media3HindiAudioPolicy.apply(selector, Tracks.EMPTY)

        assertNull(result)
        assertTrue(selector.parameters.getTrackTypeDisabled(C.TRACK_TYPE_AUDIO))
        assertTrue(selector.parameters.getTrackTypeDisabled(C.TRACK_TYPE_VIDEO))
    }
}

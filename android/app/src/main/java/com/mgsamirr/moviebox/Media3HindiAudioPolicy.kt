package com.mgsamirr.moviebox

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

/**
 * Applies a fail-closed audio selection to the actual manifest/container tracks.
 * Returns null while no audio groups are exposed, false when audio exists but
 * no supported Hindi track is present, and true when a Hindi track is selected.
 */
object Media3HindiAudioPolicy {
    fun apply(trackSelector: DefaultTrackSelector, tracks: Tracks): Boolean? {
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (audioGroups.isEmpty()) {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            )
            return null
        }

        val hindiTrack = audioGroups.asSequence()
            .flatMap { group ->
                (0 until group.length).asSequence()
                    .filter { index ->
                        group.isTrackSupported(index) &&
                            HindiTrackSelector.isHindiLanguage(group.getTrackFormat(index).language)
                    }
                    .map { index -> group to index }
            }
            .firstOrNull()

        val parameters = trackSelector.buildUponParameters()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, hindiTrack == null)
        if (hindiTrack != null) {
            parameters.addOverride(
                TrackSelectionOverride(hindiTrack.first.mediaTrackGroup, listOf(hindiTrack.second))
            )
        }
        trackSelector.setParameters(parameters)
        return hindiTrack != null
    }
}

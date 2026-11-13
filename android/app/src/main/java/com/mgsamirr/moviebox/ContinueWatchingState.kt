package com.mgsamirr.moviebox

/**
 * Resume points are stored as playback positions, not stream URLs or credentials.
 * Only authorized titles with verified Hindi audio can be shown in Continue Watching.
 */
data class ResumePoint(
    val titleId: String,
    val positionMs: Long,
    val durationMs: Long
) {
    val isValid: Boolean
        get() = titleId.isNotBlank() &&
            durationMs > 0L &&
            positionMs in 1 until durationMs
}

data class ContinueWatchingState private constructor(
    val resumePoints: Map<String, ResumePoint>
) {
    fun record(point: ResumePoint): ContinueWatchingState {
        if (!point.isValid) return this
        return copy(resumePoints = resumePoints + (point.titleId to point))
    }

    fun remove(titleId: String): ContinueWatchingState =
        copy(resumePoints = resumePoints - titleId)

    fun eligibleTitles(titles: List<CatalogTitle>): List<CatalogTitle> {
        val eligibleIds = titles
            .filter(HindiAudioPolicy::hasVerifiedHindiAudio)
            .mapTo(mutableSetOf()) { it.id }
        return resumePoints.values
            .filter { it.titleId in eligibleIds && it.isValid }
            .sortedByDescending { it.positionMs }
            .mapNotNull { point -> titles.firstOrNull { it.id == point.titleId } }
    }

    companion object {
        fun empty(): ContinueWatchingState = ContinueWatchingState(emptyMap())

        fun from(points: List<ResumePoint>): ContinueWatchingState =
            points.filter(ResumePoint::isValid).associateBy { it.titleId }
                .let(::ContinueWatchingState)
    }
}

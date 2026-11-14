package com.mgsamirr.moviebox

/**
 * Resume points store playback positions only; never stream URLs, DRM keys, or credentials.
 * updatedAtEpochMs controls recency ordering independently of playback position.
 */
data class ResumePoint(
    val titleId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtEpochMs: Long = 0L
) {
    val isValid: Boolean
        get() = titleId.isNotBlank() && titleId.length <= 256 &&
            durationMs > 0L && positionMs in 1 until durationMs &&
            updatedAtEpochMs >= 0L
}

data class ContinueWatchingState private constructor(
    val resumePoints: Map<String, ResumePoint>
) {
    fun record(point: ResumePoint): ContinueWatchingState {
        if (!point.isValid) return this
        val timestamped = if (point.updatedAtEpochMs == 0L) {
            point.copy(updatedAtEpochMs = System.currentTimeMillis())
        } else point
        return copy(resumePoints = resumePoints + (point.titleId to timestamped))
    }

    fun remove(titleId: String): ContinueWatchingState =
        copy(resumePoints = resumePoints - titleId)

    fun eligibleTitles(titles: List<CatalogTitle>): List<CatalogTitle> {
        val titlesById = titles
            .filter(HindiAudioPolicy::hasVerifiedHindiAudio)
            .associateBy { it.id }
        return resumePoints.values
            .filter { it.titleId in titlesById && it.isValid }
            .sortedWith(compareByDescending<ResumePoint> { it.updatedAtEpochMs }
                .thenByDescending { it.positionMs })
            .mapNotNull { titlesById[it.titleId] }
    }

    companion object {
        fun empty(): ContinueWatchingState = ContinueWatchingState(emptyMap())

        fun from(points: List<ResumePoint>): ContinueWatchingState =
            points.filter(ResumePoint::isValid)
                .associateBy { it.titleId }
                .let(::ContinueWatchingState)
    }
}

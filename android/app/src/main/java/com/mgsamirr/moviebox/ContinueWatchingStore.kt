package com.mgsamirr.moviebox

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists resume positions in a separate preferences file so Android backup
 * rules can keep the watchlist without copying viewing history to another device.
 * Stream URLs, DRM state, provider credentials and license data are never stored.
 */
class ContinueWatchingStore(
    private val preferences: SharedPreferences
) {
    fun load(): ContinueWatchingState {
        val raw = preferences.getString(KEY_RESUME_POINTS, null) ?: return ContinueWatchingState.empty()
        if (raw.length > MAX_JSON_CHARS) return ContinueWatchingState.empty()

        val points = runCatching {
            val root = JSONObject(raw)
            if (root.optInt(JSON_VERSION, -1) != CURRENT_VERSION) {
                return@runCatching emptyList()
            }
            val entries = root.optJSONArray(JSON_POINTS) ?: return@runCatching emptyList()
            buildList {
                for (index in 0 until minOf(entries.length(), MAX_POINTS)) {
                    val item = entries.optJSONObject(index) ?: continue
                    val point = ResumePoint(
                        titleId = item.optString(JSON_TITLE_ID, ""),
                        positionMs = item.optLong(JSON_POSITION_MS, -1L),
                        durationMs = item.optLong(JSON_DURATION_MS, -1L),
                        updatedAtEpochMs = item.optLong(JSON_UPDATED_AT, -1L)
                    )
                    if (point.isValid) add(point)
                }
            }
        }.getOrElse { emptyList() }

        return ContinueWatchingState.from(points)
    }

    fun save(state: ContinueWatchingState) {
        val points = state.resumePoints.values
            .filter(ResumePoint::isValid)
            .sortedByDescending(ResumePoint::updatedAtEpochMs)
            .take(MAX_POINTS)
        val root = JSONObject()
            .put(JSON_VERSION, CURRENT_VERSION)
            .put(JSON_POINTS, JSONArray().apply {
                points.forEach { point ->
                    put(
                        JSONObject()
                            .put(JSON_TITLE_ID, point.titleId)
                            .put(JSON_POSITION_MS, point.positionMs)
                            .put(JSON_DURATION_MS, point.durationMs)
                            .put(JSON_UPDATED_AT, point.updatedAtEpochMs)
                    )
                }
            })
        preferences.edit().putString(KEY_RESUME_POINTS, root.toString()).apply()
    }

    fun record(point: ResumePoint): ContinueWatchingState {
        val next = load().record(point)
        if (point.isValid) save(next)
        return next
    }

    fun remove(titleId: String): ContinueWatchingState {
        val next = load().remove(titleId)
        save(next)
        return next
    }

    private companion object {
        const val KEY_RESUME_POINTS = "resume_points_json"
        const val JSON_VERSION = "version"
        const val JSON_POINTS = "points"
        const val JSON_TITLE_ID = "titleId"
        const val JSON_POSITION_MS = "positionMs"
        const val JSON_DURATION_MS = "durationMs"
        const val JSON_UPDATED_AT = "updatedAtEpochMs"
        const val CURRENT_VERSION = 1
        const val MAX_POINTS = 500
        const val MAX_JSON_CHARS = 512 * 1024
    }
}

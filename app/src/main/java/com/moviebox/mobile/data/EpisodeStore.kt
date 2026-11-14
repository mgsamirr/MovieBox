package com.moviebox.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CatalogEpisode(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val synopsis: String = "",
    val playbackUrl: String = "",
    val downloadUrl: String = "",
    val durationMinutes: Int = 0
)

class EpisodeStore(context: Context) {
    private val prefs = context.getSharedPreferences("moviebox_episodes", Context.MODE_PRIVATE)

    fun all(seriesId: String): List<CatalogEpisode> {
        val root = runCatching { JSONObject(prefs.getString("items", "{}")) }.getOrElse { JSONObject() }
        val array = root.optJSONArray(seriesId) ?: JSONArray()
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                add(CatalogEpisode(o.optString("id"), o.optInt("seasonNumber", 1), o.optInt("episodeNumber", 1),
                    o.optString("title"), o.optString("synopsis"), o.optString("playbackUrl"),
                    o.optString("downloadUrl"), o.optInt("durationMinutes", 0)))
            }
        }.sortedWith(compareBy<CatalogEpisode> { it.seasonNumber }.thenBy { it.episodeNumber })
    }

    fun save(seriesId: String, episode: CatalogEpisode) =
        persist(seriesId, all(seriesId).filterNot { it.id == episode.id } + episode)

    fun delete(seriesId: String, episodeId: String) =
        persist(seriesId, all(seriesId).filterNot { it.id == episodeId })

    fun deleteSeason(seriesId: String, seasonNumber: Int) =
        persist(seriesId, all(seriesId).filterNot { it.seasonNumber == seasonNumber })

    private fun persist(seriesId: String, episodes: List<CatalogEpisode>) {
        val root = runCatching { JSONObject(prefs.getString("items", "{}")) }.getOrElse { JSONObject() }
        val array = JSONArray()
        episodes.forEach { e -> array.put(JSONObject().apply {
            put("id", e.id); put("seasonNumber", e.seasonNumber); put("episodeNumber", e.episodeNumber)
            put("title", e.title); put("synopsis", e.synopsis); put("playbackUrl", e.playbackUrl)
            put("downloadUrl", e.downloadUrl); put("durationMinutes", e.durationMinutes)
        }) }
        root.put(seriesId, array)
        prefs.edit().putString("items", root.toString()).apply()
    }
}

package com.moviebox.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class RecentTitle(val title: CatalogTitle, val watchedAt: Long)

class WatchHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("moviebox_watch_history", Context.MODE_PRIVATE)

    fun all(catalog: CatalogStore): List<RecentTitle> {
        val items = catalog.all().associateBy { it.id }
        val array = runCatching { JSONArray(prefs.getString("items", "[]")) }.getOrElse { JSONArray() }
        return buildList {
            for (i in 0 until array.length()) {
                val row = array.optJSONObject(i) ?: continue
                val title = items[row.optString("id")] ?: continue
                add(RecentTitle(title, row.optLong("watchedAt")))
            }
        }.sortedByDescending { it.watchedAt }
    }

    fun markPlayed(title: CatalogTitle) {
        val existing = runCatching { JSONArray(prefs.getString("items", "[]")) }.getOrElse { JSONArray() }
        val rows = (0 until existing.length()).mapNotNull { existing.optJSONObject(it) }
            .filterNot { it.optString("id") == title.id }
        val next = JSONArray()
        next.put(JSONObject().put("id", title.id).put("watchedAt", System.currentTimeMillis()))
        rows.take(49).forEach { next.put(it) }
        prefs.edit().putString("items", next.toString()).apply()
    }

    fun clear() = prefs.edit().remove("items").apply()
}

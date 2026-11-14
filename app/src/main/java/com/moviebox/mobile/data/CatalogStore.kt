package com.moviebox.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class ContentCategory(val label: String) {
    MOVIE("Movies"), ANIME("Anime"), SERIES("Web Series")
}

data class CatalogTitle(
    val id: String,
    val title: String,
    val category: ContentCategory,
    val language: String,
    val synopsis: String,
    val playbackUrl: String,
    val downloadUrl: String,
    val isWatchlisted: Boolean = false,
    val genre: String = "",
    val releaseYear: String = "",
    val posterUrl: String = ""
)

class CatalogStore(context: Context) {
    private val prefs = context.getSharedPreferences("moviebox_catalog", Context.MODE_PRIVATE)

    fun all(): List<CatalogTitle> {
        val array = runCatching { JSONArray(prefs.getString("items", "[]")) }.getOrElse { JSONArray() }
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val category = runCatching { ContentCategory.valueOf(o.optString("category")) }.getOrDefault(ContentCategory.MOVIE)
                add(CatalogTitle(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    category = category,
                    language = o.optString("language", "Hindi Dubbed"),
                    synopsis = o.optString("synopsis"),
                    playbackUrl = o.optString("playbackUrl"),
                    downloadUrl = o.optString("downloadUrl"),
                    isWatchlisted = o.optBoolean("watchlisted"),
                    genre = o.optString("genre"),
                    releaseYear = o.optString("releaseYear"),
                    posterUrl = o.optString("posterUrl")
                ))
            }
        }
    }

    fun save(title: CatalogTitle) {
        val items = all().filterNot { it.id == title.id } + title
        persist(items)
    }

    fun delete(id: String) = persist(all().filterNot { it.id == id })

    fun toggleWatchlist(id: String) {
        persist(all().map { if (it.id == id) it.copy(isWatchlisted = !it.isWatchlisted) else it })
    }

    private fun persist(items: List<CatalogTitle>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id); put("title", item.title); put("category", item.category.name)
                put("language", item.language); put("synopsis", item.synopsis)
                put("playbackUrl", item.playbackUrl); put("downloadUrl", item.downloadUrl)
                put("watchlisted", item.isWatchlisted); put("genre", item.genre)
                put("releaseYear", item.releaseYear); put("posterUrl", item.posterUrl)
            })
        }
        prefs.edit().putString("items", array.toString()).apply()
    }
}

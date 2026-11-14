package com.moviebox.mobile.data

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject

data class SavedDownload(val id: Long, val title: String, val fileName: String)

class DownloadRepository(private val context: Context) {
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val prefs = context.getSharedPreferences("moviebox_downloads", Context.MODE_PRIVATE)

    fun enqueue(url: String, title: String, fileName: String): Long {
        val uri = Uri.parse(url.trim())
        require(uri.scheme == "https" && !uri.host.isNullOrBlank()) { "A valid HTTPS download URL is required." }
        val safeName = fileName.substringAfterLast('/').substringBefore('?')
            .replace(Regex("[^A-Za-z0-9._ -]"), "_").ifBlank { "movie-download.mp4" }
        val request = DownloadManager.Request(uri)
            .setTitle(title)
            .setDescription("MovieBox offline download")
            .setMimeType("video/*")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, safeName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
        val id = manager.enqueue(request)
        val records = all().filterNot { it.id == id } + SavedDownload(id, title, safeName)
        save(records)
        return id
    }

    fun all(): List<SavedDownload> {
        val array = runCatching { JSONArray(prefs.getString("items", "[]")) }.getOrElse { JSONArray() }
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                add(SavedDownload(o.optLong("id"), o.optString("title"), o.optString("fileName")))
            }
        }
    }

    fun status(id: Long): DownloadStatus? {
        val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            fun value(column: String) = it.getColumnIndex(column).takeIf { index -> index >= 0 }?.let(it::getInt) ?: 0
            val status = value(DownloadManager.COLUMN_STATUS)
            val downloaded = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR).let { index -> if (index >= 0) it.getLong(index) else 0L }
            val total = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES).let { index -> if (index >= 0) it.getLong(index) else -1L }
            val reason = value(DownloadManager.COLUMN_REASON)
            return DownloadStatus(status, downloaded, total, reason)
        }
    }

    fun downloadedUri(id: Long): Uri? = manager.getUriForDownloadedFile(id)

    fun remove(id: Long) {
        manager.remove(id)
        save(all().filterNot { it.id == id })
    }

    private fun save(items: List<SavedDownload>) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("id", it.id).put("title", it.title).put("fileName", it.fileName)) }
        prefs.edit().putString("items", array.toString()).apply()
    }
}

data class DownloadStatus(val status: Int, val downloadedBytes: Long, val totalBytes: Long, val reason: Int) {
    fun label(): String = when (status) {
        DownloadManager.STATUS_PENDING -> "Waiting"
        DownloadManager.STATUS_RUNNING -> "Downloading"
        DownloadManager.STATUS_PAUSED -> "Paused"
        DownloadManager.STATUS_SUCCESSFUL -> "Downloaded"
        DownloadManager.STATUS_FAILED -> "Failed ($reason)"
        else -> "Checking"
    }
}

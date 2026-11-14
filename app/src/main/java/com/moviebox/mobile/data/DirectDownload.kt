package com.moviebox.mobile.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

/**
 * Enqueues a direct file download. This class downloads files only; it never
 * hands a remote URL to the video player.
 *
 * Call only with a direct-download URL from a source that authorizes downloading.
 */
class DirectDownload(private val context: Context) {
    private val manager = context.getSystemService(DownloadManager::class.java)

    fun enqueue(url: String, fileName: String, title: String): Long {
        val uri = Uri.parse(url)
        require(uri.scheme == "https") { "Only HTTPS download links are accepted." }
        require(uri.host != null) { "Invalid download URL." }
        val safeName = fileName.substringAfterLast('/').substringBefore('?')
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .ifBlank { "movie-download.mp4" }

        val request = DownloadManager.Request(uri)
            .setTitle(title)
            .setDescription("Downloading for offline viewing")
            .setMimeType("video/*")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_MOVIES,
                safeName
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        return manager.enqueue(request)
    }

    fun query(id: Long): DownloadManager.Query =
        DownloadManager.Query().setFilterById(id)
}

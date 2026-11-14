package com.moviebox.mobile.data

import android.content.Context

class PlaybackProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("moviebox_playback_progress", Context.MODE_PRIVATE)

    fun positionMs(mediaId: String): Long = prefs.getLong(mediaId, 0L).coerceAtLeast(0L)

    fun save(mediaId: String, positionMs: Long) {
        if (positionMs <= 0L) return
        prefs.edit().putLong(mediaId, positionMs).apply()
    }

    fun clear(mediaId: String) {
        prefs.edit().remove(mediaId).apply()
    }
}

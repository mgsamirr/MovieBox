package com.moviebox.mobile.data

import android.content.Context

class AppPreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences("moviebox_app_preferences", Context.MODE_PRIVATE)

    fun resumePlayback(): Boolean = prefs.getBoolean("resume_playback", true)
    fun setResumePlayback(enabled: Boolean) {
        prefs.edit().putBoolean("resume_playback", enabled).apply()
    }

    fun showRecentlyWatched(): Boolean = prefs.getBoolean("show_recently_watched", true)
    fun setShowRecentlyWatched(enabled: Boolean) {
        prefs.edit().putBoolean("show_recently_watched", enabled).apply()
    }
}

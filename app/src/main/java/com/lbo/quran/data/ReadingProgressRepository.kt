package com.lbo.quran.data

import android.content.Context

class ReadingProgressRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("quran_reading_progress", Context.MODE_PRIVATE)

    fun getLastReadAyah(): String? {
        val id = prefs.getString(KEY_LAST_READ, null)
        return if (id.isNullOrBlank()) null else id
    }

    fun saveLastReadAyah(aId: String) {
        prefs.edit().putString(KEY_LAST_READ, aId).apply()
    }

    fun getBookmarks(): List<String> {
        val raw = prefs.getString(KEY_BOOKMARKS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun isBookmarked(aId: String): Boolean = aId in getBookmarks()

    fun addBookmark(aId: String) {
        val current = getBookmarks()
        if (aId in current) return
        val updated = current + aId
        prefs.edit().putString(KEY_BOOKMARKS, updated.joinToString(",")).apply()
    }

    fun removeBookmark(aId: String) {
        val updated = getBookmarks().filter { it != aId }
        prefs.edit().putString(KEY_BOOKMARKS, updated.joinToString(",")).apply()
    }

    fun toggleBookmark(aId: String): Boolean {
        return if (isBookmarked(aId)) {
            removeBookmark(aId)
            false
        } else {
            addBookmark(aId)
            true
        }
    }

    companion object {
        private const val KEY_LAST_READ = "last_read_a_id"
        private const val KEY_BOOKMARKS = "bookmarked_a_ids"
    }
}

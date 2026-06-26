package com.example.android_dev.data

import android.content.Context

class PlusModePreference(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("smart_todo_plus_mode", Context.MODE_PRIVATE)

    fun load(): Boolean = prefs.getBoolean(KEY_PLUS_ENABLED, true)

    fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PLUS_ENABLED, enabled).apply()
    }

    private companion object {
        const val KEY_PLUS_ENABLED = "plus_enabled"
    }
}

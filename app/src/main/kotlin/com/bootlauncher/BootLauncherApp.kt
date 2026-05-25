package com.bootlauncher

import android.app.Application
import android.content.Context
import com.bootlauncher.data.local.AppDatabase
import com.bootlauncher.util.FileLogger

class BootLauncherApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        FileLogger.init(this)
    }

    companion object {
        private const val PREFS_NAME = "bootlauncher_prefs"
        private const val KEY_AUTO_START_ENABLED = "auto_start_enabled"
        private const val KEY_SHOW_WHEN_LOCKED = "show_when_locked"

        fun isShowWhenLocked(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_SHOW_WHEN_LOCKED, false)
        }

        fun setShowWhenLocked(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putBoolean(KEY_SHOW_WHEN_LOCKED, enabled).apply()
        }

        fun isAutoStartEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_AUTO_START_ENABLED, true)
        }

        fun setAutoStartEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putBoolean(KEY_AUTO_START_ENABLED, enabled).apply()
        }
    }
}

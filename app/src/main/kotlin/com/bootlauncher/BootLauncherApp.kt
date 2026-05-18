package com.bootlauncher

import android.app.Application
import com.bootlauncher.data.local.AppDatabase

class BootLauncherApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}

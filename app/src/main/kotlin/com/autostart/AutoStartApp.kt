package com.autostart

import android.app.Application
import com.autostart.data.local.AppDatabase

class AutoStartApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}

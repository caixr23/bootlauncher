package com.bootlauncher.ui.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bootlauncher.data.local.AppDatabase
import com.bootlauncher.data.local.AppEntity
import com.bootlauncher.data.local.AppRepository
import com.bootlauncher.util.FileLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        FileLogger.d("LauncherVM", "init: getting database")
        val db = AppDatabase.getDatabase(application)
        repository = AppRepository(db.appDao(), db.launchLogDao())
        FileLogger.d("LauncherVM", "init: repository created, querying desktop apps")
        viewModelScope.launch {
            val apps = repository.getDesktopAppsOnce()
            FileLogger.d("LauncherVM", "desktopAppsOnce: count=${apps.size}, pkgs=${apps.map { it.packageName }}")
        }
    }

    val desktopApps: StateFlow<List<AppEntity>> = repository.getDesktopApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

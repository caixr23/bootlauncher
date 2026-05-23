package com.bootlauncher.ui.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bootlauncher.data.local.AppDatabase
import com.bootlauncher.data.local.AppEntity
import com.bootlauncher.data.local.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AppRepository(db.appDao(), db.launchLogDao())
    }

    val desktopApps: StateFlow<List<AppEntity>> = repository.getDesktopApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

package com.bootlauncher.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bootlauncher.util.FileLogger
import com.bootlauncher.BootLauncherApp
import com.bootlauncher.data.local.AppDatabase
import com.bootlauncher.data.local.AppEntity
import com.bootlauncher.data.local.AppRepository
import com.bootlauncher.data.local.LaunchLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AppRepository(db.appDao(), db.launchLogDao())
    }

    val apps: StateFlow<List<AppEntity>> = repository.getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _latestBootTime = MutableStateFlow<Long?>(null)
    val latestBootTime: StateFlow<Long?> = _latestBootTime

    private val _bootLogs = MutableStateFlow<List<LaunchLog>>(emptyList())
    val bootLogs: StateFlow<List<LaunchLog>> = _bootLogs

    val autoStartEnabled: MutableStateFlow<Boolean> = MutableStateFlow(
        BootLauncherApp.isAutoStartEnabled(application)
    )

    init {
        loadLatestBootLogs()
    }

    private fun loadLatestBootLogs() {
        viewModelScope.launch {
            val bootTime = repository.getLatestBootTime()
            if (bootTime != null) {
                _latestBootTime.value = bootTime
                _bootLogs.value = repository.getLogsForBoot(bootTime)
            }
        }
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        BootLauncherApp.setAutoStartEnabled(getApplication(), enabled)
        autoStartEnabled.value = enabled
    }

    val showWhenLocked: MutableStateFlow<Boolean> = MutableStateFlow(
        BootLauncherApp.isShowWhenLocked(application)
    )

    fun setShowWhenLocked(enabled: Boolean) {
        BootLauncherApp.setShowWhenLocked(getApplication(), enabled)
        showWhenLocked.value = enabled
    }

    fun addApp(packageName: String, label: String) {
        viewModelScope.launch {
            try {
                val currentApps = repository.getAllApps().first()
                val nextSortOrder = currentApps.size
                repository.insert(AppEntity(
                    packageName = packageName,
                    label = label,
                    sortOrder = nextSortOrder,
                    enabled = true
                ))
            } catch (e: Exception) {
                FileLogger.e("MainViewModel", "addApp failed for $packageName", e)
            }
        }
    }

    fun removeApp(app: AppEntity) {
        viewModelScope.launch {
            repository.delete(app)
        }
    }

    fun updateEnabled(app: AppEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.update(app.copy(enabled = enabled))
        }
    }

    fun updateDelay(id: Long, delayMs: Long) {
        viewModelScope.launch {
            val current = repository.getAllApps().first()
            val target = current.find { it.id == id }
            if (target != null) {
                repository.update(target.copy(delayMs = delayMs))
            }
        }
    }

    fun moveApp(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val current = repository.getAllApps().first()
            if (fromIndex !in current.indices || toIndex !in current.indices) return@launch
            val reordered = current.toMutableList()
            val item = reordered.removeAt(fromIndex)
            reordered.add(toIndex, item)
            reordered.forEachIndexed { index, app ->
                repository.updateOrder(app.id, index)
            }
        }
    }

    fun updateShowOnDesktop(app: AppEntity, show: Boolean) {
        viewModelScope.launch {
            repository.update(app.copy(showOnDesktop = show))
        }
    }
}

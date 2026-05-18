package com.bootlauncher.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bootlauncher.data.local.AppDatabase
import com.bootlauncher.data.local.AppEntity
import com.bootlauncher.data.local.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val dao = AppDatabase.getDatabase(application).appDao()
        repository = AppRepository(dao)
    }

    val apps: StateFlow<List<AppEntity>> = repository.getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addApp(packageName: String, label: String) {
        viewModelScope.launch {
            val currentApps = repository.getAllApps().first()
            val nextSortOrder = currentApps.size
            repository.insert(
                AppEntity(
                    packageName = packageName,
                    label = label,
                    sortOrder = nextSortOrder,
                    enabled = true
                )
            )
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
            repository.getAllApps().collect { apps ->
                val target = apps.find { it.id == id }
                if (target != null) {
                    repository.update(target.copy(delayMs = delayMs))
                    return@collect
                }
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
}

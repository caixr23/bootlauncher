package com.autostart.ui.main

import com.autostart.data.local.AppDao
import com.autostart.data.local.AppEntity
import com.autostart.data.local.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelTest {

    class FakeAppDao : AppDao {
        private val apps = mutableListOf<AppEntity>()
        override fun getEnabledApps(): Flow<List<AppEntity>> = flowOf(apps.filter { it.enabled })
        override fun getAllApps(): Flow<List<AppEntity>> = flowOf(apps.toList())
        override suspend fun getEnabledAppsOnce(): List<AppEntity> = apps.filter { it.enabled }
        override suspend fun insert(app: AppEntity): Long { apps.add(app); return apps.size.toLong() }
        override suspend fun update(app: AppEntity) {
            val idx = apps.indexOfFirst { it.id == app.id }
            if (idx >= 0) apps[idx] = app
        }
        override suspend fun delete(app: AppEntity) { apps.remove(app) }
        override suspend fun updateOrder(id: Long, order: Int) {
            val idx = apps.indexOfFirst { it.id == id }
            if (idx >= 0) apps[idx] = apps[idx].copy(sortOrder = order)
        }
        override suspend fun deleteAll() { apps.clear() }
        override suspend fun count(): Int = apps.size
    }

    @Test
    fun addApp_incrementsSortOrder() = runTest {
        val repo = AppRepository(FakeAppDao())
        repo.insert(AppEntity(packageName = "com.app1", label = "App 1", sortOrder = 0, enabled = true))
        repo.insert(AppEntity(packageName = "com.app2", label = "App 2", sortOrder = 1, enabled = true))

        val apps = repo.getAllApps().first()
        assertEquals(2, apps.size)
        assertEquals(0, apps[0].sortOrder)
        assertEquals(1, apps[1].sortOrder)
    }

    @Test
    fun removeApp_deletesFromRepo() = runTest {
        val repo = AppRepository(FakeAppDao())
        repo.insert(AppEntity(packageName = "com.app1", label = "App 1", enabled = true))
        val app = repo.getAllApps().first().first()
        repo.delete(app)

        assertEquals(0, repo.getAllApps().first().size)
    }

    @Test
    fun updateDelay_updatesAppDelay() = runTest {
        val repo = AppRepository(FakeAppDao())
        repo.insert(AppEntity(packageName = "com.app1", label = "App 1", delayMs = 0, enabled = true))
        val app = repo.getAllApps().first().first()
        repo.update(app.copy(delayMs = 5000))

        assertEquals(5000L, repo.getAllApps().first().first().delayMs)
    }
}

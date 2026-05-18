package com.bootlauncher.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals

class AppRepositoryTest {

    class FakeAppDao : AppDao {
        private val apps = mutableListOf<AppEntity>()
        override fun getEnabledApps() = kotlinx.coroutines.flow.flowOf(apps.filter { it.enabled })
        override fun getAllApps() = kotlinx.coroutines.flow.flowOf(apps.toList())
        override suspend fun getEnabledAppsOnce() = apps.filter { it.enabled }
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
    fun insertAndGetAllApps() = runTest {
        val repository = AppRepository(FakeAppDao())
        repository.insert(AppEntity(packageName = "com.example.test", label = "Test App", delayMs = 1000, sortOrder = 0, enabled = true))

        val apps = repository.getAllApps().first()
        assertEquals(1, apps.size)
        assertEquals("com.example.test", apps[0].packageName)
        assertEquals("Test App", apps[0].label)
        assertEquals(1000L, apps[0].delayMs)
    }

    @Test
    fun getEnabledAppsExcludesDisabled() = runTest {
        val repository = AppRepository(FakeAppDao())
        repository.insert(AppEntity(packageName = "com.enabled", label = "Enabled", enabled = true, sortOrder = 0))
        repository.insert(AppEntity(packageName = "com.disabled", label = "Disabled", enabled = false, sortOrder = 1))

        val enabled = repository.getEnabledApps().first()
        assertEquals(1, enabled.size)
        assertEquals("com.enabled", enabled[0].packageName)
    }

    @Test
    fun enabledAppsSortedByOrder() = runTest {
        val repository = AppRepository(FakeAppDao())
        repository.insert(AppEntity(packageName = "com.second", label = "Second", enabled = true, sortOrder = 2))
        repository.insert(AppEntity(packageName = "com.first", label = "First", enabled = true, sortOrder = 1))

        val enabled = repository.getEnabledApps().first()
        assertEquals("com.first", enabled[0].packageName)
        assertEquals("com.second", enabled[1].packageName)
    }

    @Test
    fun deleteRemovesApp() = runTest {
        val repository = AppRepository(FakeAppDao())
        repository.insert(AppEntity(packageName = "com.del", label = "Delete Me", enabled = true, sortOrder = 0))
        val app = repository.getAllApps().first().first()
        repository.delete(app)

        val apps = repository.getAllApps().first()
        assertEquals(0, apps.size)
    }

    @Test
    fun updateChangesDelay() = runTest {
        val repository = AppRepository(FakeAppDao())
        repository.insert(AppEntity(packageName = "com.update", label = "Update Me", delayMs = 0, sortOrder = 0))
        val app = repository.getAllApps().first().first().copy(delayMs = 5000)
        repository.update(app)

        val updated = repository.getAllApps().first().first()
        assertEquals(5000L, updated.delayMs)
    }

    @Test
    fun getEnabledAppsOnceReturnsSnapshot() = runTest {
        val repository = AppRepository(FakeAppDao())
        repository.insert(AppEntity(packageName = "com.snapshot", label = "Snapshot", enabled = true, sortOrder = 0))

        val apps = repository.getEnabledAppsOnce()
        assertEquals(1, apps.size)
    }
}

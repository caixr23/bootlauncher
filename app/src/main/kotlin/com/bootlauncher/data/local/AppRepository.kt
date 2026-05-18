package com.bootlauncher.data.local

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    fun getAllApps(): Flow<List<AppEntity>> = appDao.getAllApps()

    fun getEnabledApps(): Flow<List<AppEntity>> = appDao.getEnabledApps()

    suspend fun getEnabledAppsOnce(): List<AppEntity> = appDao.getEnabledAppsOnce()

    suspend fun insert(app: AppEntity): Long = appDao.insert(app)

    suspend fun update(app: AppEntity) = appDao.update(app)

    suspend fun delete(app: AppEntity) = appDao.delete(app)

    suspend fun updateOrder(id: Long, order: Int) = appDao.updateOrder(id, order)
}

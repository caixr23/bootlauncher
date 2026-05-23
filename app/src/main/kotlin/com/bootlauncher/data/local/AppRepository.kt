package com.bootlauncher.data.local

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val appDao: AppDao,
    private val launchLogDao: LaunchLogDao
) {

    fun getAllApps(): Flow<List<AppEntity>> = appDao.getAllApps()

    fun getEnabledApps(): Flow<List<AppEntity>> = appDao.getEnabledApps()

    suspend fun getEnabledAppsOnce(): List<AppEntity> = appDao.getEnabledAppsOnce()

    suspend fun insert(app: AppEntity): Long = appDao.insert(app)

    suspend fun update(app: AppEntity) = appDao.update(app)

    suspend fun delete(app: AppEntity) = appDao.delete(app)

    suspend fun updateOrder(id: Long, order: Int) = appDao.updateOrder(id, order)

    suspend fun insertLaunchLog(log: LaunchLog) = launchLogDao.insert(log)

    suspend fun insertLaunchLogs(logs: List<LaunchLog>) = launchLogDao.insertAll(logs)

    suspend fun getLatestBootTime(): Long? = launchLogDao.getLatestBootTime()

    suspend fun getLogsForBoot(bootTime: Long): List<LaunchLog> = launchLogDao.getLogsForBoot(bootTime)

    suspend fun getRecentLogs(limit: Int = 50): List<LaunchLog> = launchLogDao.getRecentLogs(limit)

    fun getDesktopApps(): Flow<List<AppEntity>> = appDao.getDesktopApps()

    suspend fun getDesktopAppsOnce(): List<AppEntity> = appDao.getDesktopAppsOnce()

    suspend fun desktopAppCount(): Int = appDao.desktopAppCount()
}

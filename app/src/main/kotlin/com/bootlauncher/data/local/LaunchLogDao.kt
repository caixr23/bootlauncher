package com.bootlauncher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LaunchLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LaunchLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<LaunchLog>)

    @Query("SELECT DISTINCT bootTime FROM launch_logs ORDER BY bootTime DESC LIMIT 1")
    suspend fun getLatestBootTime(): Long?

    @Query("SELECT * FROM launch_logs WHERE bootTime = :bootTime ORDER BY launchTime ASC")
    suspend fun getLogsForBoot(bootTime: Long): List<LaunchLog>

    @Query("SELECT * FROM launch_logs ORDER BY bootTime DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 50): List<LaunchLog>

    @Query("DELETE FROM launch_logs WHERE bootTime = :bootTime")
    suspend fun deleteForBoot(bootTime: Long)

    @Query("DELETE FROM launch_logs")
    suspend fun deleteAll()
}

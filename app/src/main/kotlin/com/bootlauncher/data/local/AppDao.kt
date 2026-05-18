package com.bootlauncher.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps WHERE enabled = 1 ORDER BY sortOrder ASC")
    fun getEnabledApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps ORDER BY sortOrder ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE enabled = 1 ORDER BY sortOrder ASC")
    suspend fun getEnabledAppsOnce(): List<AppEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: AppEntity): Long

    @Update
    suspend fun update(app: AppEntity)

    @Delete
    suspend fun delete(app: AppEntity)

    @Query("UPDATE apps SET sortOrder = :order WHERE id = :id")
    suspend fun updateOrder(id: Long, order: Int)

    @Query("DELETE FROM apps")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM apps")
    suspend fun count(): Int
}

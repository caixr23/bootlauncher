package com.bootlauncher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launch_logs", primaryKeys = ["bootTime", "packageName"])
data class LaunchLog(
    val bootTime: Long,
    val packageName: String,
    val launchTime: Long,
    val delayMs: Long,
    val success: Boolean,
    val errorMessage: String? = null
)

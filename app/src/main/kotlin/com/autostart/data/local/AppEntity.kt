package com.autostart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val label: String,
    val delayMs: Long = 0,
    val sortOrder: Int = 0,
    val enabled: Boolean = true
)

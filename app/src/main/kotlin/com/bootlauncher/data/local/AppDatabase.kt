package com.bootlauncher.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bootlauncher.util.FileLogger

@Database(
    entities = [AppEntity::class, LaunchLog::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun launchLogDao(): LaunchLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS launch_logs (
                        bootTime INTEGER NOT NULL,
                        packageName TEXT NOT NULL,
                        launchTime INTEGER NOT NULL,
                        delayMs INTEGER NOT NULL,
                        success INTEGER NOT NULL,
                        errorMessage TEXT,
                        PRIMARY KEY (bootTime, packageName)
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            FileLogger.d("AppDatabase", "getDatabase called")
            return INSTANCE ?: synchronized(this) {
                FileLogger.d("AppDatabase", "building database")
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bootlauncher.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                FileLogger.d("AppDatabase", "database built, getting DAOs")
                val result = db.also { INSTANCE = it }
                FileLogger.d("AppDatabase", "getDatabase returning")
                result
            }
        }
    }
}

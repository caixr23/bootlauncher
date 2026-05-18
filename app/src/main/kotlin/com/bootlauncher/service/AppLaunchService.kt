package com.bootlauncher.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.bootlauncher.R
import com.bootlauncher.data.local.AppDatabase
import com.bootlauncher.data.local.AppRepository
import com.bootlauncher.data.local.LaunchLog
import com.bootlauncher.util.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppLaunchService : Service() {

    companion object {
        private const val TAG = "AppLaunchService"
        private const val CHANNEL_ID = "bootlauncher_launch_channel"
        private const val NOTIFICATION_ID = 1
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var repository: AppRepository

    override fun onCreate() {
        super.onCreate()
        FileLogger.d(TAG, "onCreate")
        val db = AppDatabase.getDatabase(this)
        repository = AppRepository(db.appDao(), db.launchLogDao())
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bootTime = intent?.getLongExtra("boot_time", System.currentTimeMillis())
            ?: System.currentTimeMillis()
        FileLogger.d(TAG, "onStartCommand: bootTime=$bootTime")
        serviceScope.launch {
            launchConfiguredApps(bootTime)
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        FileLogger.d(TAG, "onDestroy")
    }

    private suspend fun launchConfiguredApps(bootTime: Long) {
        val apps = repository.getEnabledAppsOnce()
        if (apps.isEmpty()) {
            FileLogger.d(TAG, "No enabled apps to launch")
            return
        }

        FileLogger.d(TAG, "Launching ${apps.size} apps, bootTime=$bootTime")
        val logs = mutableListOf<LaunchLog>()

        for ((index, app) in apps.withIndex()) {
            val startTime = System.currentTimeMillis()
            FileLogger.d(TAG, "[$index/${apps.size}] Preparing to launch: ${app.label} (${app.packageName}), delay=${app.delayMs}ms")

            if (app.delayMs > 0) {
                FileLogger.d(TAG, "  Waiting ${app.delayMs}ms...")
                delay(app.delayMs)
            }

            val result = launchApp(app.packageName)
            val endTime = System.currentTimeMillis()
            val elapsed = endTime - startTime

            logs.add(
                LaunchLog(
                    bootTime = bootTime,
                    packageName = app.packageName,
                    launchTime = endTime,
                    delayMs = app.delayMs,
                    success = result.first,
                    errorMessage = result.second
                )
            )

            if (result.first) {
                FileLogger.d(TAG, "  Launched ${app.packageName} in ${elapsed}ms")
            } else {
                FileLogger.e(TAG, "  Failed to launch ${app.packageName} in ${elapsed}ms: ${result.second}")
            }
        }

        repository.insertLaunchLogs(logs)
        val totalElapsed = System.currentTimeMillis() - bootTime
        FileLogger.d(TAG, "All apps launched. Total elapsed: ${totalElapsed}ms")
    }

    private fun launchApp(packageName: String): Pair<Boolean, String?> {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                Pair(true, null)
            } else {
                FileLogger.w(TAG, "No launch intent for: $packageName")
                Pair(false, "No launch intent found")
            }
        } catch (e: Exception) {
            FileLogger.e(TAG, "Failed to launch $packageName", e)
            Pair(false, e.message)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
        FileLogger.d(TAG, "Notification channel created: $CHANNEL_ID")
    }

    private fun buildNotification() = android.app.Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text))
        .build()
}

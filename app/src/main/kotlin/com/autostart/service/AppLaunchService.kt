package com.autostart.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.autostart.R
import com.autostart.data.local.AppDatabase
import com.autostart.data.local.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppLaunchService : Service() {

    companion object {
        private const val TAG = "AppLaunchService"
        private const val CHANNEL_ID = "autostart_launch_channel"
        private const val NOTIFICATION_ID = 1
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var repository: AppRepository

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(AppDatabase.getDatabase(this).appDao())
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started, launching configured apps")
        serviceScope.launch {
            launchConfiguredApps()
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    private suspend fun launchConfiguredApps() {
        val apps = repository.getEnabledAppsOnce()
        if (apps.isEmpty()) {
            Log.d(TAG, "No enabled apps to launch")
            return
        }

        Log.d(TAG, "Launching ${apps.size} apps")

        for (app in apps) {
            if (app.delayMs > 0) {
                Log.d(TAG, "Waiting ${app.delayMs}ms before launching ${app.label}")
                delay(app.delayMs)
            }
            launchApp(app.packageName)
        }

        Log.d(TAG, "All apps launched")
    }

    private fun launchApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                Log.d(TAG, "Launched: $packageName")
            } else {
                Log.w(TAG, "No launch intent for: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
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
    }

    private fun buildNotification() = android.app.Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text))
        .build()
}

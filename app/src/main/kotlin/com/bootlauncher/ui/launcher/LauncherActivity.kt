package com.bootlauncher.ui.launcher

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.bootlauncher.R
import com.bootlauncher.BootLauncherApp
import com.bootlauncher.receiver.LockScreenAdminReceiver
import com.bootlauncher.service.AppLaunchService
import com.bootlauncher.util.FileLogger

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val relaunchRunnable = Runnable {
        FileLogger.d("PhoneCall", "Relaunching apps after call")
        val intent = Intent(this, AppLaunchService::class.java)
        intent.putExtra("boot_time", System.currentTimeMillis())
        startForegroundService(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BootLauncherApp.isShowWhenLocked(this)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent {
            val apps by viewModel.desktopApps.collectAsState()
            androidx.compose.material3.MaterialTheme {
                LauncherDesktopScreen(
                    apps = apps,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
        handleLaunchIntent(intent)
        registerPhoneCallListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(relaunchRunnable)
        try {
            val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        } catch (e: Exception) {
            FileLogger.w("PhoneCall", "Failed to unregister listener", e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun registerPhoneCallListener() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            FileLogger.d("PhoneCall", "READ_PHONE_STATE not granted, skipping")
            return
        }
        try {
            val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            FileLogger.d("PhoneCall", "PhoneStateListener registered")
        } catch (e: Exception) {
            FileLogger.e("PhoneCall", "Failed to register PhoneStateListener", e)
        }
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    FileLogger.d("PhoneCall", "Incoming call detected, canceling any pending relaunch")
                    handler.removeCallbacks(relaunchRunnable)
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    FileLogger.d("PhoneCall", "Call answered")
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    FileLogger.d("PhoneCall", "Call ended, scheduling relaunch in 30s")
                    handler.removeCallbacks(relaunchRunnable)
                    handler.postDelayed(relaunchRunnable, 30_000)
                }
            }
        }
    }

    private fun handleLaunchIntent(intent: Intent?) {
        when (intent?.action) {
            "com.bootlauncher.LAUNCH_APP" -> {
                val packageName = intent.getStringExtra("package_name")
                if (packageName != null) {
                    FileLogger.d("LauncherActivity", "Received LAUNCH_APP intent: $packageName")
                    launchApp(packageName)
                }
            }
            "com.bootlauncher.SCREEN_CONTROL" -> {
                val actions = intent.getStringArrayExtra("action_type")
                    ?: intent.getStringExtra("action_type")?.let { arrayOf(it) }
                    ?: arrayOf("wake")
                FileLogger.d("LauncherActivity", "Received SCREEN_CONTROL: ${actions.joinToString()}")
                for (action in actions) {
                    when (action) {
                        "wake" -> window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                        "unlock" -> window.addFlags(
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        )
                        "lock" -> {
                            val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
                            val comp = ComponentName(this, LockScreenAdminReceiver::class.java)
                            if (dpm.isAdminActive(comp)) {
                                dpm.lockNow()
                            } else {
                                FileLogger.e("LauncherActivity", "Device admin not activated, cannot lock")
                            }
                        }
                    }
                }
            }
            "com.bootlauncher.SHOW_NOTIFICATION" -> {
                val title = intent.getStringExtra("title") ?: "BootLauncher"
                val message = intent.getStringExtra("message") ?: ""
                val durationMs = intent.getLongExtra("duration", 3000)
                val importance = when (intent.getStringExtra("importance")) {
                    "high" -> NotificationManager.IMPORTANCE_HIGH
                    "default" -> NotificationManager.IMPORTANCE_DEFAULT
                    "min" -> NotificationManager.IMPORTANCE_MIN
                    else -> NotificationManager.IMPORTANCE_LOW
                }
                FileLogger.d("LauncherActivity", "Received SHOW_NOTIFICATION: title=$title, message=$message, duration=${durationMs}ms, importance=$importance")
                showNotification(title, message, durationMs, importance)
            }
        }
    }

    private fun launchApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                FileLogger.e("LauncherActivity", "No launch intent for: $packageName")
            }
        } catch (e: Exception) {
            FileLogger.e("LauncherActivity", "Failed to launch $packageName", e)
        }
    }

    private fun showNotification(title: String, message: String, durationMs: Long, importance: Int) {
        val channelId = "bootlauncher_notify_channel"
        val notificationId = 1001
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Notification", importance)
        )
        val notification = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .build()
        nm.notify(notificationId, notification)
        handler.postDelayed({ nm.cancel(notificationId) }, durationMs)
    }
}

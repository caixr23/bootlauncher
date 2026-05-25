package com.bootlauncher.ui.launcher

import android.content.Intent
import android.os.Bundle
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
import com.bootlauncher.BootLauncherApp
import com.bootlauncher.util.FileLogger

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.action == "com.bootlauncher.LAUNCH_APP") {
            val packageName = intent.getStringExtra("package_name")
            if (packageName != null) {
                FileLogger.d("LauncherActivity", "Received LAUNCH_APP intent: $packageName")
                launchApp(packageName)
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
}

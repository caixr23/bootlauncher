package com.bootlauncher.ui.launcher

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bootlauncher.util.FileLogger

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        FileLogger.d("LauncherActivity", "onCreate")
        setContent {
            val apps by viewModel.desktopApps.collectAsState()
            FileLogger.d("LauncherActivity", "compose: apps.size=${apps.size}")
            LaunchedEffect(apps.size) {
                FileLogger.d("LauncherActivity", "LaunchedEffect: apps.size=${apps.size}, pkgs=${apps.map { it.packageName }}")
            }
            androidx.compose.material3.MaterialTheme {
                LauncherDesktopScreen(
                    apps = apps,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }
}

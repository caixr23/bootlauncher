package com.bootlauncher.ui.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.bootlauncher.R
import com.bootlauncher.service.AppLaunchService
import com.bootlauncher.util.FileLogger
import java.util.concurrent.atomic.AtomicBoolean

class LauncherActivity : ComponentActivity() {

    private val hasLaunched = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = getString(R.string.launcher_message)
            textSize = 24f
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
        })

        startLaunchService()
    }

    override fun onResume() {
        super.onResume()
        FileLogger.d("LauncherActivity", "onResume")
        startLaunchService()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        FileLogger.d("LauncherActivity", "onNewIntent")
        hasLaunched.set(false)
    }

    private fun startLaunchService() {
        if (!hasLaunched.compareAndSet(false, true)) {
            FileLogger.d("LauncherActivity", "Already launched, skipping")
            return
        }
        FileLogger.d("LauncherActivity", "Starting AppLaunchService")
        val intent = Intent(this, AppLaunchService::class.java)
        intent.putExtra("boot_time", System.currentTimeMillis())
        startForegroundService(intent)
    }
}

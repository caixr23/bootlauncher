package com.bootlauncher.ui.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bootlauncher.R
import com.bootlauncher.service.AppLaunchService

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = getString(R.string.launcher_message)
            textSize = 24f
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
        })

        startForegroundService(Intent(this, AppLaunchService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        finish()
    }
}

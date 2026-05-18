package com.bootlauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bootlauncher.BootLauncherApp
import com.bootlauncher.service.AppLaunchService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            if (!BootLauncherApp.isAutoStartEnabled(context)) {
                Log.d(TAG, "Auto-start is disabled, skipping")
                return
            }
            Log.d(TAG, "Boot completed, starting AppLaunchService")
            val serviceIntent = Intent(context, AppLaunchService::class.java)
            serviceIntent.putExtra("boot_time", System.currentTimeMillis())
            context.startForegroundService(serviceIntent)
        }
    }
}

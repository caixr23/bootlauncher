package com.autostart.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable
)

class PackageManagerHelper(private val context: Context) {

    private val pm = context.packageManager

    fun getInstalledApps(excludePackageNames: Set<String>): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolveInfos
            .filter { it.activityInfo.packageName !in excludePackageNames }
            .map {
                val appInfo = it.activityInfo.applicationInfo
                InstalledApp(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString(),
                    icon = appInfo.loadIcon(pm)
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

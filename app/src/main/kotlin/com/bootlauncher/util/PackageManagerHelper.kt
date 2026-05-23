package com.bootlauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable
)

class PackageManagerHelper(val context: Context) {

    private val pm = context.packageManager

    fun getInstalledApps(excludePackageNames: Set<String>): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            FileLogger.e("PackageManagerHelper", "queryIntentActivities failed", e)
            return emptyList()
        }
        return resolveInfos
            .filter { it.activityInfo.packageName !in excludePackageNames }
            .distinctBy { it.activityInfo.packageName }
            .map {
                val appInfo = it.activityInfo.applicationInfo
                val icon = try {
                    appInfo.loadIcon(pm)
                } catch (e: Exception) {
                    FileLogger.w("PackageManagerHelper", "loadIcon failed for ${it.activityInfo.packageName}", e)
                    pm.defaultActivityIcon
                }
                InstalledApp(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString(),
                    icon = icon
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

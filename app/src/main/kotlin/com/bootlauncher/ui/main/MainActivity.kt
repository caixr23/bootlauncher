package com.bootlauncher.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bootlauncher.BootLauncherApp
import com.bootlauncher.data.local.AppEntity
import com.bootlauncher.data.local.LaunchLog
import com.bootlauncher.ui.components.AppListItem
import com.bootlauncher.ui.components.AppPickerSheet
import com.bootlauncher.ui.components.DelayPickerDialog
import com.bootlauncher.ui.theme.BootLauncherTheme
import com.bootlauncher.util.FileLogger
import com.bootlauncher.util.PackageManagerHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun isDefaultLauncher(context: Context): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.packageName == context.packageName
}

fun requestDefaultLauncher(context: Context) {
    val pkg = context.packageName
    val component = "$pkg/.ui.launcher.LauncherActivity"
    try {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "pm set-home-activity $component"))
        process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            Toast.makeText(context, "已设置为默认桌面，请重启设备生效", Toast.LENGTH_LONG).show()
            return
        }
    } catch (e: Exception) {
        FileLogger.w("Launcher", "pm command failed", e)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? android.app.role.RoleManager
            if (roleManager?.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) == true) {
                if (roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                    Toast.makeText(context, "已经是默认桌面", Toast.LENGTH_SHORT).show()
                    return
                } else {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                    (context as? ComponentActivity)?.startActivityForResult(intent, REQUEST_CODE_SET_LAUNCHER)
                    return
                }
            }
        } catch (e: Exception) {
            FileLogger.w("Launcher", "RoleManager failed", e)
        }
    }

    val intents = listOf(
        Intent("android.settings.HOME_SETTINGS"),
        Intent(Settings.ACTION_HOME_SETTINGS),
        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$pkg")
        }
    )
    for (intent in intents) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            FileLogger.w("Launcher", "Failed to open: ${intent.action}", e)
        }
    }
    Toast.makeText(context, "无法打开设置，请手动在系统设置中设置默认桌面", Toast.LENGTH_LONG).show()
}

private const val REQUEST_CODE_SET_LAUNCHER = 1001

data class CheckResult(val name: String, val passed: Boolean, val detail: String)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var pmHelper: PackageManagerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            pmHelper = PackageManagerHelper(this)
            setContent {
                BootLauncherTheme {
                    MainScreen(viewModel, pmHelper)
                }
            }
        } catch (e: Exception) {
            FileLogger.e("MainActivity", "onCreate failed", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, pmHelper: PackageManagerHelper) {
    val apps by viewModel.apps.collectAsState()
    val autoStartEnabled by viewModel.autoStartEnabled.collectAsState()
    val showWhenLocked by viewModel.showWhenLocked.collectAsState()
    val latestBootTime by viewModel.latestBootTime.collectAsState()
    val bootLogs by viewModel.bootLogs.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }
    var showDelayDialog by remember { mutableStateOf<AppEntity?>(null) }
    var showLogsExpanded by remember { mutableStateOf(false) }
    var checkResults by remember { mutableStateOf<List<CheckResult>>(emptyList()) }

    val context = LocalContext.current
    var isDefaultLauncher by remember { mutableStateOf(isDefaultLauncher(context)) }
    val notificationPermissionGranted = remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted.value = granted
    }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    val configuredPackages = remember(apps) { apps.map { it.packageName }.toSet() }
    val desktopCount = remember(apps) { apps.count { it.showOnDesktop } }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("BootLauncher Manager") },
                scrollBehavior = scrollBehavior,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "Desktop: $desktopCount/${MainViewModel.MAX_DESKTOP_APPS}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text("Auto-start", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = autoStartEnabled,
                            onCheckedChange = { viewModel.setAutoStartEnabled(it) }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAppPicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add App")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Button(
                    onClick = { checkResults = runDiagnostics(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("  Diagnose Permissions", modifier = Modifier.padding(start = 8.dp))
                }
            }

            if (checkResults.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val allPassed = checkResults.all { it.passed }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (allPassed) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (allPassed) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    if (allPassed) "All checks passed"
                                    else "${checkResults.count { it.passed }}/${checkResults.size} passed",
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            checkResults.forEach { result ->
                                CheckResultItem(result)
                            }
                        }
                    }
                }
            }

            if (!notificationPermissionGranted.value) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Notification permission denied. Launch service may not work.",
                                modifier = Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            FileLogger.w("MainActivity", "Battery optimization settings not available", e)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Add to battery optimization whitelist",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDefaultLauncher)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isDefaultLauncher) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isDefaultLauncher) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                            Text(
                                if (isDefaultLauncher) "Is Default Launcher"
                                else "NOT Default Launcher",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        if (!isDefaultLauncher) {
                            Text(
                                "App is not set as the default home screen. Tap below to set it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                            Button(
                                onClick = { requestDefaultLauncher(context) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Set as Default Launcher")
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show on Lock Screen", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Skip lock screen, show desktop directly",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = showWhenLocked,
                        onCheckedChange = { viewModel.setShowWhenLocked(it) }
                    )
                }
            }

            if (latestBootTime != null) {
                item {
                    BootTimeCard(
                        bootTime = latestBootTime!!,
                        logs = bootLogs,
                        expanded = showLogsExpanded,
                        onExpandChange = { showLogsExpanded = it }
                    )
                }
            }

            if (apps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No apps configured. Tap \"+\" to add.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                items(apps, key = { it.id }) { app ->
                    val iconBitmap = remember(app.packageName) {
                        try {
                            val drawable = pmHelper.context.packageManager.getApplicationIcon(app.packageName)
                            val bmp = android.graphics.Bitmap.createScaledBitmap(
                                drawable.toBitmap(), 96, 96, true
                            )
                            bmp.asImageBitmap()
                        } catch (e: Exception) {
                            FileLogger.w("MainActivity", "Icon load failed for ${app.packageName}", e)
                            null
                        }
                    }
                    AppListItem(
                        app = app,
                        icon = iconBitmap,
                        onLaunchClick = {
                            try {
                                val intent = pmHelper.context.packageManager
                                    .getLaunchIntentForPackage(app.packageName)
                                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                pmHelper.context.startActivity(intent)
                            } catch (e: Exception) {
                                FileLogger.e("MainActivity", "Launch failed for ${app.packageName}", e)
                            }
                        },
                        onEnabledChange = { enabled -> viewModel.updateEnabled(app, enabled) },
                        onShowOnDesktopChange = { show -> viewModel.updateShowOnDesktop(app, show) },
                        onDelayClick = { showDelayDialog = app },
                        onDelete = { viewModel.removeApp(app) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAppPicker) {
        val installedApps = remember(configuredPackages) {
            pmHelper.getInstalledApps(setOf("com.bootlauncher") + configuredPackages)
        }
        AppPickerSheet(
            installedApps = installedApps,
            onAppSelected = { installedApp ->
                viewModel.addApp(installedApp.packageName, installedApp.label)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    showDelayDialog?.let { app ->
        DelayPickerDialog(
            currentDelaySeconds = (app.delayMs / 1000).toInt(),
            onSave = { newDelayMs ->
                viewModel.updateDelay(app.id, newDelayMs)
                showDelayDialog = null
            },
            onDismiss = { showDelayDialog = null }
        )
    }
}

private fun runDiagnostics(context: Context): List<CheckResult> {
    val results = mutableListOf<CheckResult>()
    val pm = context.packageManager
    val pkg = context.packageName

    val isHome = isDefaultLauncher(context)
    results.add(CheckResult(
        "Default Launcher",
        isHome,
        if (isHome) "Is the default home screen" else "NOT set as default launcher (tap the button above to set)"
    ))

    val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true
    results.add(CheckResult(
        "Notification Permission",
        notifGranted,
        if (notifGranted) "Granted" else "Denied (POST_NOTIFICATIONS)"
    ))

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val ignoringBattery = powerManager.isIgnoringBatteryOptimizations(pkg)
    results.add(CheckResult(
        "Battery Optimization Whitelist",
        ignoringBattery,
        if (ignoringBattery) "Whitelisted" else "Not whitelisted (may be killed by system)"
    ))

    val bootReceiver = try {
        val receiverInfo = pm.getReceiverInfo(
            android.content.ComponentName(pkg, "com.bootlauncher.receiver.BootReceiver"),
            PackageManager.GET_META_DATA
        )
        receiverInfo.enabled
    } catch (e: Exception) {
        FileLogger.w("Diag", "BootReceiver check failed", e)
        false
    }
    results.add(CheckResult(
        "Boot Receiver Registered",
        bootReceiver,
        if (bootReceiver) "Enabled, will receive BOOT_COMPLETED" else "NOT registered or disabled"
    ))

    val autoStart = BootLauncherApp.isAutoStartEnabled(context)
    results.add(CheckResult(
        "Auto-start Enabled",
        autoStart,
        if (autoStart) "Will launch apps on boot" else "Disabled in settings"
    ))

    val serviceType = try {
        val info = pm.getServiceInfo(
            android.content.ComponentName(pkg, "com.bootlauncher.service.AppLaunchService"),
            PackageManager.GET_META_DATA
        )
        info.foregroundServiceType
    } catch (e: Exception) {
        FileLogger.w("Diag", "Service check failed", e)
        0
    }
    val hasSpecialUse = serviceType and android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE != 0
    results.add(CheckResult(
        "Foreground Service Type",
        hasSpecialUse,
        if (hasSpecialUse) "SPECIAL_USE declared" else "Missing SPECIAL_USE type (Android 14+ will fail)"
    ))

    val installedCount = try {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).size
    } catch (e: Exception) {
        FileLogger.w("Diag", "queryIntentActivities failed", e)
        -1
    }
    val queryOk = installedCount > 0
    results.add(CheckResult(
        "App Query (QUERY_ALL_PACKAGES)",
        queryOk,
        if (queryOk) "Found $installedCount launchable apps" else "Cannot query apps (set as default launcher first)"
    ))

    val logFile = FileLogger.getLogFile()
    val logOk = logFile != null && logFile.parentFile?.exists() == true
    results.add(CheckResult(
        "File Logger",
        logOk,
        if (logOk) "Path: ${logFile?.absolutePath}" else "Log directory not accessible"
    ))

    return results
}

@Composable
private fun CheckResultItem(result: CheckResult) {
    val icon = if (result.passed) Icons.Default.CheckCircle else Icons.Default.Error
    val tint = if (result.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon, contentDescription = null, tint = tint,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(result.name, style = MaterialTheme.typography.bodySmall)
            Text(
                result.detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BootTimeCard(
    bootTime: Long,
    logs: List<LaunchLog>,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val bootTimeStr = dateFormat.format(Date(bootTime))

    val totalElapsed = if (logs.isNotEmpty()) {
        val lastLaunch = logs.maxOf { it.launchTime }
        lastLaunch - bootTime
    } else 0L

    val successCount = logs.count { it.success }
    val failCount = logs.size - successCount

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Last Boot",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        bootTimeStr,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Total: ${totalElapsed}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        "Success: $successCount / Fail: $failCount",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = { onExpandChange(!expanded) },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(if (expanded) "Hide details" else "Show details (${logs.size} apps)")
                }

                if (expanded) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    logs.forEach { log ->
                        LaunchLogItem(log, dateFormat)
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchLogItem(log: LaunchLog, dateFormat: SimpleDateFormat) {
    val timeStr = dateFormat.format(Date(log.launchTime))
    val icon = if (log.success) Icons.Default.CheckCircle else Icons.Default.Error
    val tint = if (log.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(end = 8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(log.packageName, style = MaterialTheme.typography.bodySmall)
            if (log.errorMessage != null) {
                Text(
                    log.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Text(
            "${log.delayMs / 1000}s delay | $timeStr",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

package com.bootlauncher.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.bootlauncher.data.local.AppEntity
import com.bootlauncher.data.local.LaunchLog
import com.bootlauncher.ui.components.AppListItem
import com.bootlauncher.ui.components.AppPickerSheet
import com.bootlauncher.ui.components.DelayPickerDialog
import com.bootlauncher.ui.theme.BootLauncherTheme
import com.bootlauncher.util.PackageManagerHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var pmHelper: PackageManagerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")
        pmHelper = PackageManagerHelper(this)
        setContent {
            BootLauncherTheme {
                MainScreen(viewModel, pmHelper)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, pmHelper: PackageManagerHelper) {
    val apps by viewModel.apps.collectAsState()
    val autoStartEnabled by viewModel.autoStartEnabled.collectAsState()
    val latestBootTime by viewModel.latestBootTime.collectAsState()
    val bootLogs by viewModel.bootLogs.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }
    var showDelayDialog by remember { mutableStateOf<AppEntity?>(null) }
    var showLogsExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
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
        Log.d("MainActivity", "Notification permission: $granted")
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val configuredPackages = remember(apps) { apps.map { it.packageName }.toSet() }
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
                            Log.w("MainActivity", "Battery optimization settings not available", e)
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
                    val icon = remember(app.packageName) {
                        try {
                            pmHelper.context.packageManager.getApplicationIcon(app.packageName)
                        } catch (e: PackageManager.NameNotFoundException) {
                            null
                        }
                    }
                    AppListItem(
                        app = app,
                        icon = icon,
                        onEnabledChange = { enabled -> viewModel.updateEnabled(app, enabled) },
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

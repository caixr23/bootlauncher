package com.bootlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.bootlauncher.util.FileLogger
import com.bootlauncher.util.InstalledApp

private val loggedPackages = mutableSetOf<String>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    installedApps: List<InstalledApp>,
    onAppSelected: (InstalledApp) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    FileLogger.d("AppPickerSheet", "compose start, apps=${installedApps.size}")
    val sheetState = rememberModalBottomSheetState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        FileLogger.d("AppPickerSheet", "LaunchedEffect fired")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        FileLogger.d("AppPickerSheet", "ModalBottomSheet content composing")
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Select App",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }
        val filtered = if (searchQuery.isBlank()) installedApps
            else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }

        FileLogger.d("AppPickerSheet", "filtered=${filtered.size}, building LazyColumn")

        LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
            items(filtered, key = { it.packageName }) { app ->
                FileLogger.d("AppPickerSheet", "composing item: ${app.packageName}")

                val iconBitmap = try {
                    val drawable = app.icon
                    FileLogger.d("AppPickerSheet", "icon class=${drawable?.javaClass?.simpleName}, " +
                        "w=${drawable?.intrinsicWidth}, h=${drawable?.intrinsicHeight} for ${app.packageName}")
                    drawable?.toBitmap()
                } catch (e: Exception) {
                    FileLogger.e("AppPickerSheet", "toBitmap failed for ${app.packageName}: ${e.message}", e)
                    null
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppSelected(app) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (iconBitmap != null) {
                        FileLogger.d("AppPickerSheet", "creating Image for ${app.packageName}, " +
                            "bitmap=${iconBitmap.width}x${iconBitmap.height}, config=${iconBitmap.config}")
                        Image(
                            bitmap = iconBitmap.asImageBitmap(),
                            contentDescription = app.label,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        FileLogger.w("AppPickerSheet", "skipping Image for ${app.packageName}, iconBitmap is null")
                    }
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        LaunchedEffect(filtered.size) {
            FileLogger.d("AppPickerSheet", "LazyColumn composition done, filtered=${filtered.size}")
        }
    }
}

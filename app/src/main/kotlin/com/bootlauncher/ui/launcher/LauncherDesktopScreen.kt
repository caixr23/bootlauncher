package com.bootlauncher.ui.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.bootlauncher.data.local.AppEntity
import com.bootlauncher.ui.main.MainActivity
import com.bootlauncher.util.FileLogger

private const val MAX_DESKTOP_SLOTS = 20

@Composable
fun LauncherDesktopScreen(
    apps: List<AppEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val desktopItems = remember(apps) {
        val items = apps.map { DesktopItem.App(it) }.toMutableList<DesktopItem>()
        if (items.size < MAX_DESKTOP_SLOTS - 1) {
            repeat(MAX_DESKTOP_SLOTS - 1 - items.size) {
                items.add(DesktopItem.Empty)
            }
        }
        items.add(DesktopItem.Settings)
        items
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(desktopItems) { item ->
            when (item) {
                is DesktopItem.App -> AppGridItem(
                    app = item.app,
                    context = context,
                    onClick = { launchApp(context, item.app.packageName) }
                )
                is DesktopItem.Settings -> SettingsGridItem(
                    onClick = {
                        val intent = Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
                is DesktopItem.Empty -> EmptySlot()
            }
        }
    }
}

@Composable
private fun AppGridItem(
    app: AppEntity,
    context: Context,
    onClick: () -> Unit
) {
    val icon: ImageBitmap? = remember(app.packageName) {
        try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(app.packageName)
            val bitmap = Bitmap.createScaledBitmap(drawable.toBitmap(), 96, 96, true)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            FileLogger.e("LauncherDesktop", "Failed to load icon for ${app.packageName}", e)
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = app.label,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun SettingsGridItem(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = "Settings",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun EmptySlot() {
    Box(modifier = Modifier.fillMaxSize())
}

private fun launchApp(context: Context, packageName: String) {
    FileLogger.d("LauncherDesktop", "Launching app: $packageName")
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            FileLogger.e("LauncherDesktop", "No launch intent for $packageName")
        }
    } catch (e: Exception) {
        FileLogger.e("LauncherDesktop", "Failed to launch $packageName", e)
    }
}

private sealed class DesktopItem {
    data class App(val app: AppEntity) : DesktopItem()
    object Settings : DesktopItem()
    object Empty : DesktopItem()
}

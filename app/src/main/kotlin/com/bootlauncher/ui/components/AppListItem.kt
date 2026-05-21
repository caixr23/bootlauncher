package com.bootlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.unit.dp
import com.bootlauncher.data.local.AppEntity
import com.bootlauncher.util.FileLogger

@Composable
fun AppListItem(
    app: AppEntity,
    icon: android.graphics.drawable.Drawable?,
    onEnabledChange: (Boolean) -> Unit,
    onDelayClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    FileLogger.d("AppListItem", "composing app: ${app.packageName} (id=${app.id}), label=${app.label}")
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                FileLogger.d("AppListItem", "icon for ${app.packageName}: class=${it.javaClass.simpleName}, w=${it.intrinsicWidth}, h=${it.intrinsicHeight}")
                val iconBitmap = try {
                    it.toBitmap()
                } catch (e: Exception) {
                    FileLogger.e("AppListItem", "toBitmap failed for ${app.packageName}: ${e.message}", e)
                    null
                }
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap.asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    FileLogger.w("AppListItem", "skipping Image for ${app.packageName}")
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onDelayClick, modifier = Modifier.padding(start = (-8).dp)) {
                    Text(
                        text = "Delay: ${app.delayMs / 1000}s",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Checkbox(checked = app.enabled, onCheckedChange = onEnabledChange)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}

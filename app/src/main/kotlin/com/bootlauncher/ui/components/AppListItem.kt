package com.bootlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bootlauncher.data.local.AppEntity
import com.bootlauncher.util.FileLogger

@Composable
fun AppListItem(
    app: AppEntity,
    onEnabledChange: (Boolean) -> Unit,
    onDelayClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    FileLogger.d("AppListItem", "start: id=${app.id}, pkg=${app.packageName}")
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileLogger.d("AppListItem", "Row start: ${app.packageName}")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                FileLogger.d("AppListItem", "label Text: ${app.label}")
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FileLogger.d("AppListItem", "delay Text: delay=${app.delayMs}")
                Text(
                    text = "Delay: ${app.delayMs / 1000}s",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { onDelayClick() }
                )
                FileLogger.d("AppListItem", "Column done: ${app.packageName}")
            }
            FileLogger.d("AppListItem", "Checkbox: ${app.packageName}")
            Checkbox(checked = app.enabled, onCheckedChange = onEnabledChange)
            FileLogger.d("AppListItem", "IconButton: ${app.packageName}")
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
            FileLogger.d("AppListItem", "Row done: ${app.packageName}")
        }
    }
    FileLogger.d("AppListItem", "end: ${app.packageName}")
}

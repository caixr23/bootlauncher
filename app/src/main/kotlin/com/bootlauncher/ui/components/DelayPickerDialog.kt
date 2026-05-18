package com.bootlauncher.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DelayPickerDialog(
    currentDelaySeconds: Int,
    onSave: (newDelayMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var delaySeconds by remember { mutableIntStateOf(currentDelaySeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Delay") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = delaySeconds.toString(),
                    onValueChange = { value ->
                        delaySeconds = value.toIntOrNull() ?: 0
                    },
                    label = { Text("Delay") },
                    modifier = Modifier.width(100.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("seconds")
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(delaySeconds.toLong() * 1000) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

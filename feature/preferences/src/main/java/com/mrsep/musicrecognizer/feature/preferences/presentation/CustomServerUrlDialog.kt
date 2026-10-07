package com.mrsep.musicrecognizer.feature.preferences.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrsep.musicrecognizer.core.strings.R as StringsR

@Composable
internal fun CustomServerUrlDialog(
    currentUrl: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val urlState = remember { mutableStateOf(currentUrl) }
    val isValidUrl = remember(urlState.value) {
        urlState.value.isEmpty() || isValidServerUrl(urlState.value)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Config custom server")
        },
        text = {
            Column {
                Text("Enter server URL (POST endpoint):")
                OutlinedTextField(
                    value = urlState.value,
                    onValueChange = { urlState.value = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://example.com/api/music") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    singleLine = true,
                )
                if (urlState.value.isNotEmpty() && !isValidUrl) {
                    Text(
                        "Invalid URL",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(urlState.value.trim()) },
                enabled = isValidUrl
            ) {
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

private fun isValidServerUrl(url: String): Boolean {
    return try {
        java.net.URL(url)
        url.startsWith("http://") || url.startsWith("https://")
    } catch (e: Exception) {
        false
    }
}

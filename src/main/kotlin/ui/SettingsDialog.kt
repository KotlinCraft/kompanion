package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import config.AppConfig

@Composable
fun SettingsDialog(
    initialConfig: AppConfig,
    onClose: (AppConfig) -> Unit
) {
    var openAiKey by remember { mutableStateOf(initialConfig.openAiKey) }

    Dialog(onDismissRequest = { onClose(initialConfig.copy(openAiKey = openAiKey)) }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Settings", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = openAiKey,
                    onValueChange = { openAiKey = it },
                    label = { Text("OpenAI Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { onClose(initialConfig.copy(openAiKey = openAiKey)) }) {
                        Text("Save and Close")
                    }
                }
            }
        }
    }
}

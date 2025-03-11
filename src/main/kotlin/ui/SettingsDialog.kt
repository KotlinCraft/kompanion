package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import config.AppConfig
import config.Provider

data class ValidationError(val message: String)

@Composable
fun ValidationErrorDialog(errors: List<ValidationError>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Validation Errors") },
        text = {
            Column {
                errors.forEach { error ->
                    Text("• ${error.message}")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun SettingsDialog(
    initialConfig: AppConfig,
    onClose: (AppConfig) -> Unit
) {
    val appConfig by remember { mutableStateOf(initialConfig) }
    var openAiKey by remember { mutableStateOf(appConfig.openAiKey) }
    var anthropicKey by remember { mutableStateOf(appConfig.anthropicKey) }
    var currentProvider by remember { mutableStateOf(appConfig.currentProvider) }

    var showValidationErrors by remember { mutableStateOf(false) }
    var validationErrors by remember { mutableStateOf(listOf<ValidationError>()) }

    val scrollState = rememberScrollState()

    fun validateSettings(): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (currentProvider == Provider.ANTHROPIC && anthropicKey.isBlank()) {
            errors.add(ValidationError("Anthropic API key is required when Anthropic is selected as provider"))
        }

        if (currentProvider == Provider.OPENAI && openAiKey.isBlank()) {
            errors.add(ValidationError("OpenAI API key is required when OpenAI is selected as provider"))
        }

        return errors
    }

    fun saveSettings() {
        val errors = validateSettings()
        if (errors.isEmpty()) {
            AppConfig.save(
                appConfig.copy(
                    openAiKey = openAiKey,
                    anthropicKey = anthropicKey,
                    currentProvider = currentProvider
                )
            )
        } else {
            validationErrors = errors
            showValidationErrors = true
        }
    }

    if (showValidationErrors) {
        ValidationErrorDialog(
            errors = validationErrors,
            onDismiss = { showValidationErrors = false }
        )
    }

    Dialog(onDismissRequest = { onClose(appConfig) }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.surface,
            modifier = Modifier.height(600.dp).width(450.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Settings", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(16.dp))

                // Provider Selection
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "AI Provider", style = MaterialTheme.typography.subtitle1)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = currentProvider == Provider.OPENAI,
                                    onClick = { currentProvider = Provider.OPENAI }
                                )
                                Text("OpenAI")
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = currentProvider == Provider.ANTHROPIC,
                                    onClick = { currentProvider = Provider.ANTHROPIC }
                                )
                                Text("Anthropic")
                            }
                        }
                    }
                }

                // Anthropic Section
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Anthropic Configuration", style = MaterialTheme.typography.subtitle1)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = anthropicKey,
                            onValueChange = { anthropicKey = it },
                            label = { Text("Anthropic Key") },
                            placeholder = { Text("Enter your Anthropic API key") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = currentProvider == Provider.ANTHROPIC && anthropicKey.isBlank()
                        )
                        if (currentProvider == Provider.ANTHROPIC && anthropicKey.isBlank()) {
                            Text(
                                "Required when Anthropic is selected",
                                color = MaterialTheme.colors.error,
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "OpenAI Configuration", style = MaterialTheme.typography.subtitle1)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = openAiKey,
                            onValueChange = { openAiKey = it },
                            label = { Text("OpenAI Key") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = currentProvider == Provider.OPENAI && openAiKey.isBlank()
                        )
                        if (currentProvider == Provider.OPENAI && openAiKey.isBlank()) {
                            Text(
                                "Required when OpenAI is selected",
                                color = MaterialTheme.colors.error,
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { saveSettings() }) {
                        Text("Save and Close")
                    }
                }
            }
        }
    }
}
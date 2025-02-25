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

@Composable
fun SettingsDialog(
    initialConfig: AppConfig,
    onClose: (AppConfig) -> Unit
) {
    var openAiKey by remember { mutableStateOf(initialConfig.openAiKey) }
    var anthropicKey by remember { mutableStateOf(initialConfig.anthropicKey) }
    var smallModel by remember { mutableStateOf(initialConfig.model.small) }
    var bigModel by remember { mutableStateOf(initialConfig.model.big) }
    var etherscanBaseApiKey by remember { mutableStateOf(initialConfig.etherscan.baseApiKey) }
    var etherscanEthereumApiKey by remember { mutableStateOf(initialConfig.etherscan.ethereumApiKey) }
    var currentProvider by remember { mutableStateOf(initialConfig.currentProvider) }
    
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = { onClose(initialConfig.copy(openAiKey = openAiKey)) }) {
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
                            modifier = Modifier.fillMaxWidth()
                        )
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
                            value = smallModel,
                            onValueChange = { smallModel = it },
                            label = { Text("Small Model") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = bigModel,
                            onValueChange = { bigModel = it },
                            label = { Text("Big Model") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = openAiKey,
                            onValueChange = { openAiKey = it },
                            label = { Text("OpenAI Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Etherscan Section
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Etherscan Configuration", style = MaterialTheme.typography.subtitle1)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = etherscanBaseApiKey,
                            onValueChange = { etherscanBaseApiKey = it },
                            label = { Text("Base API Key") },
                            placeholder = { Text("Enter your Etherscan Base API key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = etherscanEthereumApiKey,
                            onValueChange = { etherscanEthereumApiKey = it },
                            label = { Text("Ethereum API Key") },
                            placeholder = { Text("Enter your Etherscan Ethereum API key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "These API keys are required for Etherscan blockchain data integration",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = {
                        onClose(
                            initialConfig.copy(
                                openAiKey = openAiKey,
                                model = initialConfig.model.copy(small = smallModel, big = bigModel),
                                anthropicKey = anthropicKey,
                                etherscan = initialConfig.etherscan.copy(
                                    baseApiKey = etherscanBaseApiKey,
                                    ethereumApiKey = etherscanEthereumApiKey
                                ),
                                currentProvider = currentProvider
                            )
                        )
                    }) {
                        Text("Save and Close")
                    }
                }
            }
        }
    }
}
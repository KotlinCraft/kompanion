package ui

import androidx.compose.material.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Mcp(
    val name: String,
    val command: String,
    val arguments: List<String>,
    val environmentVariables: Map<String, String>
)

@Composable
fun McpScreen(mcpList: List<Mcp>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage MCPs") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mcpList) { mcp ->
                Card(
                    elevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Name: ${mcp.name}",
                            style = MaterialTheme.typography.h6
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Command: ${mcp.command}",
                            style = MaterialTheme.typography.body1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Arguments: ${mcp.arguments.joinToString(", ")}",
                            style = MaterialTheme.typography.body2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Environment Variables:",
                            style = MaterialTheme.typography.subtitle1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        mcp.environmentVariables.forEach { (key, value) ->
                            Text(
                                text = "$key: $value",
                                style = MaterialTheme.typography.body2
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}
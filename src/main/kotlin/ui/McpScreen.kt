package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Mcp(
    val name: String,
    val command: String,
    val arguments: List<String>,
    val environmentVariables: Map<String, String>
)

@Composable
fun McpScreen(mcpList: List<Mcp>) {
    // Theme colors taken from ChatScreen styles
    val darkBackground = Color(0xFF1E1E2E)
    val darkSecondary = Color(0xFF2D2D3F)
    val accentColor = Color(0xFF7289DA)
    
    // Main container using Box and Column to mirror ChatScreen layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar matching ChatScreen's style
            TopAppBar(
                title = {
                    Text(
                        text = "Manage MCPs", 
                        color = Color.White, 
                        fontSize = 20.sp
                    )
                },
                backgroundColor = darkSecondary,
                contentColor = accentColor,
                elevation = 4.dp
            )
            // List of MCP items with consistent spacing and styling
            LazyColumn(
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
                        Column(
                            modifier = Modifier
                                .background(darkSecondary)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Name: ${mcp.name}",
                                style = MaterialTheme.typography.h6,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Command: ${mcp.command}",
                                style = MaterialTheme.typography.body1,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Arguments: ${mcp.arguments.joinToString(", ")}",
                                style = MaterialTheme.typography.body2,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Environment Variables:",
                                style = MaterialTheme.typography.subtitle1,
                                color = accentColor,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            mcp.environmentVariables.forEach { (key, value) ->
                                Text(
                                    text = "$key: $value",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import agent.modes.Mode
import agent.tool.ToolAllowedStatus
import kotlinx.coroutines.runBlocking

/**
 * A component that displays the number of available tools and shows a tooltip with tool details when clicked.
 */
@Composable
fun ToolCounter(
    accentColor: Color,
    backgroundColor: Color,
    activeMode: Mode? = null
) {
    // Get tool names from the active mode
    val toolNames = remember(activeMode) {
        if (activeMode != null) {
            runBlocking { activeMode.getLoadedTools() }
        } else {
            emptyList()
        }
    }

    // Tool count is the size of the tool names list
    val toolCount = toolNames.size

    var showTooltip by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.2f))
                .clickable { showTooltip = !showTooltip }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = "Available Tools",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$toolCount Tools",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Tooltip popup with tools list
            if (showTooltip) {
                Popup(
                    alignment = Alignment.TopEnd,
                    properties = PopupProperties(
                        focusable = true,
                        dismissOnClickOutside = true
                    ),
                    onDismissRequest = { showTooltip = false }
                ) {
                    Surface(
                        color = backgroundColor,
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(8.dp)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyUp) {
                                    showTooltip = false
                                    true
                                } else {
                                    false
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).widthIn(max = 300.dp)
                        ) {
                            Text(
                                text = "Available Tools",
                                color = accentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (toolNames.isEmpty()) {
                                Text(
                                    text = "No tools available for the current mode",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            } else {
                                toolNames.forEach { tool ->
                                    // Status color based on the tool's allowed status
                                    val statusColor = when (tool.allowedStatus) {
                                        ToolAllowedStatus.ALLOWED -> Color.Green
                                        ToolAllowedStatus.NOT_ALLOWED -> Color.Red
                                        null -> Color.Gray
                                    }
                                    
                                    // Status text based on the tool's allowed status
                                    val statusText = when (tool.allowedStatus) {
                                        ToolAllowedStatus.ALLOWED -> "Allowed"
                                        ToolAllowedStatus.NOT_ALLOWED -> "Not Allowed"
                                        null -> "Undefined"
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        // Status indicator dot
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(statusColor, RoundedCornerShape(4.dp))
                                        )
                                        
                                        Spacer(modifier = Modifier.width(6.dp))
                                        
                                        // Tool name and status
                                        Column {
                                            Text(
                                                text = tool.name,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
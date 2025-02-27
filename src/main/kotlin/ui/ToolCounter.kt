package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

/**
 * A component that displays the number of available tools and shows a tooltip with tool details when clicked.
 */
@Composable
fun ToolCounter(
    accentColor: Color,
    backgroundColor: Color
) {
    // Hardcoded number of tools (can be replaced with actual count later)
    val toolCount = 8
    
    // Hardcoded list of tool names (replace with actual tools later)
    val toolNames = listOf(
        "fetch", 
        "intellij_get_context", 
        "get_file_content", 
        "write_file", 
        "search_files", 
        "run_command", 
        "list_directory", 
        "intellij_editor_content"
    )
    
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
                            
                            toolNames.forEach { toolName ->
                                Text(
                                    text = "• $toolName",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
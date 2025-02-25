package ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun InfoTooltip(
    accentColor: Color,
    backgroundColor: Color
) {
    val messages by remember { derivedStateOf { InfoManager.getInfoMessages() } }
    var showTooltip by remember { mutableStateOf(false) }
    
    // Hide tooltip if no messages exist
    LaunchedEffect(messages) {
        if (messages.isEmpty()) {
            showTooltip = false
        }
    }
    
    // Only show the info icon if we have messages
    if (messages.isNotEmpty()) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f))
                .clickable { showTooltip = !showTooltip }
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Information",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            
            // Tooltip popup
            if (showTooltip) {
                Popup(
                    alignment = Alignment.BottomEnd,
                    // This enables the popup to be dismissed when clicked outside
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
                            // Add key event handling for Escape key
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
                            messages.entries.forEachIndexed { index, (id, message) ->
                                Text(
                                    text = message,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(4.dp)
                                )
                                
                                // Add separator between messages
                                if (index < messages.size - 1) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .padding(vertical = 4.dp)
                                            .background(Color.Gray.copy(alpha = 0.3f))
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
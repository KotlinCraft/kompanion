package ui.chat

import agent.interaction.ToolStatus
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GetProxyIndicator(
    contract: String,
    network: String,
    status: ToolStatus,
    implementation: String? = null,
    error: String? = null,
) {
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(status == ToolStatus.RUNNING) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = when (status) {
                            ToolStatus.RUNNING -> listOf(Color(0xFF2D3349).copy(alpha = 0.7f), Color(0xFF1F2937).copy(alpha = 0.8f))
                            ToolStatus.COMPLETED -> listOf(Color(0xFF25313D).copy(alpha = 0.6f), Color(0xFF1F2937).copy(alpha = 0.8f))
                            ToolStatus.FAILED -> listOf(Color(0xFF3D2525).copy(alpha = 0.6f), Color(0xFF372525).copy(alpha = 0.8f))
                        }
                    )
                )
                .border(
                    width = 3.dp,
                    color = when (status) {
                        ToolStatus.RUNNING -> Color(0xFFF59E0B)
                        ToolStatus.COMPLETED -> Color(0xFF10B981)
                        ToolStatus.FAILED -> Color(0xFFEF4444)
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            // Header row with collapse/expand toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // B Logo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color.Black, RoundedCornerShape(2.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = "B",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(2.dp)
                                .background(Color.Red)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Bankless: Retrieving proxy implementation",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Status indicator moved to be immediately left of the collapse indicator
                when (status) {
                    ToolStatus.RUNNING -> {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp),
                            color = Color(0xFFF59E0B)
                        )
                    }
                    ToolStatus.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    ToolStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Collapse/Expand toggle button
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { isExpanded = !isExpanded }
                )
            }
            
            // Content with animation for collapsing/expanding
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Content row - Contract and network
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "for contract ",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        
                        // Contract address display with copy button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .clickable { 
                                    clipboardManager.setText(AnnotatedString(contract))
                                }
                        ) {
                            Text(
                                text = contract,
                                color = Color(0xFF8DD5C8),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy contract address",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        Text(
                            text = "on network",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        
                        // Network badge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF4F46E5).copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF818CF8), shape = RoundedCornerShape(50))
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                Text(
                                    text = network,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF818CF8)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    // Show implementation or error if available
                    if (status == ToolStatus.COMPLETED && implementation != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Implementation:",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.3f))
                                .padding(8.dp)
                                .clickable { 
                                    clipboardManager.setText(AnnotatedString(implementation))
                                }
                        ) {
                            Text(
                                text = implementation,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy implementation address",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else if (status == ToolStatus.FAILED && error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.15f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(12.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = error,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

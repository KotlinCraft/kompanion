package ui.chat

import agent.blockchain.bankless.model.token.FungibleTokenVO
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
fun TokenInformationIndicator(
    address: String,
    network: String,
    status: ToolStatus,
    token: FungibleTokenVO? = null,
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
                    text = "Bankless: Fetching token information",
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
                    
                    // Content row - Address and network
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "for token ",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        
                        // Address display with copy button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .clickable { 
                                    clipboardManager.setText(AnnotatedString(address))
                                }
                        ) {
                            Text(
                                text = address,
                                color = Color(0xFF8DD5C8),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy address",
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
                    
                    // Show token info or error if available
                    if (status == ToolStatus.COMPLETED && token != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.3f))
                                .padding(8.dp)
                        ) {
                            // Token Symbol and Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Token,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                Text(
                                    text = "${token.symbol}",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                if (token.verified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                Text(
                                    text = "·",
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                Text(
                                    text = token.name,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 13.sp
                                )
                            }
                            
                            // Token details
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Text(
                                    text = "Decimals:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                Text(
                                    text = "${token.decimals}",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            // Underlying tokens if any
                            if (token.underlyingTokens.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Underlying Tokens:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                
                                token.underlyingTokens.forEach { underlyingToken ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowRight,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(4.dp))
                                        
                                        Text(
                                            text = "${underlyingToken.symbol} (${underlyingToken.name})",
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 12.sp
                                        )
                                        
                                        if (underlyingToken.verified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Filled.VerifiedUser,
                                                contentDescription = "Verified",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
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

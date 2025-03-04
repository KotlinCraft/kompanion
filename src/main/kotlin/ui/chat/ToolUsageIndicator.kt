package ui.chat

import agent.interaction.ToolStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolUsageIndicator(toolName: String, message: String, status: ToolStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = when (status) {
                        ToolStatus.RUNNING -> Color(0x33A580FF)
                        ToolStatus.COMPLETED -> Color(0x3362A16A)
                        ToolStatus.FAILED -> Color(0x33FF5252)
                    },
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (toolName) {
                        "Contract" -> Icons.Filled.Code
                        "Blockchain" -> Icons.Filled.AccountBalance
                        "API" -> Icons.Filled.Api
                        else -> Icons.Filled.Build
                    },
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${toolName}: ${message}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (status == ToolStatus.RUNNING) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp),
                        color = Color(0xFFA580FF)
                    )
                } else if (status == ToolStatus.COMPLETED) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color(0xFF62A16A),
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
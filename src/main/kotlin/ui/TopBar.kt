import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopBar(
    darkBackground: Color,
    mode: String,
    onSettingsClick: () -> Unit,
    onModeChange: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(darkBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App title
            Text(
                text = "Kompanion",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            // Mode selector
            Card(
                backgroundColor = Color(0xFF2D2D3F),
                shape = RoundedCornerShape(24.dp),
                elevation = 2.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Code mode button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onModeChange("code") }
                            .background(if (mode == "code") Color(0xFF2E6F40) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = "Code mode",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Code",
                                color = Color.White,
                                fontWeight = if (mode == "code") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Ask mode button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onModeChange("ask") }
                            .background(if (mode == "ask") Color(0xFF4A6FD0) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.QuestionAnswer,
                                contentDescription = "Ask mode",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Ask",
                                color = Color.White,
                                fontWeight = if (mode == "ask") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Blockchain mode button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onModeChange("blockchain") }
                            .background(if (mode == "blockchain") Color(0xFF936FBC) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = "Blockchain mode",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Blockchain",
                                color = Color.White,
                                fontWeight = if (mode == "blockchain") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            
            // Settings button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        // Add subtle divider at the bottom
        Divider(
            color = Color.White.copy(alpha = 0.1f),
            thickness = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}
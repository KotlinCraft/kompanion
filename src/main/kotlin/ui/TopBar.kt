import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopBar(
    darkBackground: Color,
    mode: String,
    onSettingsClick: () -> Unit,
    onModeChange: (String) -> Unit = {},
    isProcessing: Boolean = false
) {
    // Set up animation parameters for glisten effect
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animate shimmer effect horizontally across the component
    val shimmerPosition = infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Animate glow intensity
    val glowIntensity = infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Animate elevation for subtle pulsing
    val elevation = infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Define glisten colors based on mode
    val glistenColor1: Color
    val glistenColor2: Color
    
    when (mode) {
        "blockchain" -> {
            // More purple shimmer for blockchain mode
            glistenColor1 = Color(0xFFA374D5) // Deeper purple
            glistenColor2 = Color(0xFF8A4FD0) // Rich purple
        }
        "code" -> {
            // Green shimmer for code mode
            glistenColor1 = Color(0xFF4CAF50) // Medium green
            glistenColor2 = Color(0xFF2E7D32) // Darker green
        }
        else -> {
            // Default shimmer colors (fallback)
            glistenColor1 = Color(0xFF94A6E6) // Light blue-purple
            glistenColor2 = Color(0xFF61DAFB) // Light cyan-blue
        }
    }
    
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
            
            // Mode selector with glisten effect when processing
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isProcessing) {
                            Modifier
                                .shadow(
                                    elevation = elevation.value.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    ambientColor = glistenColor1,
                                    spotColor = glistenColor2
                                )
                        } else {
                            Modifier
                        }
                    )
            ) {
                Card(
                    backgroundColor = Color(0xFF2D2D3F),  // Always use solid background to avoid transparency issues
                    shape = RoundedCornerShape(24.dp),
                    elevation = if (isProcessing) 4.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // This Box handles the glow effect background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isProcessing) {
                                    // Only apply the glow effect background when processing
                                    Modifier.background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF2D2D3F),
                                                Color(0xFF2D2D3F),
                                                glistenColor1.copy(alpha = glowIntensity.value * 0.5f),
                                                glistenColor2.copy(alpha = glowIntensity.value * 0.6f),
                                                glistenColor1.copy(alpha = glowIntensity.value * 0.5f),
                                                Color(0xFF2D2D3F),
                                                Color(0xFF2D2D3F)
                                            ),
                                            // Position the gradient based on the animation value
                                            startX = shimmerPosition.value * 500 - 250,
                                            endX = shimmerPosition.value * 500 + 250
                                        )
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {


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

                            Spacer(modifier = Modifier.width(8.dp))

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
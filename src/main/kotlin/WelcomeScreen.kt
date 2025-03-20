import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.LocalAppResources

@Composable
fun WelcomeScreen(
    onMessageSent: (String) -> Unit
) {
    // Theme colors - same as ChatScreen for consistency
    val darkBackground = Color(0xFF1E1E2E)
    val darkSecondary = Color(0xFF2D2D3F)
    val accentColor = Color(0xFF7289DA)
    val glistenColor1 = Color(0xFF94A6E6)
    val glistenColor2 = Color(0xFF61DAFB)

    // State variable for mode selection ("full-auto" or "code")
    var mode by remember { mutableStateOf("full-auto") }
    var inputText by remember { mutableStateOf("") }

    // Create animation for glisten effect
    val infiniteTransition = rememberInfiniteTransition()

    // Animate the color gradient positions for border
    val gradientPosition = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Animate shadow elevation for a pulsing effect
    val shadowElevation = infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Text shimmer animation with smooth bi-directional movement
    val shimmerOffsetX = infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Function to send the current message
    fun sendCurrentMessage() {
        if (inputText.isNotBlank()) {
            val userMessage = inputText
            inputText = ""
            onMessageSent(userMessage)
        }
    }

    // Main container with TopBar integrated at the top for mode selection
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
    ) {
        Column {
            // TopBar for mode selection (active type: "full-auto" or "code")
            TopBar(
                darkBackground = darkBackground,
                mode = mode,
                onSettingsClick = { /* Settings not applicable in WelcomeScreen */ },
                onModeChange = { newMode -> mode = newMode },
                isProcessing = false
            )
            // Rest of the WelcomeScreen content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                // Display app title
                Text(
                    "Kompanion",
                    color = Color.White,
                    style = MaterialTheme.typography.h4,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Input field with send button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color.White,
                                cursorColor = accentColor,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                backgroundColor = darkSecondary
                            ),
                            placeholder = {
                                // Animated gradient placeholder text
                                Text(
                                    text = "Ask me something...",
                                    style = TextStyle(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Gray.copy(alpha = 0.3f),
                                                Color.Gray.copy(alpha = 0.5f),
                                                glistenColor1.copy(alpha = 0.7f),
                                                glistenColor2,
                                                glistenColor1.copy(alpha = 0.7f),
                                                Color.Gray.copy(alpha = 0.5f),
                                                Color.Gray.copy(alpha = 0.3f)
                                            ),
                                            start = Offset(shimmerOffsetX.value - 150f, 0f),
                                            end = Offset(shimmerOffsetX.value + 150f, 0f)
                                        )
                                    ),
                                    fontSize = 16.sp
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = shadowElevation.value.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    ambientColor = glistenColor1,
                                    spotColor = glistenColor2
                                )
                                .border(
                                    width = 2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(glistenColor1, glistenColor2, glistenColor1),
                                        start = Offset(gradientPosition.value * 200, 0f),
                                        end = Offset(gradientPosition.value * 200 + 100, 100f)
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .onKeyEvent { event ->
                                    if (event.key == Key.Enter && !event.isShiftPressed && event.type == KeyEventType.KeyUp) {
                                        sendCurrentMessage()
                                        true
                                    } else false
                                }
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3A3A3A))
                            .clickable { sendCurrentMessage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Press Enter to send",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
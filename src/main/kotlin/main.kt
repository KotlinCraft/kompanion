import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import common.LocalAppResources
import common.rememberAppResources

@OptIn(ExperimentalAnimationApi::class)
fun main() = application {
    // Initialize InfoManager and check for configuration issues
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kompanion"
    ) {
        CompositionLocalProvider(LocalAppResources provides rememberAppResources()) {
            // Track application state - start with welcome screen
            var showChatScreen by remember { mutableStateOf(false) }
            var initialMessage by remember { mutableStateOf("") }
            
            // Use conditional rendering with animation transition
            AnimatedContent(
                targetState = showChatScreen,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(500)) with 
                    fadeOut(animationSpec = tween(500))).using(
                        SizeTransform(clip = false)
                    )
                }
            ) { isShowingChatScreen ->
                if (isShowingChatScreen) {
                    // Pass initial message to ChatScreen
                    ChatScreenWithInitialMessage(initialMessage)
                } else {
                    // Show welcome screen and handle first message
                    WelcomeScreen { message ->
                        initialMessage = message
                        showChatScreen = true
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreenWithInitialMessage(initialMessage: String) {
    // This will ensure the initial message is processed when the ChatScreen is first displayed
    LaunchedEffect(Unit) {
        // The actual implementation will depend on how we access the ChatScreen's internals
        // This will be handled in the ChatScreen component itself
    }
    
    ChatScreen(initialMessage)
}
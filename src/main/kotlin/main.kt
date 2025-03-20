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
            ChatScreen()
        }
    }
}
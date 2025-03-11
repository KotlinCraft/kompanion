import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import common.LocalAppResources
import common.rememberAppResources
import ui.info.InfoManager

fun main() = application {
    // Initialize InfoManager and check for configuration issues
    Window(
        onCloseRequest = ::exitApplication,
        title = "AI Code Companion"
    ) {
        CompositionLocalProvider(LocalAppResources provides rememberAppResources()) {
            ChatScreen()
        }
    }
}

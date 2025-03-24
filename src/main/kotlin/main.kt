import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import common.LocalAppResources
import common.rememberAppResources
import ui.Mcp
import ui.McpScreen

@OptIn(ExperimentalAnimationApi::class)
fun main() = application {
    // Initialize InfoManager and check for configuration issues
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kompanion"
    ) {
        CompositionLocalProvider(LocalAppResources provides rememberAppResources()) {
            ChatScreen()
            /*McpScreen(
                listOf(
                    Mcp(
                        name = "MCP 1",
                        command = "mcp1",
                        arguments = listOf("arg1", "arg2"),
                        environmentVariables = mapOf("key1" to "value1", "key2" to "value2")
                    ))
                )
            ) */
        }
    }
}
package ui.chat

import agent.interaction.ToolStatus
import androidx.compose.runtime.Composable

open class ChatMessage(
    val content: String,
    val isUser: Boolean
) {
    @Composable
    open fun constructChatBubble() {
        MessageBubble(this)
    }
}

class ToolMessage(
    content: String,
    val toolIndicator: @Composable () -> Unit
) : ChatMessage(content, false) {

    @Composable
    override fun constructChatBubble() {
        toolIndicator()
    }
}


package ui.chat

import agent.interaction.ToolStatus
import androidx.compose.runtime.Composable
import java.util.*

open class ChatMessage(
    val id: UUID,
    val content: String,
    val isUser: Boolean
) {
    @Composable
    open fun constructChatBubble() {
        MessageBubble(this)
    }
}

class ToolMessage(
    id: UUID,
    content: String,
    val toolIndicator: @Composable () -> Unit
) : ChatMessage(id, content, false) {

    @Composable
    override fun constructChatBubble() {
        toolIndicator()
    }
}


package agent.modes

import agent.interaction.*
import androidx.compose.runtime.Composable
import ui.chat.ToolUsageIndicator

interface Interactor {

    suspend fun sendMessage(message: String) {
        interactionHandler().interact(AgentResponse(message))
    }

    suspend fun askUser(question: String): String {
        return interactionHandler().interact(AgentQuestion(question))
    }

    suspend fun toolUsage(
        toolName: String,
        status: ToolStatus,
        message: String = "",
        toolIndicator: @Composable () -> Unit = { ToolUsageIndicator(toolName, message, status) }
    ): String {
        return interactionHandler().interact(
            ToolUsageMessage(
                action = message,
                status = status,
                toolIndicator = toolIndicator
            )
        )
    }

    suspend fun confirmWithUser(message: String): Boolean {
        while (true) {
            val response = interactionHandler().interact(AgentAskConfirmation(message))
            when (response.trim().uppercase()) {
                "Y", "YES" -> return true
                "N", "NO" -> return false
                else -> sendMessage("Invalid response. Please answer with Y or N.")
            }
        }
    }

    fun interactionHandler(): InteractionHandler
}
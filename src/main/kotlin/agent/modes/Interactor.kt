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

    suspend fun defaultToolUsage(
        toolName: String,
        status: ToolStatus,
        message: String
    ) {
        interactionHandler().interact(
            ToolUsageMessage(
                action = message,
                toolIndicator = { ToolUsageIndicator(toolName, message, status) }
            )
        )
    }

    suspend fun customToolUsage(
        message: String = "",
        toolIndicator: @Composable () -> Unit
    ): String {
        return interactionHandler().interact(
            ToolUsageMessage(toolIndicator = toolIndicator)
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
package agent.modes

import agent.interaction.*
import androidx.compose.runtime.Composable
import ui.chat.ToolUsageIndicator
import java.util.UUID

interface Interactor {

    suspend fun sendMessage(message: String) {
        interactionHandler().interact(AgentResponse(message))
    }

    suspend fun askUser(question: String): String {
        return interactionHandler().interact(AgentQuestion(question))
    }

    suspend fun defaultToolUsage(
        id: UUID = UUID.randomUUID(),
        toolName: String,
        status: ToolStatus,
        message: String
    ) : UUID {
        interactionHandler().interact(
            ToolUsageMessage(
                id = id,
                action = message,
                toolIndicator = { ToolUsageIndicator(toolName, message, status) }
            )
        )
        return id
    }

    suspend fun customToolUsage(
        id: UUID = UUID.randomUUID(),
        message: String = "",
        toolIndicator: @Composable () -> Unit
    ): UUID {
        interactionHandler().interact(
            ToolUsageMessage(
                id = id,
                toolIndicator = toolIndicator
            )
        )
        return id
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
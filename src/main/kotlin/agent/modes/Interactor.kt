package agent.modes

import agent.interaction.AgentAskConfirmation
import agent.interaction.AgentQuestion
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler

interface Interactor {

    suspend fun sendMessage(message: String) {
        interactionHandler().interact(AgentResponse(message))
    }

    suspend fun askUser(question: String): String {
        return interactionHandler().interact(AgentQuestion(question))
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
package agent

import agent.interaction.InteractionHandler
import agent.modes.Interactor
import agent.modes.Mode

class Agent internal constructor(
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
    val mode: Mode
) : Interactor {

    fun fetchContextManager(): ContextManager {
        return contextManager
    }

    suspend fun perform(request: String): String {
        // Store user message in context manager first
        (contextManager as? InMemoryContextManager)?.addUserMessage(request) 
            ?: contextManager.storeMessage("User: $request")
        
        // Perform the action
        val response = mode.perform(request)
        
        // Store agent response in context manager
        (contextManager as? InMemoryContextManager)?.addAgentMessage(response)
            ?: contextManager.storeMessage("Kompanion: $response")
        
        return response
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}

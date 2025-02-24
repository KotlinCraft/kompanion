package agent

import agent.interaction.AgentQuestion
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import agent.traits.Analyst
import agent.traits.Coder

class CodingAgent internal constructor(
    private val contextManager: ContextManager,
    coder: Coder,
    analyst: Analyst
) : CodeAgent, Coder by coder, Analyst by analyst {

    lateinit var interactionHandler: InteractionHandler

    override fun registerHandler(interactionHandler: InteractionHandler) {
        this.interactionHandler = interactionHandler
    }

    override fun fetchContextManager(): ContextManager {
        return contextManager
    }

    override suspend fun sendMessage(message: String) {
        interactionHandler.interact(AgentResponse(message))
    }

    override suspend fun askUser(question: String): String {
        return interactionHandler.interact(AgentQuestion(question))
    }
}

package agent

import agent.domain.UserFeedback
import agent.interaction.AgentQuestion
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler

class CodingAgent internal constructor(
    reasoner: Reasoner,
    codeGenerator: CodeGenerator,
    private val contextManager: ContextManager
) : CodeAgent, AutomatedCoder(
    reasoner, codeGenerator
) {

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

    override suspend fun askQuestion(question: String): String {
        return interactionHandler.interact(AgentQuestion(question))
    }

    override suspend fun addFeedback(feedback: UserFeedback) {
        reasoner.learn(feedback)
    }
}

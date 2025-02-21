package agent

import agent.domain.CodingAgentResponse
import agent.domain.UserFeedback
import agent.domain.UserRequest
import agent.interaction.InteractionHandler

interface CodeAgent {
    suspend fun process(request: UserRequest): CodingAgentResponse
    suspend fun addFeedback(feedback: UserFeedback)
    fun registerHandler(interactionHandler: InteractionHandler)
    fun fetchContextManager(): ContextManager
}
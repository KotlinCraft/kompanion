package chat

import agent.CodeAgent
import agent.domain.UserRequest
import agent.interaction.AgentMessage
import agent.interaction.InteractionHandler

open class ChatBot(
    val agent: CodeAgent,
    private val onMessage: suspend ((message: AgentMessage) -> String)
) : InteractionHandler {

    init {
        agent.registerHandler(this)
    }

    open suspend fun handleMessage(
        message: String
    ): String {
        // Process the request
        val response = agent.processCodingRequest(
            UserRequest(
                instruction = message,
            )
        )

        // Format response for chat
        return """ℹ️ ${response.explanation}""".trimIndent()
    }

    override suspend fun interact(agentMessage: AgentMessage): String {
        return onMessage(agentMessage)
    }
}

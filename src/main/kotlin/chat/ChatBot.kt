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

    open suspend fun codingRequest(message: String): String {
        val response = agent.processCodingRequest(UserRequest(message))
        return """ℹ️ ${response.explanation}""".trimIndent()
    }

    open suspend fun codeBaseQuestion(message: String): String {
        val response = agent.askQuestion(message)
        return """ℹ️ ${response.reply}""".trimIndent()
    }

    override suspend fun interact(agentMessage: AgentMessage): String {
        return onMessage(agentMessage)
    }
}

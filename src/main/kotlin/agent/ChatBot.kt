package agent

import agent.domain.UserRequest
import agent.interaction.AgentMessage

open class ChatBot(
    private val agent: CodeAgent,
    private val onMessage: suspend ((message: AgentMessage) -> String)
) : AgentMessageCallback {
    init {
        if (agent is CodingAgent) {
            agent.setMessageCallback(this)
        }
    }

    override suspend fun onAgentInteraction(message: AgentMessage): String {
        return onMessage(message)
    }

    open suspend fun handleMessage(
        message: String
    ): String {
        // Process the request
        val response = agent.process(
            UserRequest(
                instruction = message,
            )
        )

        // Format response for chat
        return """
                Generated Code:
                ```
                
                Explanation:
                ${response.explanation}
                
                Next Steps:
                ${response.nextSteps.joinToString("\n")}
                
                Confidence: ${response.confidence * 100}%
            """.trimIndent()
    }
}

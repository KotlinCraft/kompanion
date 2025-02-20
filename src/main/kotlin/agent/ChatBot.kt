package agent

import agent.domain.UserRequest
import agent.interaction.AgentMessage
import agent.interaction.AgentQuestion
import agent.interaction.AgentResponse

open class ChatBot(
    private val agent: CodeAgent,
    private val onMessage: ((String) -> Unit)? = null
) : AgentMessageCallback {
    init {
        if (agent is CodingAgent) {
            agent.setMessageCallback(this)
        }
    }

    override fun onMessage(message: AgentMessage) {
        when (message) {
            is AgentResponse -> {
                onMessage?.invoke(message.message)
            }

            is AgentQuestion -> {
                onMessage?.invoke(message.message)
            }
        }
    }

    open suspend fun handleMessage(
        message: String,
        onMessage: (String) -> Unit,
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

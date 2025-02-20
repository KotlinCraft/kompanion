package agent

import agent.domain.UserRequest

open class ChatBot(
    private val agent: CodeAgent,
    private val onMessage: ((String) -> Unit)? = null
) : AgentMessageCallback {
    init {
        if (agent is CodingAgent) {
            agent.setMessageCallback(this)

        }
    }

    override fun onMessage(message: String) {
        onMessage?.invoke(message)
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

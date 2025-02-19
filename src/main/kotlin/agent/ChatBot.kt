package agent

import agent.domain.UserRequest

class ChatBot(private val agent: CodeAgent) : AgentMessageCallback {
    init {
        if (agent is CodeGenerationAgent) {
            agent.setMessageCallback(this)
        }
    }

    override fun onMessage(message: String) {
        // TODO: In a real implementation, this would need to handle async message display
        println("Agent: $message")
    }

    suspend fun handleMessage(message: String): String {
        // Update context with any attached files

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

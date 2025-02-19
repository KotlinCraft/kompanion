package agent

import agent.domain.UserRequest
import kotlinx.coroutines.delay

open class ChatBot(
    private val agent: CodeAgent,
    private val onMessage: ((String) -> Unit)? = null
) : AgentMessageCallback {
    init {
        if (agent is CodeGenerationAgent) {
            agent.setMessageCallback(this)

        }
    }

    override fun onMessage(message: String) {
        onMessage?.invoke(message)
    }

    open suspend fun handleMessage(
        message: String,
        onResponse: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            // Process the request
            val response = agent.process(
                UserRequest(
                    instruction = message,
                )
            )

            // Format response for chat
            val formattedResponse = """
                Generated Code:
                ```
                
                Explanation:
                ${response.explanation}
                
                Next Steps:
                ${response.nextSteps.joinToString("\n")}
                
                Confidence: ${response.confidence * 100}%
            """.trimIndent()
            
            onResponse(formattedResponse)
        } catch (e: Exception) {
            onError(e)
        }
    }
}

package agent

import agent.domain.CodingAgentResponse
import agent.domain.UserFeedback
import agent.domain.UserRequest
import agent.interaction.InteractionHandler

interface CodeAgent {
    suspend fun processCodingRequest(request: UserRequest): CodingAgentResponse
    suspend fun addFeedback(feedback: UserFeedback)
    fun registerHandler(interactionHandler: InteractionHandler)
    fun fetchContextManager(): ContextManager
    suspend fun sendMessage(message: String)
    suspend fun askQuestion(question: String): String


    suspend fun confirmWithUser(message: String): Boolean {
        while (true) {
            val response = askQuestion("$message\nPlease respond with Y or N:")
            when (response.trim().uppercase()) {
                "Y" -> return true
                "N" -> return false
                else -> sendMessage("Invalid response. Please answer with Y or N.")
            }
        }
    }
}
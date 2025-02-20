package agent

import agent.domain.UserFeedback
import agent.domain.UserRequest
import agent.interaction.AgentMessage
import agent.interaction.AgentQuestion
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import kotlinx.coroutines.delay

class FakeChatBot(onMessage: suspend ((AgentMessage) -> String)) : ChatBot(FakeAgent(), onMessage) {
    override suspend fun handleMessage(
        message: String,
    ): String {
        delay(2000) // Simulate some initial processing
        interact(AgentResponse("Starting to process your request..."))

        delay(2000) // Simulate more processing
        interact(AgentResponse("Analyzing code patterns..."))

        delay(2000) // Final delay before response

        return """
            Generated Code:
            ```kotlin
            fun example() {
                println("This is fake generated code")
            }
            ```
            
            Explanation:
            This is a fake response for testing purposes.
            
            Next Steps:
            1. Review the generated code
            2. Test the implementation
            3. Consider edge cases
            
            Confidence: 95%
        """.trimIndent()
    }

    private class FakeAgent : CodeAgent {
        override suspend fun process(request: UserRequest) = throw UnsupportedOperationException()
        override suspend fun addFeedback(feedback: UserFeedback) = throw UnsupportedOperationException()
        override fun registerHandler(interactionHandler: InteractionHandler) {
            //don't do anything
        }
    }
}
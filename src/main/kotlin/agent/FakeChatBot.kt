package agent

import agent.coding.domain.CodingResult
import agent.domain.*
import agent.interaction.AgentMessage
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import agent.reason.Reasoner
import chat.ChatBot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

class FakeChatBot(onMessage: suspend ((AgentMessage) -> String)) : ChatBot(FakeAgent(), onMessage) {
    override suspend fun codingRequest(
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

    class FakeContextManager : ContextManager {
        override fun getContext(): StateFlow<Set<CodeFile>> {
            TODO("Not yet implemented")
        }

        override fun updateFiles(files: List<CodeFile>) {
            TODO("Not yet implemented")
        }

        override fun clearContext() {
            TODO("Not yet implemented")
        }

        override fun fetchWorkingDirectory(): String {
            TODO("Not yet implemented")
        }

        override fun getFullFileList(): String {
            TODO("Not yet implemented")
        }
    }

    class FakeReasoner(): Reasoner {
        override suspend fun analyzeRequest(request: UserRequest): Understanding {
            TODO("Not yet implemented")
        }

        override suspend fun createPlan(understanding: Understanding): GenerationPlan {
            TODO("Not yet implemented")
        }

        override suspend fun evaluateCode(result: GenerationResult, understanding: Understanding): CodeEvaluation {
            TODO("Not yet implemented")
        }

        override suspend fun askQuestion(question: String, understanding: Understanding): CodebaseQuestionResponse {
            TODO("Not yet implemented")
        }
    }

    class FakeCodeGenerator: CodeGenerator {
        override suspend fun generate(plan: GenerationPlan, currentCode: String): GenerationResult {
            TODO("Not yet implemented")
        }

        override suspend fun execute(plan: GenerationPlan, generationResult: GenerationResult): CodingResult {
            TODO("Not yet implemented")
        }
    }

    private class FakeAgent : CodeAgent(
        FakeContextManager(),
        FakeReasoner(),
        FakeCodeGenerator()
    ){
        override suspend fun processCodingRequest(request: UserRequest) = throw UnsupportedOperationException()

        override suspend fun sendMessage(message: String) {
            TODO("Not yet implemented")
        }

        override suspend fun askUser(question: String): String {
            TODO("Not yet implemented")
        }

        override suspend fun askQuestion(question: String): CodebaseQuestionResponse {
            TODO("Not yet implemented")
        }
    }
}
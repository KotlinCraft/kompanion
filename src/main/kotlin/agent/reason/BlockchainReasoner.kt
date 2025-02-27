package agent.reason

import agent.ContextManager
import agent.domain.CodebaseQuestionResponse
import ai.Action
import ai.LLMProvider
import org.springframework.core.ParameterizedTypeReference

class BlockchainReasoner(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager
) {

    suspend fun askQuestion(
        question: String,
        actions: List<Action>
    ): CodebaseQuestionResponse {
        val prompt = """
            The user asked a question about an onchain related topic:
            $question
            
            Provide the best possible answer based on the context you have.
            
            Return your answer as a simple string and include the confidence level (0-1) of your answer.
        """.trimIndent()

        // Attempt to leverage the same LLM approach used in DefaultReasoner, if available
        return LLMProvider.prompt(
            input = prompt,
            actions = actions,
            temperature = 0.3,
            parameterizedTypeReference = object : ParameterizedTypeReference<CodebaseQuestionResponse>() {})
    }
}

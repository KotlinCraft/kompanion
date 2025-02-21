package agent

import agent.domain.UserRequest
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference

class Analyst(
    private val reasoner: Reasoner,
    private val contextManager: ContextManager
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Answers a user question about the codebase using the Reasoner and current context.
     *
     * Internally leverages the same LLM approach as DefaultReasoner, but
     * does not perform code generation. Instead, it focuses on providing textual
     * explanations or insights about the codebase's content.
     *
     * @param question The user's question about the codebase
     * @return A text answer, possibly requesting more file context if needed.
     */
    suspend fun answerQuestion(question: String): String {
        logger.info("Received question: $question")
        val request = UserRequest(question)
        val understanding = reasoner.analyzeRequest(request)

        val prompt = """
            ${contextManager.currentContextPrompt()}
            
            The user asked a question about the codebase:
            $question
            
            Provide the best possible answer based on the context you have. 
            If you need more context about any file, you can request it with "request_file_context".
            
            Return your answer as a simple string.
        """.trimIndent()

        // Attempt to leverage the same LLM approach used in DefaultReasoner, if available
        val answer = (reasoner as? DefaultReasoner)?.let { defaultReasoner ->
            defaultReasoner.LLMProvider.prompt(
                input = prompt,
                actions = emptyList(),
                temperature = 0.3,
                parameterizedTypeReference = object : ParameterizedTypeReference<String>() {}
            )
        } ?: "LLM-based reasoning not available. Please implement an LLM call or fallback mechanism."

        return answer ?: "No answer returned from LLM."
    }
}

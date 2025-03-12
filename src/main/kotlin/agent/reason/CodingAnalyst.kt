package agent.reason

import agent.ContextManager
import agent.ToolManager
import agent.domain.Understanding
import ai.LLMProvider
import arrow.core.Either
import arrow.core.getOrElse
import org.springframework.core.ParameterizedTypeReference

class CodingAnalyst(
    private val contextManager: ContextManager,
    private val llmProvider: LLMProvider,
    private val toolManager: ToolManager
) {

    suspend fun analyzeRequest(request: String): Understanding {
        val prompt = """
            ${contextManager.currentContextPrompt(true)}
            
            Analyze the following code-related request and extract key information.
            The content for various files (but not all) might be provided for context. 
            If you think a file already exists but its contents was not provided yet, request it using "request_file_context". 
            Only attempt to read files you could find in the outline and use their absolute path like you found them in the outline.
            
            important: We cannot use files that are not in our context yet.

            Make sure you have access to every file mentioned in the request before continuing. Navigate the context and files to come up with the best possible answer.
            
            Provide a structured analysis including:
            1. The main objective
            2. Required features or changes
            3. Relevance score (0.0-1.0) for each provided context file
        """.trimIndent()

        return Either.catch {
            llmProvider.prompt(
                system = prompt,
                userMessage = request,
                actions = toolManager.tools.map { it.toolCallback },
                temperature = 0.7,
                parameterizedTypeReference = object : ParameterizedTypeReference<Understanding>() {},
            )
        }.getOrElse {
            it.printStackTrace()
            throw IllegalArgumentException("I'm afraid I was unable to analyze your request.")
        }
    }
}
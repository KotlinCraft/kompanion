package agent.reason

import agent.ContextManager
import agent.ToolManager
import agent.domain.CodebaseQuestionResponse
import ai.LLMProvider
import org.springframework.core.ParameterizedTypeReference

class AutoModeReasoner(
    private val LLMProvider: LLMProvider,
    private val toolManager: ToolManager,
    private val contextManager: ContextManager
) {

    suspend fun askQuestion(
        question: String
    ): CodebaseQuestionResponse {
        val prompt = """
            ROLE:
• You are Kompanion, an amazing analyst and orchestrator. 

CONTEXT:
• Your context already includes: ${contextManager.currentContextPrompt(false)}  

FINAL INSTRUCTION:
• Provide the best possible, concise answer to the user’s request. If it's not an immediate question but an instruction, follow it directly.
• Use your tools to gather any necessary clarifications or data.  
• Offer a clear, direct response and add a summary of what you did at the end.
        """.trimIndent()

        // Attempt to leverage the same LLM approach used in DefaultReasoner, if available
        return LLMProvider.prompt(
            system = prompt,
            userMessage = question,
            actions = toolManager.tools.map { it.toolCallback },
            temperature = 0.7,
            parameterizedTypeReference = object : ParameterizedTypeReference<CodebaseQuestionResponse>() {},
        )
    }
}

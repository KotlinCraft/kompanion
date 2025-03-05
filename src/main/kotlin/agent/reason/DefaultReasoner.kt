package agent.reason

import agent.ContextManager
import agent.InMemoryContextManager
import agent.ToolManager
import agent.domain.CodebaseQuestionResponse
import agent.domain.GenerationPlan
import agent.domain.Understanding
import ai.LLMProvider
import arrow.core.Either
import arrow.core.getOrElse
import org.springframework.core.ParameterizedTypeReference

class DefaultReasoner(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager,
    private val toolManager: ToolManager,
) : Reasoner {

    override suspend fun analyzeRequest(request: String): Understanding {
        val prompt = """
            ${contextManager.currentContextPrompt(true)}
            
            ${getMessageHistoryPrompt()}
            
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
            LLMProvider.prompt(
                system = prompt,
                userMessage = request,
                actions = toolManager.tools.map { it.toolCallback },
                temperature = 0.7,
                parameterizedTypeReference = object : ParameterizedTypeReference<Understanding>() {},
                toolcallbacks = toolManager.toolCallbacks
            )
        }.getOrElse {
            it.printStackTrace()
            throw IllegalArgumentException("I'm afraid I was unable to analyze your request.")
        }
    }

    override suspend fun createPlan(understanding: Understanding): GenerationPlan {
        val prompt = """
            ${contextManager.currentContextPrompt(true)}
            
            Based on the following understanding of a code request, create a detailed generation plan.
            
            Objective: ${understanding.objective}
            Required Features: 
            ${understanding.requiredFeatures.joinToString("\n") { "- $it" }}
            Context Relevance:
            ${understanding.contextRelevance.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}
            
            Create a detailed plan with the following structure:
            1. A list of specific implementation steps, each containing:
               - The action to perform
               - Required inputs
               - Expected output
            2. A clear description of the expected final outcome
            3. A list of specific validation criteria to verify the implementation
            4. Ask questions if things are unclear.
            
            Ensure the response is structured to match:
            {
              "steps": [
                {
                  "action": "string describing the step",
                  "input": {key-value map of inputs needed},
                  "expectedOutput": "string describing expected output"
                }
              ],
              "expectedOutcome": "detailed description of final result",
              "validationCriteria": ["list", "of", "verification", "points"]
            }
        """.trimIndent()

        return LLMProvider.prompt(
            system = prompt,
            userMessage = null,
            actions = toolManager.tools.map { it.toolCallback },
            toolcallbacks = toolManager.toolCallbacks,
            temperature = 0.5,
            parameterizedTypeReference = object : ParameterizedTypeReference<GenerationPlan>() {})
    }

    override suspend fun askQuestion(
        question: String,
        understanding: Understanding,
    ): CodebaseQuestionResponse {


        val prompt = """
            ${contextManager.currentContextPrompt(true)}
            
            ${getMessageHistoryPrompt()}
            
            $question
            
            Objective: ${understanding.objective}
            Required Features: 
            ${understanding.requiredFeatures.joinToString("\n") { "- $it" }}
            Context Relevance:
            ${understanding.contextRelevance.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}
            
            Provide the best possible answer based on the context you have.
            If you need the context of any file, you can request it with "request_file_context".
            
            Return your answer as a simple string and include the confidence level (0-1) of your answer.
            
            The user will ask questions related to blockchain contracts.
        """.trimIndent()

        // Attempt to leverage the same LLM approach used in DefaultReasoner, if available
        return LLMProvider.prompt(
            system = prompt,
            userMessage = question,
            actions = toolManager.tools.map { it.toolCallback },
            temperature = 0.3,
            parameterizedTypeReference = object : ParameterizedTypeReference<CodebaseQuestionResponse>() {},
            toolcallbacks = toolManager.toolCallbacks
        )
    }

    /**
     * Get formatted message history for prompts
     */
    private fun getMessageHistoryPrompt(): String {
        // If we have an InMemoryContextManager, use its formatted history
        val formattedHistory = (contextManager as? InMemoryContextManager)?.getFormattedMessageHistory()
            ?: buildMessageHistoryFromContext()

        return if (formattedHistory.isNotEmpty()) {
            """
            Previous conversation history (consider this as context for this continuation):
            $formattedHistory
            """
        } else {
            "" // Empty string if no history
        }
    }

    /**
     * Build message history from context if not using InMemoryContextManager
     */
    private fun buildMessageHistoryFromContext(): String {
        val messages = contextManager.fetchMessages()
        return if (messages.isNotEmpty()) {
            messages.joinToString("\n")
        } else {
            ""
        }
    }
}

package agent.reason

import agent.ContextManager
import agent.ToolManager
import agent.domain.GenerationPlan
import agent.domain.Understanding
import ai.LLMProvider
import org.springframework.core.ParameterizedTypeReference

class SimplePlanner(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager,
    private val toolManager: ToolManager,
)  {

    suspend fun createPlan(request: String, understanding: Understanding): GenerationPlan {
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
            userMessage = request,
            actions = toolManager.tools.map { it.toolCallback },
            temperature = 0.5,
            parameterizedTypeReference = object : ParameterizedTypeReference<GenerationPlan>() {})
    }
}

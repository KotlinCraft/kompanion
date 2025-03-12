package agent.reason

import agent.ContextManager
import agent.ToolManager
import agent.domain.GenerationPlan
import ai.LLMProvider
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference

/**
 * Strategy that applies intermediate reasoning to a generation plan
 * without using tools directly - compatible with "Reasoning LLMs"
 */
class DefaultReasoningStrategy(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager
) : ReasoningStrategy {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun reason(request: String, plan: GenerationPlan): GenerationPlan {
        logger.debug("Applying reasoning to plan: {}", plan)

        val prompt = """
            ${contextManager.currentContextPrompt(true)}
            
            You are an expert developer analyzing and enhancing a generation plan.
            
            ## Original Request:
            $request
            
            ## Current Plan:
            Steps: 
            ${
            plan.steps.joinToString("\n") { step ->
                "- Action: ${step.action}\n  Input: ${step.input}\n  Expected Output: ${step.expectedOutput}"
            }
        }
            
            Expected Outcome:
            ${plan.expectedOutcome}
            
            Validation Criteria:
            ${plan.validationCriteria.joinToString("\n") { "- $it" }}
            
            ## Instructions:
            1. Analyze the current plan and codebase context
            2. Consider potential improvements, edge cases, or missing steps
            3. Refine the plan for better precision and effectiveness
            4. Maintain the same structure in your refined plan
            
            Return an improved version of the plan with the same JSON structure.
        """.trimIndent()

        // No actions/tools passed here - allowing "Reasoning LLMs" to work
        return LLMProvider.prompt(
            system = prompt,
            userMessage = "Please provide an improved generation plan based on the context.",
            temperature = 0.7,
            actions = emptyList(),
            parameterizedTypeReference = object : ParameterizedTypeReference<GenerationPlan>() {}
        )
    }
}

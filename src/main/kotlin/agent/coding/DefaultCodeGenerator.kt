package agent.coding

import agent.CodeGenerator
import agent.ContextManager
import agent.ToolManager
import agent.coding.domain.CodingResult
import agent.domain.GenerationPlan
import ai.LLMProvider
import org.springframework.core.ParameterizedTypeReference

class DefaultCodeGenerator(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager,
    private val toolManager: ToolManager
) : CodeGenerator {

    override suspend fun execute(
        plan: GenerationPlan,
    ): CodingResult {
        val prompt = """
            ${contextManager.currentContextPrompt()}
            
            You're an amazing developer, with many years of experience and a deep understanding of the clean code and architecture.
            Based on the following generation plan you have generated the necessary code changes. 
            Use files in your current context to understand your changes. 
            If the result is not what you expected, you can retry.
            
            Plan Steps:
                        ${
            plan.steps.joinToString("\n") { step ->
                "- Action: ${step.action}\n  Input: ${step.input}\n  Expected Output: ${step.expectedOutput}"
            }
        }
        
            Expected Outcome:
            ${plan.expectedOutcome}
            
            Validation Criteria:
            ${plan.validationCriteria.joinToString("\n") { "- $it" }}
    
            Goal: 
            use your toolcalls to apply the changes to the codebase.
        """.trimIndent()

        return LLMProvider.prompt(
            input = prompt,
            actions = toolManager.tools.map { it.action },
            temperature = 0.5,
            parameterizedTypeReference = object : ParameterizedTypeReference<CodingResult>() {})
    }


}

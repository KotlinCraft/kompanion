package agent.coding

import agent.ContextManager
import agent.ToolManager
import agent.coding.domain.CodingResult
import agent.domain.GenerationPlan
import ai.LLMProvider
import org.springframework.core.ParameterizedTypeReference

class CodeGenerator(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager,
    private val toolManager: ToolManager
) {

    suspend fun execute(
        plan: GenerationPlan,
    ): CodingResult {
        val prompt = """
            ${contextManager.currentContextPrompt()}
            
            You're an amazing developer, with many years of experience and a deep understanding of the clean code and architecture.
            Based on the following generation plan you have generated the necessary code changes. 
            Use files in your current context to understand your changes. 
            If the result is not what you expected, you can retry.
            
            If the user doesn't ask for it specifically, don't add tests.
            
             ## Project Context:
            Based on the files in your current context, you understand the existing code structure and patterns.
            You can still pull in files in order to understand the codebase better.
            Look for similar implementations in the current codebase to maintain consistency.
           
             ## Coding Task:
            Based on the following generation plan, implement the necessary code changes using your tools (by adding and changing files). 
            First explore the codebase to understand the current structure before making changes.
                       
            
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
    
            ## Implementation Approach:
            1. First use the project structure and file search tools to understand the codebase
            2. Implement one change at a time and validate it works correctly
            3. Look for opportunities to improve existing code while preserving functionality
            

    
            Goal: 
            Use the available tools to implement the requested changes to the codebase. Not only provide reasoning, but also perform the changes.
        """.trimIndent()

        return LLMProvider.prompt(
            input = prompt,
            actions = toolManager.tools.map { it.action },
            temperature = 0.5,
            parameterizedTypeReference = object : ParameterizedTypeReference<CodingResult>() {},
            toolcallbacks = toolManager.toolCallbacks
        )
    }
}

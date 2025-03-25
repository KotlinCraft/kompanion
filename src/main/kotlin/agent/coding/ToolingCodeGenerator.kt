package agent.coding

import agent.ContextManager
import agent.ToolManager
import agent.coding.domain.CodingResult
import agent.domain.GenerationPlan
import ai.LLMProvider
import org.springframework.core.ParameterizedTypeReference

class ToolingCodeGenerator(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager,
    private val toolManager: ToolManager
) : CodeGenerator {

    override suspend fun execute(
        request: String,
        plan: GenerationPlan?,
    ): CodingResult {
        val prompt = """
            ${contextManager.currentContextPrompt(true)}
            
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
            plan?.steps?.joinToString("\n") { step ->
                "- Action: ${step.action}\n  Input: ${step.input}\n  Expected Output: ${step.expectedOutput}"
            } ?: ""
        }
        
            Expected Outcome:
            ${plan?.expectedOutcome ?: ""}
            
             Validation Criteria:
            ${plan?.validationCriteria?.joinToString("\n") { "- $it" } ?: ""}
    
            ## Implementation Approach:
            1. First use the project structure and file search tools to understand the codebase
            2. Implement one change at a time and validate it works correctly
            3. Do not change the existing code unless necessary
            4. Verify all imports are correct and all used-libraries are available in the project
            
            ##
            Goal: 
            Use the available tools to implement the requested changes to the codebase. 
            Not only provide reasoning, but also perform the changes.
            Function calling: Always execute the required function calls before you respond.
            If function calls succeeded or its data is useful, add the data to the response.
            
            Afterwards, provide a detailed explanation of the changes you made and how they improve the codebase. 
            Only provide the necessary information to the user and only provide the information when you actually changed code with your tools and they succeeded.
        """.trimIndent()

        return LLMProvider.prompt(
            system = prompt,
            userMessage = request,
            actions = toolManager.tools.map { it.toolCallback },
            temperature = 0.5,
            parameterizedTypeReference = object : ParameterizedTypeReference<CodingResult>() {},
        )
    }
}

package agent

import agent.domain.GenerationPlan
import agent.domain.GenerationResult
import ai.Model
import org.springframework.core.ParameterizedTypeReference

class DefaultCodeGenerator(
    private val model: Model,
    private val contextManager: ContextManager
) : CodeGenerator {


    override suspend fun generate(plan: GenerationPlan, currentCode: String): GenerationResult {
        val context = contextManager.getContext()
        val prompt = """
            Current working directory is: ${contextManager.fetchWorkingDirectory()}

            You're an amazing developer, with many years of experience and a deep understanding of the clean code and architecture.
            
            Files in your current context: 
            ${
            if (context.isEmpty()) "no files in context yet" else
                context.joinToString("\n") {
                    """File: ${it.path} (language: ${it.language})
                    |Content: ${it.content}
                """.trimMargin()
                }
        }
            
            Based on the following generation plan, generate the necessary code changes. Use files in your current context to provide the changes. 
            Both modification and adding new files requires absolute paths.
            
            Plan Steps:
            ${
            plan.steps.joinToString("\n") { step ->
                "- Action: ${step.action}\n  Input: ${step.input}\n  Expected Output: ${step.expectedOutput}"
            }
        }
            
            Current Code Context:
            $currentCode
            
            Expected Outcome:
            ${plan.expectedOutcome}
            
            Validation Criteria:
            ${plan.validationCriteria.joinToString("\n") { "- $it" }}
            
            Provide the code changes in the form of file operations:
            {
              "fileChanges": [
                {
                  "type": "CreateFile" or "ModifyFile",
                  "path": "file path",
                  "content": "file content for CreateFile",
                  "changes": [
                    {
                      "searchContent": "content to search for",
                      "replaceContent": "content to replace with",
                      "description": "description of the change"
                    }
                  ]
                }
              ],
              "explanation": "detailed explanation of the changes"
            }
        """.trimIndent()

        return model.prompt(
            input = prompt,
            action = emptyList(),
            temperature = 0.7,
            parameterizedTypeReference = object : ParameterizedTypeReference<GenerationResult>() {}
        )
    }
}

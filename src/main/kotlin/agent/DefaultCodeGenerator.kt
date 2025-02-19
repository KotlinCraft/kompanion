package agent

import agent.domain.*
import ai.Model
import org.springframework.core.ParameterizedTypeReference

class DefaultCodeGenerator(
    private val model: Model
) : CodeGenerator {

    override suspend fun generate(plan: GenerationPlan, currentCode: String): GenerationResult {
        val prompt = """
            Based on the following generation plan, generate the necessary code changes.
            
            Plan Steps:
            ${plan.steps.joinToString("\n") { step ->
                "- Action: ${step.action}\n  Input: ${step.input}\n  Expected Output: ${step.expectedOutput}"
            }}
            
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

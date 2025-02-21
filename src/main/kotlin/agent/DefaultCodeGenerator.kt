package agent

import agent.domain.GenerationPlan
import agent.domain.GenerationResult
import ai.Action
import ai.ActionMethod
import ai.LLMProvider
import org.springframework.core.ParameterizedTypeReference
import org.springframework.util.ReflectionUtils
import java.nio.file.Paths
import kotlin.io.path.exists

class DefaultCodeGenerator(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager
) : CodeGenerator {

    data class CodingResult(
        val editedFiles: List<String>,
        val createdFiles: List<String>
    )

    override suspend fun execute(
        plan: GenerationPlan,
        generationResult: GenerationResult,
    ): CodingResult {
        val prompt = """
            ${contextManager.currentContextPrompt()}
            
            You're an amazing developer, with many years of experience and a deep understanding of the clean code and architecture.

            Based on the following generation plan you have generated the necessary code changes. Use files in your current context to understand your changes. 
            
            Code changes to be applied:
            ${generationResult.formatted()}
            
            Expected Outcome:
            ${plan.expectedOutcome}
            
            Validation Criteria:
            ${plan.validationCriteria.joinToString("\n") { "- $it" }}
    
            Goal: 
            use your toolcalls to apply the changes to the codebase.
        """.trimIndent()

        return LLMProvider.prompt(
            input = prompt,
            actions = listOf(
                Action(
                    "modify_file",
                    """Modify a file, providing a regex search term and the replacement. The entire file, post edit, will be returned so you can verify the changes.""".trimMargin(),
                    ActionMethod(
                        ReflectionUtils.findMethod(this::class.java, "modifyFile", ModifyFileRequest::class.java),
                        this
                    )
                )
            ),
            temperature = 0.7,
            parameterizedTypeReference = object : ParameterizedTypeReference<CodingResult>() {}
        )
    }


    override suspend fun generate(plan: GenerationPlan, currentCode: String): GenerationResult {
        val prompt = """
            ${contextManager.currentContextPrompt()}
            
            You're an amazing developer, with many years of experience and a deep understanding of the clean code and architecture.

            Based on the following generation plan, generate the necessary code changes. Use files in your current context to provide the changes. 
            Both modification and adding new files requires absolute paths.
            
            Your toolcalls can be used to create and modify files. Modifying files will apply the replacement to the first occurrence of the search content.
            
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

        return LLMProvider.prompt(
            input = prompt,
            actions = emptyList(),
            temperature = 0.7,
            parameterizedTypeReference = object : ParameterizedTypeReference<GenerationResult>() {}
        )
    }

    data class ModifyFileResponse(
        val error: String?,
        val modifiedContent: String?
    )

    data class ModifyFileRequest(
        val absolutePath: String,
        val searchContent: String,
        val replaceContent: String
    )

    fun modifyFile(modifyFileRequest: ModifyFileRequest): ModifyFileResponse {
        val path = modifyFileRequest.absolutePath
        val fullPath = if (Paths.get(path).exists()) Paths.get(path) else Paths.get(
            contextManager.fetchWorkingDirectory(),
            path
        )

        if (!fullPath.exists()) {
            println("Error: File ${fullPath} does not exist")
            return ModifyFileResponse(error = "File does not exist", modifiedContent = "")
        }

        val file = fullPath.toFile()

        var content = file.readText()

        content = content.replaceFirst(modifyFileRequest.searchContent, modifyFileRequest.replaceContent)

        // Write the modified content back to the file
        file.writeText(content)
        return ModifyFileResponse(
            error = null,
            modifiedContent = content
        )
    }
}

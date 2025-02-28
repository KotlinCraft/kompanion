package agent.coding

import agent.CodeGenerator
import agent.ContextManager
import agent.coding.domain.*
import agent.domain.GenerationPlan
import agent.domain.context.ContextFile
import agent.reason.domain.RequestFileResponse
import ai.Action
import ai.ActionMethod
import ai.LLMProvider
import arrow.core.Either
import org.springframework.core.ParameterizedTypeReference
import org.springframework.util.ReflectionUtils
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class DefaultCodeGenerator(
    private val LLMProvider: LLMProvider, private val contextManager: ContextManager
) : CodeGenerator {


    val modifyFileAction = Action(
        "modify_file",
        """
            |Modify a file, providing an exact search search and the replacement. 
            |note: make the search content unique to avoid replacing unintended content.
            |The entire file, post edit, will be returned so you can verify the changes.""".trimMargin(),
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "modifyFile", ModifyFileRequest::class.java), this
        )
    )

    val createFileAction = Action(
        "create_file",
        """
            |Modify a file, providing a regex search term and the replacement. 
            |The entire file, post edit, will be returned so you can verify the changes.""".trimMargin(),
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "createFile", CreateFileRequest::class.java), this
        )
    )


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
            input = prompt, actions = listOf(
                modifyFileAction, createFileAction, readFileAction
            ), temperature = 0.5, parameterizedTypeReference = object : ParameterizedTypeReference<CodingResult>() {})
    }


    fun createFile(createFileRequest: CreateFileRequest): CreateFileResponse {
        return Either.catch {
            val fullPath = Paths.get(createFileRequest.absolutePath)

            // Create parent directories if they don't exist
            fullPath.parent?.toFile()?.mkdirs()

            // Create and write to the file
            fullPath.toFile().writeText(createFileRequest.content)
        }.fold(
            { CreateFileResponse(it.message) }, { CreateFileResponse(error = null) }
        )
    }

    fun modifyFile(modifyFileRequest: ModifyFileRequest): ModifyFileResponse {
        val path = modifyFileRequest.absolutePath
        val fullPath = if (Paths.get(path).exists()) Paths.get(path) else Paths.get(
            contextManager.fetchWorkingDirectory(), path
        )

        if (!fullPath.exists()) {
            println("Error: File ${fullPath} does not exist")
            return ModifyFileResponse(error = "File does not exist", modifiedContent = "")
        }

        val file = fullPath.toFile()

        val originalContent = file.readText()
        val newContent = originalContent.replaceFirst(modifyFileRequest.searchContent, modifyFileRequest.replaceContent)

        // Write the modified content back to the file

        file.writeText(newContent)
        return ModifyFileResponse(
            error = run {
                if (originalContent == newContent) {
                    "couldn't find search content in file"
                } else null
            }, modifiedContent = originalContent
        )
    }

    fun requestFileContext(file: String): RequestFileResponse {
        val filePath =
            Files.walk(Paths.get(contextManager.fetchWorkingDirectory())).filter { it.fileName.toString() == file }
                .findFirst()

        return if (filePath.isPresent) {
            val content = Files.readString(filePath.get())
            val path = filePath.get()

            // Add the file to context manager
            contextManager.updateFiles(
                listOf(
                    ContextFile(
                        name = path.absolutePathString(),
                        content = content
                    )
                )
            )

            RequestFileResponse(true, path.absolutePathString(), content)
        } else {
            RequestFileResponse(false, null, null)
        }
    }

    val readFileAction = Action(
        "request_file_context",
        """Provide a file in context for the request. 
                        |If the file does not exist yet, the response will contain an exists: false, else, the file will be provided.
                        |Only request an exact filename. Example: UserManager.kt, main.py or instruction.txt""".trimMargin(),
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "requestFileContext", String::class.java), this
        )
    )

}

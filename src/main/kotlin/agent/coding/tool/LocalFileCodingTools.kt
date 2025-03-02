package agent.coding.tool

import agent.ContextManager
import agent.coding.domain.CreateFileRequest
import agent.coding.domain.CreateFileResponse
import agent.coding.domain.ModifyFileRequest
import agent.coding.domain.ModifyFileResponse
import agent.tool.Tool
import agent.tool.ToolsProvider
import ai.Action
import ai.ActionMethod
import arrow.core.Either
import org.springframework.util.ReflectionUtils
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class LocalFileCodingTools(private val contextManager: ContextManager) : ToolsProvider {

    val modifyFileAction = Action(
        "modify_file_contents",
        """Modify a file, replacing its entire content with new content. 
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

    fun modifyFile(modifyFileRequest: ModifyFileRequest): ModifyFileResponse {
        val path = modifyFileRequest.absolutePath
        val fullPath = if (Paths.get(path).exists()) Paths.get(path) else Paths.get(
            contextManager.fetchWorkingDirectory(), path
        )

        if (!fullPath.exists()) {
            println("Error: File ${fullPath} does not exist")
            return ModifyFileResponse(
                path = fullPath.absolutePathString(),
                error = "File does not exist",
                newContent = null
            )
        }

        val file = fullPath.toFile()

        file.writeText(modifyFileRequest.newContent)
        return ModifyFileResponse(
            error = null,
            path = fullPath.absolutePathString(),
            newContent = modifyFileRequest.newContent
        )
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

    override fun getTools(): List<Tool> {
        return listOf(Tool(modifyFileAction), Tool(createFileAction))
    }
}
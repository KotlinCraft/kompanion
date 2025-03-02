package agent.coding.tool

import agent.ContextManager
import agent.coding.domain.CreateFileRequest
import agent.coding.domain.CreateFileResponse
import agent.coding.domain.ModifyFileRequest
import agent.coding.domain.ModifyFileResponse
import agent.interaction.InteractionHandler
import agent.modes.Interactor
import agent.tool.Tool
import agent.tool.ToolAllowedStatus
import agent.tool.ToolsProvider
import ai.Action
import ai.ActionMethod
import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.util.ReflectionUtils
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class LocalFileCodingTools(
    private val interactionHandler: InteractionHandler,
    private val contextManager: ContextManager
) : ToolsProvider, Interactor {

    val modifyFileAction = Tool(
        Action(
            "modify_file_contents",
            """Modify a file, replacing its entire content with new content. 
            |Example request: {"path": "/absolute/path/to/file", "content": "new content"}
            |The entire file, post edit, will be returned so you can verify the changes.""".trimMargin(),
            ActionMethod(
                ReflectionUtils.findMethod(this::class.java, "modifyFile", ModifyFileRequest::class.java), this
            )
        )
    )

    val createFileAction = Tool(
        Action(
            "create_file",
            """
            |Modify a file, providing a regex search term and the replacement. 
            |The entire file, post edit, will be returned so you can verify the changes.""".trimMargin(),
            ActionMethod(
                ReflectionUtils.findMethod(this::class.java, "createFile", CreateFileRequest::class.java), this
            )
        )
    )

    fun modifyFile(modifyFileRequest: ModifyFileRequest): ModifyFileResponse {
        if (createFileAction.allowedStatus == null) {
            val response = runBlocking(Dispatchers.IO) { confirmWithUser("Am I allowed to modify  files?") }
            if (response) {
                createFileAction.allowedStatus = ToolAllowedStatus.ALLOWED
            } else {
                createFileAction.allowedStatus = ToolAllowedStatus.NOT_ALLOWED
                return ModifyFileResponse(
                    path = modifyFileRequest.path,
                    error = "User denied permission to create files",
                    newContent = null
                )
            }
        }

        val path = modifyFileRequest.path
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

        file.writeText(modifyFileRequest.content)
        return ModifyFileResponse(
            error = null,
            path = fullPath.absolutePathString(),
            newContent = modifyFileRequest.content
        )
    }


    fun createFile(createFileRequest: CreateFileRequest): CreateFileResponse {
        if (createFileAction.allowedStatus == null) {
            val response = runBlocking(Dispatchers.IO) { confirmWithUser("Am I allowed to create files?") }
            if (response) {
                createFileAction.allowedStatus = ToolAllowedStatus.ALLOWED
            } else {
                createFileAction.allowedStatus = ToolAllowedStatus.NOT_ALLOWED
                return CreateFileResponse("User denied permission to create files")
            }
        }

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
        return listOf(modifyFileAction, createFileAction)
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
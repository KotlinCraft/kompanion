package agent.coding.tool

import agent.ContextManager
import agent.coding.domain.CreateFileRequest
import agent.coding.domain.CreateFileResponse
import agent.coding.domain.ModifyFileRequest
import agent.coding.domain.ModifyFileResponse
import agent.interaction.InteractionHandler
import agent.modes.Interactor
import agent.tool.ToolsProvider
import arrow.core.Either
import org.springframework.ai.tool.annotation.Tool
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class LocalFileCodingTools(
    private val interactionHandler: InteractionHandler,
    private val contextManager: ContextManager
) : ToolsProvider, Interactor {

    @Tool(
        name = "modify_file_contents",
        description =
            """Modify a file, replacing its entire content with new content. 
            |Example request: {"path": "/absolute/path/to/file", "content": "new content"}
            |The entire file, post edit, will be returned so you can verify the changes."""
    )
    fun modifyFile(modifyFileRequest: ModifyFileRequest): ModifyFileResponse {
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


    @Tool(
        name = "create_file",
        description = """
            |Modify a file, providing a regex search term and the replacement. 
            |The entire file, post edit, will be returned so you can verify the changes."""
    )
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

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
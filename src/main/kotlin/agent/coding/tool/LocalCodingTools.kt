package agent.coding.tool

import agent.ContextManager
import agent.coding.domain.CreateFileResponse
import agent.coding.domain.ModifyFileResponse
import agent.interaction.InteractionHandler
import agent.interaction.ToolStatus
import agent.modes.Interactor
import agent.tool.ToolsProvider
import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import ui.chat.CreateFileIndicator
import ui.chat.ModifyFileIndicator
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class LocalCodingTools(
    private val interactionHandler: InteractionHandler, private val contextManager: ContextManager
) : ToolsProvider, Interactor {

    @Tool(
        name = "modify_file_contents", description = """Modify a file, replacing its entire content with new content. 
            |The entire file, post edit, will be returned so you can verify the changes."""
    )
    fun modifyFile(
        @ToolParam(
            required = true, description = "Path to the file to modify"
        ) path: String, @ToolParam(
            required = true, description = "New content for the file"
        ) content: String, @ToolParam(
            required = true, description = "Explanation for the modification"
        ) explanation: String
    ): ModifyFileResponse {
        val fullPath = if (Paths.get(path).exists()) Paths.get(path) else Paths.get(
            contextManager.fetchWorkingDirectory(), path
        )

        // Show RUNNING indicator
        val toolId = runBlocking(Dispatchers.IO) {
            customToolUsage {
                ModifyFileIndicator(
                    fullPath.absolutePathString(), ToolStatus.RUNNING
                )
            }
        }

        if (!fullPath.exists()) {
            println("Error: File ${fullPath} does not exist")

            // Show FAILED indicator
            runBlocking(Dispatchers.IO) {
                customToolUsage(
                    id = toolId, toolIndicator = {
                        ModifyFileIndicator(
                            fullPath.absolutePathString(), ToolStatus.FAILED, null, "File does not exist"
                        )
                    })
            }

            return ModifyFileResponse(
                path = fullPath.absolutePathString(), error = "File does not exist", newContent = null
            )
        }

        val file = fullPath.toFile()

        try {
            file.writeText(content)

            // Show COMPLETED indicator
            runBlocking(Dispatchers.IO) {
                customToolUsage(
                    id = toolId, toolIndicator = {
                        ModifyFileIndicator(
                            fullPath.absolutePathString(), ToolStatus.COMPLETED, explanation
                        )
                    })
            }

            return ModifyFileResponse(
                error = null, path = fullPath.absolutePathString(), newContent = content
            )
        } catch (e: Exception) {
            // Show FAILED indicator
            runBlocking(Dispatchers.IO) {
                customToolUsage(
                    id = toolId, toolIndicator = {
                        ModifyFileIndicator(
                            fullPath.absolutePathString(), ToolStatus.FAILED, null, "Error modifying file: ${e.message}"
                        )
                    })
            }

            return ModifyFileResponse(
                path = fullPath.absolutePathString(), error = "Error modifying file: ${e.message}", newContent = null
            )
        }
    }


    @Tool(
        name = "create_file", description = """Create a new file with the specified content at the given absolute path.
            |Parent directories will be created if they don't exist."""
    )
    fun createFile(
        @ToolParam(
            required = true, description = "Absolute path to the file to create"
        ) absoluteFilePath: String, @ToolParam(
            required = true, description = "Content to write to the file"
        ) content: String, @ToolParam(
            required = true, description = "Explanation for creating the file"
        ) explanation: String
    ): CreateFileResponse {
        val fullPath = Paths.get(absoluteFilePath)

        // Show RUNNING indicator
        val toolId = runBlocking(Dispatchers.IO) {
            customToolUsage(
                toolIndicator = {
                    CreateFileIndicator(
                        fullPath.absolutePathString(), ToolStatus.RUNNING
                    )
                })
        }

        return Either.catch {
            // Create parent directories if they don't exist
            fullPath.parent?.toFile()?.mkdirs()

            // Create and write to the file
            fullPath.toFile().writeText(content)

            // Show COMPLETED indicator
            runBlocking(Dispatchers.IO) {
                customToolUsage(toolId) {
                    CreateFileIndicator(
                        fullPath.absolutePathString(), ToolStatus.COMPLETED
                    )
                }
            }
        }.fold({
            // Show FAILED indicator
            runBlocking(Dispatchers.IO) {
                customToolUsage(
                    id = toolId, toolIndicator = {
                        CreateFileIndicator(
                            fullPath.absolutePathString(), ToolStatus.FAILED, it.message
                        )
                    })
            }

            CreateFileResponse(it.message)
        }, { CreateFileResponse(error = null) })
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
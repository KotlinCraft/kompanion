package agent.tool

import agent.ContextManager
import agent.domain.context.ContextFile
import agent.interaction.InteractionHandler
import agent.modes.Interactor
import agent.reason.domain.RequestFileResponse
import ai.Action
import ai.ActionMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.util.ReflectionUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class FileTools(
    private val contextManager: ContextManager
) : ToolsProvider {

    val logger = LoggerFactory.getLogger(this::class.java)

    fun requestFileContext(file: String): RequestFileResponse {

        val filePath = Optional.of(Path.of(file)).filter(Path::exists).or {
            Optional.of(Path.of(contextManager.fetchWorkingDirectory() + "/" + file)).filter(Path::exists)
        }.or {
            Files.walk(Paths.get(contextManager.fetchWorkingDirectory())).filter { it.fileName.toString() == file }
                .findFirst()
        }

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

            RequestFileResponse(true, path.absolutePathString(), content).also {
                logger.info("File $file found and added to context")
            }
        } else {
            RequestFileResponse(false, null, null)
        }
    }

    val readFileAction = Action(
        "request_file_context",
        """Provide a file in context for the request. 
            | Always use the full path (absolute) file.
                        |If the file does not exist yet, the response will contain an exists: false, else, the file will be provided.
                        |Only request an exact filename. Example: UserManager.kt, main.py or instruction.txt""".trimMargin(),
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "requestFileContext", String::class.java), this
        )
    )

    override fun getTools(): List<Tool> {
        return listOf(
            Tool(readFileAction)
        )
    }
}
package agent

import agent.domain.context.ContextFile
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

interface ContextManager {
    /**
     * Retrieves the current context as a list of CodeFiles.
     * @return A list of CodeFiles representing the current context.
     */
    fun getContext(): StateFlow<Set<ContextFile>>

    /**
     * Updates the context with a list of CodeFiles.
     * @param files A list of CodeFiles to be updated in the context.
     */
    fun updateFiles(files: List<ContextFile>)

    fun removeFile(id: UUID)

    /**
     * Clears all CodeFiles from the current context.
     */
    fun clearContext()


    /**
     * Fetches the current working directory path as a string.
     * @return The path of the current working directory.
     */
    fun fetchWorkingDirectory(): String

    /**
     * Provides a detailed outline of the full file list in the codebase.
     * @return A string representation of the detailed file outline.
     */
    fun getFullFileList(): String

    /**
     * Generates a string prompt that describes the current context, including working directory and files.
     * @return A formatted string detailing the current context.
     */
    fun currentContextPrompt(includeFolderOutline: Boolean): String {
        val files = getContext().value

        val fullFileList = run {
            if (includeFolderOutline) {
                """
The full codebase outline looks like this:
${getFullFileList()}
""".trimIndent()
            } else {
                ""
            }
        }

        return """     
            Current working directory is: ${fetchWorkingDirectory()}
            
            ${fullFileList}
            
            Files in your current context: 
            ${
            if (files.isEmpty()) "no files in context yet" else
                files.joinToString("\n") {
                    """File: ${it.name}
                            |Content: ${it.content}
                        """.trimMargin()
                }
        }""".trimIndent()
    }


    fun findRelatedFiles(relatedFiles: String): List<File>
}
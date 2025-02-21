package agent

import agent.domain.CodeFile
import kotlinx.coroutines.flow.StateFlow

interface ContextManager {
    /**
     * Retrieves the current context as a list of CodeFiles.
     * @return A list of CodeFiles representing the current context.
     */
    fun getContext(): StateFlow<Map<String, CodeFile>>

    /**
     * Updates the context with a list of CodeFiles.
     * @param files A list of CodeFiles to be updated in the context.
     */
    fun updateFiles(files: List<CodeFile>)

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
    fun currentContextPrompt(): String {
        val codeFiles = getContext().value.values

        return """     
            Current working directory is: ${fetchWorkingDirectory()}
            The full codebase outline looks like this:
            ${getFullFileList()}
            
            Files in your current context: 
            ${
            if (codeFiles.isEmpty()) "no files in context yet" else
                codeFiles.joinToString("\n") {
                    """File: ${it.path} (language: ${it.language})
                            |Content: ${it.content}
                        """.trimMargin()
                }
        }""".trimIndent()
    }

}
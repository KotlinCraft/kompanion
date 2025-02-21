package agent

import agent.domain.CodeFile
import agent.domain.UserRequest

interface ContextManager {
    fun getContext(): List<CodeFile>
    fun updateFiles(files: List<CodeFile>)
    fun clearContext()
    fun fetchWorkingDirectory(): String

    fun getFullFileList(): String


    fun currentContextPrompt(): String {
        val context = getContext()

        return """     
            Current working directory is: ${fetchWorkingDirectory()}
            The full codebase outline looks like this:
            ${getFullFileList()}
            
            Files in your current context: 
            ${
            if (context.isEmpty()) "no files in context yet" else
                context.joinToString("\n") {
                    """File: ${it.path} (language: ${it.language})
                            |Content: ${it.content}
                        """.trimMargin()
                }
        }""".trimIndent()
    }

}
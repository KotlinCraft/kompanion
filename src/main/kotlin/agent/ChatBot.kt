package agent

import agent.domain.CodeFile
import agent.domain.UserRequest

class ChatBot(private val agent: CodeAgent) {
    suspend fun handleMessage(message: String, attachedFiles: List<CodeFile> = emptyList()): String {
        // Update context with any attached files
        if (attachedFiles.isNotEmpty()) {
            agent.updateContext(attachedFiles)
        }
        
        // Process the request
        val response = agent.process(
            UserRequest(
            instruction = message,
            codeContext = attachedFiles
        )
        )
        
        // Format response for chat
        return """
            Generated Code:
            ```
            ${response.generatedCode}
            ```
            
            Explanation:
            ${response.explanation}
            
            Next Steps:
            ${response.nextSteps.joinToString("\n")}
            
            Confidence: ${response.confidence * 100}%
        """.trimIndent()
    }
}
package agent.domain.action

/**
 * Parser for extracting actions and parameters from LLM responses
 */
class ActionParser {
    
    /**
     * Parse an LLM response into action type and parameters
     */
    fun parseResponse(response: String): Pair<String, Map<String, String>> {
        val lines = response.split("\n")
        
        // Extract action type
        val actionLine = lines.firstOrNull { it.startsWith("ACTION:") }?.substringAfter("ACTION:") ?: ""
        val actionType = actionLine.trim().uppercase()
        
        // Extract known parameters based on action type
        val parameters = mutableMapOf<String, String>()
        
        // Common parameters across different actions
        parameters["FILE_PATH"] = lines.firstOrNull { it.startsWith("FILE_PATH:") }
            ?.substringAfter("FILE_PATH:")?.trim() ?: ""
            
        parameters["EXPLANATION"] = lines.firstOrNull { it.startsWith("EXPLANATION:") }
            ?.substringAfter("EXPLANATION:")?.trim() ?: ""
            
        parameters["SUMMARY"] = lines.firstOrNull { it.startsWith("SUMMARY:") }
            ?.substringAfter("SUMMARY:")?.trim() ?: ""
        
        // Extract content between CONTENT: and the next ``` or end of string
        val contentStart = response.indexOf("CONTENT:")
        val contentEnd = response.lastIndexOf("```")
        val content = if (contentStart != -1 && contentEnd != -1 && contentStart < contentEnd) {
            response.substring(contentStart + "CONTENT:".length, contentEnd).trim()
        } else if (contentStart != -1) {
            response.substring(contentStart + "CONTENT:".length).trim()
        } else {
            ""
        }
        parameters["CONTENT"] = content
        
        return Pair(actionType, parameters)
    }
    
    /**
     * Count the number of action directives in the response
     */
    fun countActionsInResponse(response: String): Int {
        val lines = response.split("\n")
        // Look for ACTION: lines in the response
        return lines.count { it.trim().startsWith("ACTION:") }
    }
}
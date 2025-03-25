package agent.domain.action

/**
 * Base interface for all LLM actions
 */
interface LLMAction {
    /**
     * The action identifier used in LLM responses (e.g., "EDIT_FILE", "CREATE_FILE")
     */
    val actionType: String
    
    /**
     * Returns a summary of the action execution
     */
    fun summary(): String
    
    /**
     * Process the action and return success status 
     */
    suspend fun process(): Boolean
}
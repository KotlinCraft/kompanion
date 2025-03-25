package agent.config

import agent.ContextManager
import agent.domain.action.ActionHandler
import agent.domain.action.LLMAction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for registering LLM actions
 */
@Configuration
class ActionConfiguration {
    
    @Bean
    fun actionHandler(contextManager: ContextManager): ActionHandler {
        val handler = ActionHandler(contextManager)
        
        // The default actions (EDIT_FILE, CREATE_FILE, COMPLETE) are registered in the ActionHandler constructor
        
        // Example of how to register a custom action:
        // handler.registerAction("CUSTOM_ACTION") { params, rawResponse ->
        //     CustomAction(params["PARAM1"] ?: "", params["PARAM2"] ?: "")
        // }
        
        return handler
    }
}

/**
 * Example of a custom action (not used but shows how to extend)
 */
class CustomAction(
    val param1: String,
    val param2: String
) : LLMAction {
    override val actionType: String = "CUSTOM_ACTION"
    
    override fun summary(): String = "Custom action executed with $param1 and $param2"
    
    override suspend fun process(): Boolean {
        // Implement the custom action logic here
        return true
    }
}
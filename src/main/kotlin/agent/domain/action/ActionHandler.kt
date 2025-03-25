package agent.domain.action

import agent.ContextManager
import agent.domain.context.ContextFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * Registry and processor for LLM actions
 */
class ActionHandler(private val contextManager: ContextManager) {
    
    // Registry of action implementations by action type
    private val actionRegistry = mutableMapOf<String, ActionFactory>()
    
    // Store action format descriptions
    private val actionFormats = mutableMapOf<String, String>()
    
    /**
     * Interface for action factory functions
     */
    fun interface ActionFactory {
        fun create(parameters: Map<String, String>, rawResponse: String): LLMAction
    }
    
    /**
     * Register a new action type with its factory
     */
    fun registerAction(actionType: String, factory: ActionFactory, formatDescription: String) {
        actionRegistry[actionType] = factory
        actionFormats[actionType] = formatDescription
    }
    
    /**
     * Create an action from a parsed LLM response
     */
    fun createAction(actionType: String, parameters: Map<String, String>, rawResponse: String): LLMAction {
        return actionRegistry[actionType]?.create(parameters, rawResponse)
            ?: throw IllegalArgumentException("Unsupported action type: $actionType")
    }
    
    /**
     * Get all registered action types
     */
    fun getRegisteredActionTypes(): List<String> {
        return actionRegistry.keys.toList()
    }
    
    /**
     * Get format description for a specific action type
     */
    fun getActionFormat(actionType: String): String? {
        return actionFormats[actionType]
    }
    
    /**
     * Get all action formats
     */
    fun getAllActionFormats(): Map<String, String> {
        return actionFormats.toMap()
    }
    
    /**
     * Initialize with default actions
     */
    init {
        // Register built-in actions with format descriptions
        registerAction(
            "EDIT_FILE",
            { params, rawResponse ->
                val filePath = params["FILE_PATH"] ?: ""
                val content = params["CONTENT"] ?: ""
                val explanation = params["EXPLANATION"] ?: ""
                
                EditFileAction(filePath, content, explanation, contextManager)
            },
            """To edit a file:
            ```
            ACTION: EDIT_FILE
            FILE_PATH: /absolute/path/to/file
            EXPLANATION: Brief explanation of changes
            CONTENT:
            // Complete new content of the file
            ```"""
        )
        
        registerAction(
            "CREATE_FILE",
            { params, rawResponse ->
                val filePath = params["FILE_PATH"] ?: ""
                val content = params["CONTENT"] ?: ""
                val explanation = params["EXPLANATION"] ?: ""
                
                CreateFileAction(filePath, content, explanation, contextManager)
            },
            """To create a file:
            ```
            ACTION: CREATE_FILE
            FILE_PATH: /absolute/path/to/file
            EXPLANATION: Brief explanation of the file purpose
            CONTENT:
            // Complete content of the new file
            ```"""
        )
        
        registerAction(
            "COMPLETE",
            { params, rawResponse ->
                val summary = params["SUMMARY"] ?: ""
                
                CompleteAction(summary)
            },
            """When complete:
            ```
            ACTION: COMPLETE
            SUMMARY: Detailed explanation of all changes made
            ```"""
        )
    }
}

/**
 * Action to edit an existing file
 */
class EditFileAction(
    val filePath: String,
    val content: String,
    val explanation: String,
    private val contextManager: ContextManager
) : LLMAction {
    override val actionType: String = "EDIT_FILE"
    
    override fun summary(): String = explanation
    
    override suspend fun process(): Boolean {
        try {
            // Update the file
            val file = Path.of(filePath).toFile()
            Files.writeString(file.toPath(), content)

            // Update the context
            contextManager.updateFiles(
                listOf(
                    ContextFile(
                        id = UUID.randomUUID(),
                        name = file.absolutePath,
                        content = content,
                        displayName = file.name
                    )
                )
            )
            return true
        } catch (e: Exception) {
            return false
        }
    }
}

/**
 * Action to create a new file
 */
class CreateFileAction(
    val filePath: String,
    val content: String,
    val explanation: String,
    private val contextManager: ContextManager
) : LLMAction {
    override val actionType: String = "CREATE_FILE"
    
    override fun summary(): String = explanation
    
    override suspend fun process(): Boolean {
        try {
            // Create the file
            val file = Path.of(filePath).toFile()
            file.parentFile?.mkdirs() // Create parent directories if they don't exist
            Files.writeString(file.toPath(), content)

            // Update the context
            contextManager.updateFiles(
                listOf(
                    ContextFile(
                        id = UUID.randomUUID(),
                        name = file.absolutePath,
                        content = content,
                        displayName = file.name
                    )
                )
            )
            return true
        } catch (e: Exception) {
            return false
        }
    }
}

/**
 * Action to complete the flow
 */
class CompleteAction(val explanation: String) : LLMAction {
    override val actionType: String = "COMPLETE"
    
    override fun summary(): String = explanation
    
    override suspend fun process(): Boolean = true
}
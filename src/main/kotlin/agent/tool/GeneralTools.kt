package agent.tool

import agent.interaction.InteractionHandler
import agent.modes.Interactor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.ai.tool.ToolCallbacks
import org.springframework.ai.tool.annotation.ToolParam

class GeneralTools(private val interactionHandler: InteractionHandler) : ToolsProvider, Interactor {

    @org.springframework.ai.tool.annotation.Tool(
        name = "ask_question",
        description = "Ask the user a question to gather additional information needed to complete the task. This tool should be used when you encounter ambiguities, need clarification, or require more details to proceed effectively. It allows for interactive problem-solving by enabling direct communication with the user. Use this tool judiciously to maintain a balance between gathering necessary information and avoiding excessive back-and-forth."
    )
    fun askQuestion(@ToolParam(required = true, description = "question to ask the user") question: String): String {
        return runBlocking(Dispatchers.IO) {
            askUser(question)
        }
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }

    override fun getTools(): List<Tool> {
        return ToolCallbacks.from(this)
            .map { Tool.from(it, allowedStatus = ToolAllowedStatus.ALLOWED, showUpInTools = false) }
    }
}
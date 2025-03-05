package agent.tool

import agent.interaction.InteractionHandler
import agent.modes.Interactor
import ai.Action
import ai.ActionMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.ai.tool.ToolCallbacks
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.util.ReflectionUtils

class GeneralTools(private val interactionHandler: InteractionHandler) : ToolsProvider, Interactor {

    @org.springframework.ai.tool.annotation.Tool(
        name = "ask_question",
        description = "Ask the user a question, in order to clarify certain things."
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
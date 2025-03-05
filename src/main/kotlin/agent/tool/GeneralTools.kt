package agent.tool

import agent.interaction.InteractionHandler
import agent.modes.Interactor
import ai.Action
import ai.ActionMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.util.ReflectionUtils

class GeneralTools(private val interactionHandler: InteractionHandler) : ToolsProvider, Interactor {

    val ask_question = Action(
        "ask_question",
        "Ask the user a question, in order to clarify certain things.",
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "askQuestion", String::class.java),
            this
        ),
        showUpInTools = false
    )

    fun askQuestion(question: String): String {
        return runBlocking(Dispatchers.IO) {
            askUser(question)
        }
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }

    override fun getTools(): List<Tool> {
        return listOf(
            Tool.from(ask_question, ToolAllowedStatus.ALLOWED)
        )
    }
}
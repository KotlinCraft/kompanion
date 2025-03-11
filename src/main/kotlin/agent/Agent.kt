package agent

import agent.interaction.InteractionHandler
import agent.modes.Interactor
import agent.modes.Mode

class Agent internal constructor(
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
    val mode: Mode
) : Interactor {

    fun fetchContextManager(): ContextManager {
        return contextManager
    }

    suspend fun perform(request: String): String {
        return mode.perform(request)
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}

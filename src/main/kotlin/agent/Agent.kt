package agent

import agent.interaction.InteractionHandler
import agent.modes.Interactor
import agent.modes.Mode
import org.springframework.ai.chat.memory.ChatMemory

class Agent internal constructor(
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
    private val memory: ChatMemory,
    val mode: Mode
) : Interactor {

    fun purgeMemory() {
        memory.clear("default")
    }

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

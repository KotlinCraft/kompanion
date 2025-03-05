package agent.interaction

import java.util.*

interface InteractionHandler {
    suspend fun interact(agentMessage: AgentMessage): String
    fun removeChat(id: UUID)
}
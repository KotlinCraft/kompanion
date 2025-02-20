package agent.interaction

interface InteractionHandler {
    suspend fun interact(agentMessage: AgentMessage): String
}
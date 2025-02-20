package agent

import agent.interaction.AgentMessage

interface AgentMessageCallback {
    suspend fun onAgentInteraction(message: AgentMessage): String
}

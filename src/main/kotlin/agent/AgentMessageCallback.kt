package agent

import agent.interaction.AgentMessage

interface AgentMessageCallback {
    fun onAgentInteraction(message: AgentMessage)
}

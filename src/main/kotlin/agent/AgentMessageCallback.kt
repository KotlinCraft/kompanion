package agent

import agent.interaction.AgentMessage

interface AgentMessageCallback {
    fun onMessage(message: AgentMessage)
}

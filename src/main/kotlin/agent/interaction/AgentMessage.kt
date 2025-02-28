package agent.interaction

sealed class AgentMessage(val message: String, val important: Boolean)

//message is either a question or a response
class AgentQuestion(message: String, important: Boolean = false) : AgentMessage(message, important)
class AgentResponse(message: String, important: Boolean = false) : AgentMessage(message, important)
class AgentAskConfirmation(message: String, important: Boolean = false) : AgentMessage(message, important)
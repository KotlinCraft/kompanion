package agent.interaction

sealed class AgentMessage {

}
//message is either a question or a response
data class AgentQuestion(val message: String) : AgentMessage()
data class AgentResponse(val message: String) : AgentMessage()
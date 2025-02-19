package agent

import agent.domain.AgentResponse
import agent.domain.CodeFile
import agent.domain.UserFeedback
import agent.domain.UserRequest

interface CodeAgent {
    suspend fun process(request: UserRequest): AgentResponse
    fun updateContext(codeFiles: List<CodeFile>)
    fun addFeedback(feedback: UserFeedback)
}
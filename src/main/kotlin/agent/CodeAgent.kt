package agent

import agent.domain.CodingAgentResponse
import agent.domain.UserFeedback
import agent.domain.UserRequest

interface CodeAgent {
    suspend fun process(request: UserRequest): CodingAgentResponse
    suspend fun addFeedback(feedback: UserFeedback)
}
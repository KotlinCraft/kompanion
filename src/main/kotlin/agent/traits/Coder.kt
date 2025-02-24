package agent.traits

import agent.domain.CodingAgentResponse
import agent.domain.UserRequest

interface Coder: Interactor {
    suspend fun processCodingRequest(request: UserRequest): CodingAgentResponse
}
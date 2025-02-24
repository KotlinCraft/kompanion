package agent.traits

import agent.domain.CodingAgentResponse

interface CodingMode : Interactor {
    suspend fun processCodingRequest(request: String): CodingAgentResponse
}
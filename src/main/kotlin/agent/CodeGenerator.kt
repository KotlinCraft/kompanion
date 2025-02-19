package agent

import agent.domain.GenerationPlan
import agent.domain.GenerationResult

interface CodeGenerator {
    suspend fun generate(plan: GenerationPlan, request: UserRequest): GenerationResult
}

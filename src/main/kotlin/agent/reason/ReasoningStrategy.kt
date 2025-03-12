package agent.reason

import agent.domain.GenerationPlan

interface ReasoningStrategy {
    suspend fun reason(request: String, plan: GenerationPlan): GenerationPlan
}

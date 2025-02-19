package agent

import agent.domain.*

// Reasoning component for understanding and planning
interface Reasoner {
    suspend fun analyzeRequest(request: UserRequest): Understanding
    suspend fun createPlan(understanding: Understanding): GenerationPlan
    suspend fun evaluateCode(result: GenerationResult, understanding: Understanding): CodeEvaluation
    suspend fun learn(feedback: UserFeedback)
}

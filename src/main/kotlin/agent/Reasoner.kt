package agent

import agent.domain.*

// Reasoning component for understanding and planning
interface Reasoner {
    fun analyzeRequest(request: UserRequest): Understanding
    fun createPlan(understanding: Understanding): GenerationPlan
    fun evaluateCode(result: GenerationResult, understanding: Understanding): CodeEvaluation
    fun learn(feedback: UserFeedback)
}
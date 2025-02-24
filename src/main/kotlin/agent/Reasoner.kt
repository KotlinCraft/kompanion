package agent

import agent.domain.*

interface Reasoner {
    suspend fun analyzeRequest(request: UserRequest): Understanding
    suspend fun createPlan(understanding: Understanding): GenerationPlan
    suspend fun evaluateCode(result: GenerationResult, understanding: Understanding): CodeEvaluation
    suspend fun learn(feedback: UserFeedback)
    suspend fun askQuestion(question: String, understanding: Understanding): CodebaseQuestionResponse
}

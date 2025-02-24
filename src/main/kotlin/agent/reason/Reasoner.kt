package agent.reason

import agent.domain.*

interface Reasoner {
    suspend fun analyzeRequest(request: String): Understanding
    suspend fun createPlan(understanding: Understanding): GenerationPlan
    suspend fun evaluateCode(result: GenerationResult, understanding: Understanding): CodeEvaluation
    suspend fun askQuestion(question: String, understanding: Understanding): CodebaseQuestionResponse
}

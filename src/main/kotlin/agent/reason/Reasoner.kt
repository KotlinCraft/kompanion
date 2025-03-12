package agent.reason

import agent.domain.*

interface Reasoner {
    suspend fun analyzeRequest(request: String): Understanding
    suspend fun createPlan(request: String, understanding: Understanding): GenerationPlan
    suspend fun askQuestion(question: String, understanding: Understanding): CodebaseQuestionResponse
}

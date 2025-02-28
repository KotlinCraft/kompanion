package agent.reason

import agent.domain.*
import ai.Action

interface Reasoner {
    suspend fun analyzeRequest(request: String): Understanding
    suspend fun createPlan(understanding: Understanding): GenerationPlan
    suspend fun askQuestion(question: String, understanding: Understanding, actions: List<Action>): CodebaseQuestionResponse
}

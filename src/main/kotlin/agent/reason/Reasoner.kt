package agent.reason

import agent.domain.*
import ai.Action

interface Reasoner {
    suspend fun askQuestion(question: String, understanding: Understanding, actions: List<Action>): CodebaseQuestionResponse
}

package agent.traits

import agent.domain.CodebaseQuestionResponse

interface Analyst {
    suspend fun askQuestion(question: String): CodebaseQuestionResponse
}
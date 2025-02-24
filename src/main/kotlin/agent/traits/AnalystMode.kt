package agent.traits

import agent.domain.CodebaseQuestionResponse

interface AnalystMode {
    suspend fun askQuestion(question: String): CodebaseQuestionResponse
}
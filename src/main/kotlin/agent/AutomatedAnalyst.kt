package agent

import agent.domain.CodebaseQuestionResponse
import agent.domain.UserRequest
import agent.traits.Analyst
import agent.traits.Interactor
import org.slf4j.LoggerFactory

class AutomatedAnalyst(
    private val reasoner: Reasoner,
) : Analyst, Interactor {

    override suspend fun askQuestion(question: String): CodebaseQuestionResponse {
        val request = UserRequest(question)
        val understanding = reasoner.analyzeRequest(request)
        return reasoner.askQuestion(question, understanding)
    }

    override suspend fun sendMessage(message: String) {
        TODO("Not yet implemented")
    }

    override suspend fun askUser(question: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun confirmWithUser(message: String): Boolean {
        TODO("Not yet implemented")
    }
}

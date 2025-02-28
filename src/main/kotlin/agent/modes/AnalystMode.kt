package agent.modes

import agent.reason.Reasoner

class AnalystMode(private val reasoner: Reasoner) : Mode {
    override suspend fun perform(request: String): String {
        val understanding = reasoner.analyzeRequest(request)
        return reasoner.askQuestion(request, understanding, emptyList()).reply
    }

    override suspend fun getLoadedActionNames(): List<String> {
        return emptyList()
    }
}
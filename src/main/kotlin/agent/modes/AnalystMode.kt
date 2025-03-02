package agent.modes

import agent.ContextManager
import agent.InMemoryContextManager
import agent.ToolManager
import agent.interaction.InteractionHandler
import agent.reason.Reasoner
import agent.tool.FileTools

class AnalystMode(
    private val reasoner: Reasoner,
    private val interactionHandler: InteractionHandler,
    private val contextManager: ContextManager,
    private val toolManager: ToolManager
) : Mode {

    init {
        FileTools(contextManager).register(toolManager)
    }

    override suspend fun perform(request: String): String {
        val understanding = reasoner.analyzeRequest(request)
        return reasoner.askQuestion(request, understanding).reply
    }

    override suspend fun getLoadedActionNames(): List<String> {
        return emptyList()
    }
}
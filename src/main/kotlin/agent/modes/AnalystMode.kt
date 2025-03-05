package agent.modes

import agent.ContextManager
import agent.ToolManager
import agent.interaction.InteractionHandler
import agent.reason.Reasoner
import agent.tool.FileTools
import agent.tool.LoadedTool

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

    override suspend fun getLoadedTools(): List<LoadedTool> {
        return emptyList()
    }

    override suspend fun disableAction(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun enableAction(id: String) {
        TODO("Not yet implemented")
    }
}
package agent.modes

import agent.ContextManager
import agent.ToolManager
import agent.interaction.InteractionHandler
import agent.reason.AutoModeReasoner
import agent.tool.GeneralTools
import agent.tool.LoadedTool
import config.AppConfig
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger(AutoMode::class.java)

/**
 * Mode for blockchain-related operations.
 * Allows querying contract source code, ABIs, and other blockchain data.
 */
class AutoMode(
    private val reasoner: AutoModeReasoner,
    private val toolManager: ToolManager,
    private val interactionHandler: InteractionHandler,
) : Mode, Interactor {


    init {
        GeneralTools(interactionHandler).register(toolManager)
    }


    override suspend fun perform(request: String): String {
        return reasoner.askQuestion(request).reply
    }

    override suspend fun getLoadedTools(): List<LoadedTool> {
        return toolManager.tools.filter {
            it.showUpInTools
        }.map {
            LoadedTool(
                id = it.id,
                name = it.toolCallback.toolDefinition.name(),
                tool = it
            )
        }
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
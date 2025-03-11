package agent.modes

import agent.ContextManager
import agent.ToolManager
import agent.blockchain.tool.BanklessTools
import agent.interaction.InteractionHandler
import agent.reason.BlockchainReasoner
import agent.tool.GeneralTools
import agent.tool.LoadedTool
import agent.tool.Tool
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.nel
import config.AppConfig
import mcp.McpManager
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import java.util.*

val logger = LoggerFactory.getLogger(BlockchainMode::class.java)

/**
 * Mode for blockchain-related operations.
 * Allows querying contract source code, ABIs, and other blockchain data.
 */
class BlockchainMode(
    private val reasoner: BlockchainReasoner,
    private val toolManager: ToolManager,
    private val mcpManager: McpManager,
    contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
) : Mode, Interactor {


    init {
        GeneralTools(interactionHandler).register(toolManager)

        if (isBanklessSupported()) {
            BanklessTools(interactionHandler, contextManager).register(toolManager)
        }

        val toolbacks = mcpManager.getMcpServers().flatMap {
            Either.catch {
                SyncMcpToolCallbackProvider.syncToolCallbacks(it.nel())
            }.mapLeft {
                logger.error("unable to initialize mcp server: {}", it.message)
            }.getOrElse { emptyList() }
        }.distinctBy { it.toolDefinition.name() }

        toolbacks.forEach {
            toolManager.registerTool(
                Tool(
                    id = UUID.randomUUID().toString(),
                    toolCallback = it,
                    showUpInTools = true
                )
            )
        }
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

    fun isBanklessSupported(): Boolean {
        return AppConfig.load().banklessToken.trim().isNotBlank()
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
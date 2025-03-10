package agent.modes

import agent.ContextManager
import agent.ToolManager
import agent.blockchain.tool.BanklessTools
import agent.blockchain.tool.EtherscanTools
import agent.tool.GeneralTools
import agent.interaction.InteractionHandler
import agent.reason.BlockchainReasoner
import agent.tool.LoadedTool
import agent.tool.ToolAllowedStatus
import config.AppConfig
import org.slf4j.LoggerFactory

/**
 * Mode for blockchain-related operations.
 * Allows querying contract source code, ABIs, and other blockchain data.
 */
class BlockchainMode(
    private val reasoner: BlockchainReasoner,
    private val toolManager: ToolManager,
    contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
) : Mode, Interactor {


    init {
        GeneralTools(interactionHandler).register(toolManager)

        if (isEtherscanSupported()) {
            EtherscanTools(interactionHandler, contextManager).register(toolManager)
        }
        if (isBanklessSupported()) {
            BanklessTools(interactionHandler).register(toolManager)
        }
    }

    val logger = LoggerFactory.getLogger(this::class.java)

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

    fun isEtherscanSupported(): Boolean {
        return AppConfig.load().etherscan.baseApiKey.isNotBlank() || AppConfig.load().etherscan.ethereumApiKey.isNotBlank()
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
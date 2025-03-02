package agent.modes

import agent.ToolManager
import agent.blockchain.bankless.model.input.Input
import agent.blockchain.bankless.model.output.Output
import agent.blockchain.tool.BanklessTools
import agent.blockchain.tool.EtherscanTools
import agent.tool.GeneralTools
import agent.interaction.InteractionHandler
import agent.reason.BlockchainReasoner
import ai.Action
import config.AppConfig
import org.slf4j.LoggerFactory

/**
 * Mode for blockchain-related operations.
 * Allows querying contract source code, ABIs, and other blockchain data.
 */
class BlockchainMode(
    private val reasoner: BlockchainReasoner,
    private val interactionHandler: InteractionHandler,
) : Mode, Interactor {

    private val toolManager: ToolManager = ToolManager()

    init {
        GeneralTools(interactionHandler).register(toolManager)

        if (isEtherscanSupported()) {
            EtherscanTools(interactionHandler).register(toolManager)
        }
        if (isBanklessSupported()) {
            BanklessTools().register(toolManager)
        }
    }

    val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun perform(request: String): String {
        return reasoner.askQuestion(request, toolManager.tools.map { it.action }).reply
    }

    override suspend fun getLoadedActionNames(): List<String> {
        return toolManager.tools.map { it.action }.filter(Action::showUpInTools).map {
            it.name
        } + toolManager.toolCallbacks.map {
            it.toolDefinition.name()
        }
    }

    data class ReadContractRequest(
        val network: String,
        val address: String,
        val method: String,
        val inputs: List<Input> = emptyList(),
        val outputs: List<Output> = emptyList()
    )

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
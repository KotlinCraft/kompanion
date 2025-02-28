package agent.modes

import agent.ToolManager
import agent.blockchain.bankless.BanklessClient
import agent.blockchain.bankless.EvmReadContractStateRequest
import agent.blockchain.bankless.model.input.Input
import agent.blockchain.bankless.model.output.Output
import agent.blockchain.tool.BanklessTools
import agent.blockchain.tool.EtherscanTools
import agent.blockchain.tool.GeneralTools
import agent.interaction.InteractionHandler
import agent.reason.BlockchainReasoner
import ai.Action
import ai.ActionMethod
import arrow.core.getOrElse
import blockchain.etherscan.EtherscanClientManager
import com.bankless.claimable.rest.vo.ClaimableVO
import config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.util.ReflectionUtils

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
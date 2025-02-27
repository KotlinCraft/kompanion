package agent.modes

import agent.interaction.InteractionHandler
import agent.reason.BlockchainReasoner
import agent.reason.Reasoner
import ai.Action
import ai.ActionMethod
import arrow.core.getOrElse
import blockchain.etherscan.EtherscanClientManager
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
    private val etherscanClientManager: EtherscanClientManager,
    private val interactionHandler: InteractionHandler
) : Mode, Interactor {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun perform(request: String): String {
        return reasoner.askQuestion(
            request, listOf(
                get_contract_source
            )
        ).reply
    }

    val get_contract_source = Action(
        "get_contract_source",
        """Get the contract source for an address.""".trimMargin(),
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "getContractSource", GetContractSourceRequest::class.java),
            this
        )
    )

    data class GetContractSourceRequest(
        val network: String,
        val address: String
    )

    data class GetContractSourceResponse(
        val source: String
    )

    fun getContractSource(request: GetContractSourceRequest): GetContractSourceResponse {
        val source = runBlocking(Dispatchers.IO) {
            etherscanClientManager.getClient(request.network)?.getContractSource(request.address)?.map {
                it.result.joinToString {
                    it.sourceCode
                }
            }?.getOrElse {
                "no source"
            }
        }
        return GetContractSourceResponse(source ?: "no source found")
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
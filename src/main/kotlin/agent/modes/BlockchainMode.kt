package agent.modes

import agent.blockchain.bankless.BanklessClient
import agent.blockchain.bankless.EvmReadContractStateRequest
import agent.blockchain.bankless.model.input.Input
import agent.blockchain.bankless.model.output.Output
import agent.interaction.InteractionHandler
import agent.reason.BlockchainReasoner
import agent.reason.Reasoner
import ai.Action
import ai.ActionMethod
import arrow.core.getOrElse
import blockchain.etherscan.EtherscanClientManager
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
    private val etherscanClientManager: EtherscanClientManager,
    private val banklessClient: BanklessClient,
    private val interactionHandler: InteractionHandler
) : Mode, Interactor {

    override suspend fun perform(request: String): String {
        // Define actions based on available features
        val actions = mutableListOf(get_contract_source)
        
        // Add Bankless actions only if supported
        if (isBanklessSupported()) {
            actions.add(read_contract)
        }
        
        return reasoner.askQuestion(request, actions).reply
    }

    val get_contract_source = Action(
        "get_contract_source",
        """Get the contract source for an address.""".trimMargin(),
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "getContractSource", GetContractSourceRequest::class.java),
            this
        )
    )

    val read_contract = Action(
        "read_contract",
        """Read a contract's state using Bankless API.
            |
            example: 
            |{
  "contract": "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913",
  "method": "implementation",
  "network": "base",
  "inputs": [
  ],
  "outputs": [
    {
      "type": "address"
    }
  ]
}
            | Only attempt to read if the fucntion is fully supported by our input and output types.
            | supported input types: address, bytes4, bytes32, input
            | supported output types: bool, bytes4, string, uint256
        """.trimMargin(),
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "readContract", ReadContractRequest::class.java),
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

    data class ReadContractRequest(
        val network: String,
        val address: String,
        val method: String,
        val inputs: List<Input<*>> = emptyList(),
        val outputs: List<Output<*>> = emptyList()
    )

    data class ReadContractResponse(
        val results: List<Map<String, Any>>?,
        val error: String?
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

    fun readContract(request: ReadContractRequest): ReadContractResponse {
        if (!isBanklessSupported()) {
            return ReadContractResponse(null, "Bankless API is not configured")
        }

        return runBlocking(Dispatchers.IO) {
            try {
                val banklessRequest = EvmReadContractStateRequest(
                    contractAddress = request.address,
                    method = request.method,
                    inputs = request.inputs,
                    outputs = request.outputs
                )
                
                val result = banklessClient.readContractState(request.network, banklessRequest)
                
                result.fold(
                    { error -> ReadContractResponse(null, error) },
                    { results -> 
                        val mappedResults = results.map {
                            mapOf(
                                "value" to it.value,
                                "type" to it.type
                            )
                        }
                        ReadContractResponse(mappedResults, null) 
                    }
                )
            } catch (e: Exception) {
                ReadContractResponse(null, "Error reading contract: ${e.message}")
            }
        }
    }

    fun isBanklessSupported(): Boolean {
        return AppConfig.load().banklessToken.trim().isNotBlank()
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
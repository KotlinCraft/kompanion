package agent.blockchain.tool

import agent.blockchain.bankless.BanklessClient
import agent.blockchain.bankless.EvmReadContractStateRequest
import agent.blockchain.tool.domain.GetProxyRequest
import agent.blockchain.tool.domain.GetProxyResponse
import agent.blockchain.tool.domain.ReadContractResponse
import agent.interaction.InteractionHandler
import agent.modes.BlockchainMode.ReadContractRequest
import agent.modes.Interactor
import agent.tool.Tool
import agent.tool.ToolsProvider
import ai.Action
import ai.ActionMethod
import arrow.core.getOrElse
import com.bankless.claimable.rest.vo.ClaimableVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.util.ReflectionUtils

class BanklessTools(private val interactionHandler: InteractionHandler) : ToolsProvider, Interactor {

    private val logger = LoggerFactory.getLogger(this::class.java)

    val banklessClient = BanklessClient()

    val get_claimables = Action(
        "get_claimables",
        "Fetch the claimables for an address. " +
                "Claimables are opportunities a user has to claim tokens. It's something they can do onchain." +
                "This action will return a list of claimables for the given address. " +
                "Claimed tokens are included, but only for historical reasons.",
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "get_claimables", String::class.java),
            this
        )
    )

    fun get_claimables(address: String): List<ClaimableVO> {
        return runBlocking(Dispatchers.IO) {
            banklessClient.getClaimables(address).getOrElse { emptyList() }
        }.also {
            logger.info("Fetched it.size claimables for address address")
        }
    }

    val read_contract = Action(
        "read_contract",
        """ Call to the Bankless API to read a contract's state. The request and response must equally match the supported types. This tool requires ABI and source of the contract to succeed. Several types are supported, including:
    {
      "contract": "address",
      "method": "implementation",
      "network": "base",
      "inputs": [],
      "outputs": [
        {
          "type": "address"
        }
      ]
    }
     Don't call a function if you didn't get a source or ABI for the contract.
     supported input types: address, bytes4, bytes32, input
     supported output types: bool, bytes4, string, uint256
        """.trimMargin(),
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "readContract", ReadContractRequest::class.java),
            this
        )
    )

    fun readContract(request: ReadContractRequest): ReadContractResponse {
        return try {
            val banklessRequest = EvmReadContractStateRequest(
                contract = request.address,
                method = request.method,
                inputs = request.inputs,
                outputs = request.outputs
            )

            val result =
                runBlocking(Dispatchers.IO) { banklessClient.readContractState(request.network, banklessRequest) }

            result.fold(
                { error -> ReadContractResponse(null, error) },
                { results ->
                    val mappedResults = results.map {
                        mapOf(
                            "value" to it.value,
                            "type" to it.type,
                            "error" to (it.error ?: "")
                        )
                    }
                    ReadContractResponse(mappedResults, null)
                }
            )
        } catch (e: Exception) {
            ReadContractResponse(null, "Error reading contract: ${e.message}")
        }
    }

    val get_proxy = Action(
        "get_proxy",
        "Retrieve the proxy address for a given contract and network. The action takes network and contract as inputs.",
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "getProxy", GetProxyRequest::class.java),
            this
        )
    )

    fun getProxy(request: GetProxyRequest): GetProxyResponse {
        return try {
            val result = runBlocking(Dispatchers.IO) { banklessClient.getProxy(request.network, request.contract) }

            result.fold(
                { error -> GetProxyResponse(null, error) },
                { proxy -> GetProxyResponse(proxy.implementation, null) }
            )
        } catch (e: Exception) {
            GetProxyResponse(null, "Error retrieving proxy: ${e.message}")
        }
    }

    override fun getTools(): List<Tool> {
        return listOf(
            Tool(get_claimables),
            Tool(read_contract),
            Tool(get_proxy)
        )
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}

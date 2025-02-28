package agent.blockchain.tool

import agent.blockchain.bankless.BanklessClient
import agent.blockchain.bankless.EvmReadContractStateRequest
import agent.blockchain.tool.domain.ReadContractResponse
import agent.modes.BlockchainMode.ReadContractRequest
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

class BanklessTools : ToolsProvider {

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
            logger.info("Fetched ${it.size} claimables for address $address")
        }
    }

    val read_contract = Action(
        "read_contract",
        """Read a contract's state using Bankless API.
            |
            example: 
            |{
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
            | Only attempt to read if the function is fully supported by our input and output types.
            | Don't call a function if you didn't get a source or ABI for the contract.
            | supported input types: address, bytes4, bytes32, input
            | supported output types: bool, bytes4, string, uint256
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

    override fun getTools(): List<Tool> {
        return listOf(
            Tool(get_claimables),
            Tool(read_contract)
        )
    }
}
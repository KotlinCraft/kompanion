package agent.blockchain.tool

import agent.blockchain.bankless.BanklessClient
import agent.blockchain.bankless.EvmReadContractStateRequest
import agent.blockchain.bankless.model.contract.ReadContractRequest
import agent.blockchain.bankless.model.token.FungibleTokenVO
import agent.blockchain.tool.domain.GetProxyRequest
import agent.blockchain.tool.domain.GetProxyResponse
import agent.blockchain.tool.domain.ReadContractResponse
import agent.interaction.InteractionHandler
import agent.interaction.ToolStatus
import agent.modes.Interactor
import agent.tool.Tool
import agent.tool.ToolAllowedStatus
import agent.tool.ToolsProvider
import ai.Action
import ai.ActionMethod
import arrow.core.getOrElse
import com.bankless.claimable.rest.vo.ClaimableVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallbacks
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.util.ReflectionUtils
import ui.chat.ContractReadIndicator
import ui.chat.GetProxyIndicator
import ui.chat.TokenInformationIndicator

class BanklessTools(private val interactionHandler: InteractionHandler) : ToolsProvider, Interactor {

    private val logger = LoggerFactory.getLogger(this::class.java)

    val banklessClient = BanklessClient()

    @org.springframework.ai.tool.annotation.Tool(
        name = "get_claimables",
        description =
            """Fetch the claimables for an address. Claimables are opportunities a user has to claim tokens. It's something they can do onchain.This action will return a list of claimables for the given address. Claimed tokens are included, but only for historical reasons.""",
    )
    fun get_claimables(
        @ToolParam(
            required = true,
            description = "address to fetch claimables for"
        ) address: String
    ): List<ClaimableVO> {
        return runBlocking(Dispatchers.IO) {
            banklessClient.getClaimables(address).getOrElse { emptyList() }
        }
    }

    @org.springframework.ai.tool.annotation.Tool(
        name = "read_contract",
        description = """ Call to the Bankless API to read a contract's state and interact with it. Use the Abi types and function to interact (read only) with a contract."""
    )
    fun readContract(
        @ToolParam(
            required = true,
            description = """read and interact with a contract based on the address, method, network, inputs, and outputs"""
        ) request: ReadContractRequest
    ): ReadContractResponse {
        // Show RUNNING indicator
        val toolId = runBlocking(Dispatchers.IO) {
            customToolUsage(
                toolIndicator = {
                    ContractReadIndicator(
                        request.address,
                        request.method,
                        request.network,
                        ToolStatus.RUNNING,
                    )
                })
        }

        return try {
            val banklessRequest = EvmReadContractStateRequest(
                contract = request.address, method = request.method, inputs = request.inputs, outputs = request.outputs
            )

            val result =
                runBlocking(Dispatchers.IO) { banklessClient.readContractState(request.network, banklessRequest) }

            result.fold({ error ->
                // Create error response
                val errorResponse = ReadContractResponse(null, error)

                // Show FAILED indicator
                runBlocking(Dispatchers.IO) {
                    customToolUsage(
                        id = toolId, toolIndicator = {
                            ContractReadIndicator(
                                request.address,
                                request.method,
                                request.network,
                                ToolStatus.FAILED,
                                null,
                                error,
                                errorResponse
                            )
                        })
                }

                errorResponse
            }, { results ->
                val mappedResults = results.map {
                    mapOf(
                        "value" to it.value, "type" to it.type, "error" to (it.error ?: "")
                    )
                }

                // Create successful response
                val response = ReadContractResponse(mappedResults, null)

                // Show COMPLETED indicator
                runBlocking(Dispatchers.IO) {
                    customToolUsage(
                        id = toolId, toolIndicator = {
                            ContractReadIndicator(
                                request.address,
                                request.method,
                                request.network,
                                ToolStatus.COMPLETED,
                                null,
                                null,
                                response
                            )
                        })
                }

                response
            })
        } catch (e: Exception) {
            // Create error response
            val errorResponse = ReadContractResponse(null, "Error reading contract: ${e.message}")

            // Show FAILED indicator for exceptions
            runBlocking(Dispatchers.IO) {
                customToolUsage(
                    id = toolId, toolIndicator = {
                        ContractReadIndicator(
                            request.address,
                            request.method,
                            request.network,
                            ToolStatus.FAILED,
                            null,
                            "Error reading contract: ${e.message}",
                            errorResponse
                        )
                    })
            }

            errorResponse
        }
    }

    @org.springframework.ai.tool.annotation.Tool(
        name = "get_proxy",
        description = "Retrieve the proxy address for a given contract and network. The action takes network and contract as inputs."
    )
    fun getProxy(
        @ToolParam(
            required = true,
            description = "which network and address do we fetch the proxy for"
        ) request: GetProxyRequest
    ): GetProxyResponse {
        val toolId = runBlocking(Dispatchers.IO) {
            customToolUsage(
                toolIndicator = {
                    GetProxyIndicator(
                        request.contract,
                        request.network,
                        ToolStatus.RUNNING,
                    )
                })
        }

        return try {
            val result = runBlocking(Dispatchers.IO) { banklessClient.getProxy(request.network, request.contract) }

            result.fold({ error ->
                // Show FAILED indicator
                runBlocking(Dispatchers.IO) {
                    customToolUsage(
                        id = toolId, toolIndicator = {
                            GetProxyIndicator(
                                request.contract, request.network, ToolStatus.FAILED, null, error
                            )
                        })
                }
                GetProxyResponse(null, error)
            }, { proxy ->
                // Show COMPLETED indicator
                runBlocking(Dispatchers.IO) {
                    customToolUsage(
                        id = toolId, toolIndicator = {
                            GetProxyIndicator(
                                request.contract,
                                request.network,
                                ToolStatus.COMPLETED,
                                proxy.implementation,
                            )
                        })
                }
                GetProxyResponse(proxy.implementation, null)
            })
        } catch (e: Exception) {
            // Show FAILED indicator for exceptions
            runBlocking(Dispatchers.IO) {
                customToolUsage(
                    id = toolId, toolIndicator = {
                        GetProxyIndicator(
                            request.contract,
                            request.network,
                            ToolStatus.FAILED,
                            null,
                            "Error retrieving proxy: ${e.message}"
                        )
                    })
            }
            GetProxyResponse(null, "Error retrieving proxy: ${e.message}")
        }
    }

    @org.springframework.ai.tool.annotation.Tool(
        name = "fetch_token_information",
        description = "Fetch token information for a token deployed to a chain and address.",
    )
    fun fetchTokenInformation(
        @ToolParam(required = true, description = "chain of the network") chain: String,
        @ToolParam(required = true, description = "address to check fetch information ") address: String
    ): FungibleTokenVO? {
        return runBlocking(Dispatchers.IO) {
            val toolId = customToolUsage(
                toolIndicator = {
                    TokenInformationIndicator(
                        address,
                        chain,
                        ToolStatus.RUNNING,
                    )
                })
            banklessClient.fetchTokenInformation(chain, address).fold({ error ->
                // Show FAILED indicator
                customToolUsage(toolId) {
                    TokenInformationIndicator(
                        address, chain, ToolStatus.FAILED, null, "Error fetching token information: $error"
                    )
                }
                null
            }, { token ->
                // Show COMPLETED indicator
                customToolUsage(toolId) {
                    TokenInformationIndicator(
                        address,
                        chain,
                        ToolStatus.COMPLETED,
                        token,
                    )
                }
                token
            })
        }
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
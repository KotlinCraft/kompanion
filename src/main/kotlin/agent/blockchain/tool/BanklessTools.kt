package agent.blockchain.tool

import agent.ContextManager
import agent.blockchain.bankless.BanklessClient
import agent.blockchain.bankless.EvmReadContractStateRequest
import agent.blockchain.bankless.model.contract.Output
import agent.blockchain.bankless.model.contract.ReadContractRequest
import agent.blockchain.bankless.model.event.EthLog
import agent.blockchain.bankless.model.event.GetEventLogsRequest
import agent.blockchain.bankless.model.token.FungibleTokenVO
import agent.blockchain.tool.domain.*
import agent.domain.context.ContextFile
import agent.interaction.InteractionHandler
import agent.interaction.ToolStatus
import agent.modes.Interactor
import agent.tool.ToolsProvider
import arrow.core.getOrElse
import blockchain.etherscan.ui.ContractAbiFetchIndicator
import blockchain.etherscan.ui.ContractSourceFetchIndicator
import com.bankless.claimable.rest.vo.ClaimableVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.annotation.ToolParam
import ui.chat.ContractReadIndicator
import ui.chat.GetEventLogsIndicator
import ui.chat.GetProxyIndicator
import ui.chat.TokenInformationIndicator
import java.util.*

class BanklessTools(
    private val interactionHandler: InteractionHandler,
    private val contextManager: ContextManager,
) : ToolsProvider, Interactor {

    private val logger = LoggerFactory.getLogger(this::class.java)

    val banklessClient = BanklessClient()

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
            required = true, description = "which network and address do we fetch the proxy for"
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
        name = "get_event_logs",
        description = """Fetch event logs for a given address and topic. 
            | Requires the source to navigate and understand the logs. Don't "assume" certain events are supported, but verify. 
            | When fetching events on proxy contracts, use the proxy, not the implementation
        """
    )
    fun getEventLogs(
        @ToolParam(
            required = true, description = "network to fetch logs for"
        ) network: String, @ToolParam(
            required = true, description = "addresses to fetch logs for"
        ) addresses: List<String>, @ToolParam(
            required = true, description = "topic to fetch logs for. Use tool to create topic from abi event. Don't assume topics, but build them using build_event_topic"
        ) topic: String, @ToolParam(
            required = false, description = "optional topics to fetch logs for"
        ) optionalTopics: List<String?>? = emptyList()
    ): GetEventLogsResponse {
        // Show RUNNING indicator
        val toolId = runBlocking(Dispatchers.IO) {
            customToolUsage(
                toolIndicator = {
                    GetEventLogsIndicator(
                        addresses,
                        topic,
                        network,
                        ToolStatus.RUNNING
                    )
                })
        }

        return try {
            val result = runBlocking(Dispatchers.IO) {
                banklessClient.getEvents(
                    network, GetEventLogsRequest(
                        addresses, topic, optionalTopics
                    )
                )
            }

            result.fold({ error ->
                // Show FAILED indicator
                runBlocking(Dispatchers.IO) {
                    customToolUsage(
                        id = toolId, toolIndicator = {
                            GetEventLogsIndicator(
                                addresses,
                                topic,
                                network,
                                ToolStatus.FAILED,
                                null,
                                error
                            )
                        })
                }
                GetEventLogsResponse(error = error)
            }, { logs ->
                // Show COMPLETED indicator
                runBlocking(Dispatchers.IO) {
                    customToolUsage(
                        id = toolId, toolIndicator = {
                            GetEventLogsIndicator(
                                addresses,
                                topic,
                                network,
                                ToolStatus.COMPLETED,
                                logs,
                                null
                            )
                        })
                }
                GetEventLogsResponse(ethLog = logs)
            })
        } catch (e: Exception) {
            // Show FAILED indicator for exceptions
            runBlocking(Dispatchers.IO) {
                customToolUsage(
                    id = toolId, toolIndicator = {
                        GetEventLogsIndicator(
                            addresses,
                            topic,
                            network,
                            ToolStatus.FAILED,
                            null,
                            "Error retrieving event logs: ${e.message}"
                        )
                    })
            }
            GetEventLogsResponse(error = "Error retrieving event logs: ${e.message}")
        }
    }

    @org.springframework.ai.tool.annotation.Tool(
        name = "build_event_topic",
        description = """Build an event topic signature based on event name and arguments.  
            | Keep the order of the types and indexed properties in the arguments list.
            |Used to calculate the topic signature for blockchain event lookup."""
    )
    fun buildEventTopic(
        @ToolParam(
            required = true, description = "blockchain network (e.g., ethereum, base)"
        ) chain: String,
        @ToolParam(
            required = true, description = "name of the event"
        ) eventName: String,
        @ToolParam(
            required = true,
            description = """object contains a list of output parameters with type and indexed properties. 
                |Example: {"type": "uint256", "indexed": false}
            """
        ) methodArguments: MethodArguments
    ): BuildEventTopicResponse {
        return try {
            val result = runBlocking(Dispatchers.IO) {
                banklessClient.buildEventTopic(chain, eventName, methodArguments.outputs.map {
                    Output(it.type, it.indexed ?: false)
                })
            }

            result.fold({ error ->
                logger.info(error)
                BuildEventTopicResponse(null, error)
            }, { topic ->
                logger.info("found topic: $topic for event $eventName and arguments ${methodArguments.outputs.map { "type: ${it.type}, indexed: ${it.indexed}" }}")
                BuildEventTopicResponse(topic, null)
            })
        } catch (e: Exception) {
            logger.info("Error building event topic: ${e.message}")
            BuildEventTopicResponse(null, "Error building event topic: ${e.message}")
        }
    }

    data class GetEventLogsResponse(
        val error: String? = null, val ethLog: EthLog? = null
    )

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

    @org.springframework.ai.tool.annotation.Tool(
        name = "get_contract_source",
        description = "Get the contract source for an address."
    )
    fun getContractSource(
        @ToolParam(
            required = true,
            description = "address and network to fetch contract source for"
        ) request: GetContractSourceRequest
    ): GetContractSourceResponse = runBlocking(Dispatchers.IO) {

        val toolId = customToolUsage {
            ContractSourceFetchIndicator(
                request.address, request.network, ToolStatus.RUNNING
            )
        }

        val source = banklessClient.getSource(request.network, request.address).map {
            it.result.joinToString {
                it.sourceCode
            }
        }?.map {
            cleanSolidityCode(it).also {
                if (it.isNotBlank()) {
                    contextManager.updateFiles(
                        listOf(
                            ContextFile(UUID.randomUUID(), request.address + "_" + request.network + "_source.sol", it)
                        )
                    )
                }
            }
        }?.getOrElse {
            failedContractSource(toolId, request)
            "not a contract or no source found"
        }

        if ((source ?: "").split(" ").size > 10000) {
            failedContractSource(toolId, request)
            GetContractSourceResponse("source code too long, use get_contract_abi instead")
        }

        customToolUsage(toolId) {
            ContractSourceFetchIndicator(
                request.address, request.network, ToolStatus.COMPLETED
            )
        }
        GetContractSourceResponse(source ?: "not a contract or no source found")
    }

    private fun failedContractSource(toolId: UUID, request: GetContractSourceRequest) {
        runBlocking(Dispatchers.IO) {
            customToolUsage(toolId) {
                ContractSourceFetchIndicator(
                    request.address, request.network, ToolStatus.FAILED
                )
            }
        }
    }

    @org.springframework.ai.tool.annotation.Tool(
        name = "get_contract_abi",
        description = "Get the contract ABI for an address. Use this if the source was not available"
    )
    fun getContractAbi(request: GetContractAbiRequest): GetContractAbiResponse {
        val abi = runBlocking(Dispatchers.IO) {
            banklessClient.getAbi(request.network, request.address).map {
                it.result
            }?.also {
                it.onRight {
                    runBlocking(Dispatchers.IO) {
                        customToolUsage(
                            message = "Fetched contract ABI for address ${request.address} on network ${request.network}",
                            toolIndicator = {
                                ContractAbiFetchIndicator(
                                    request.address, request.network, ToolStatus.COMPLETED
                                )
                            },
                        )
                    }

                    if (it.isNotBlank()) {
                        contextManager.updateFiles(
                            listOf(
                                ContextFile(
                                    UUID.randomUUID(),
                                    request.address + "_" + request.network + "_abi.json",
                                    it
                                )
                            )
                        )
                    }
                }
            }?.getOrElse {
                "not a contract or no ABI found"
            }
        }
        return GetContractAbiResponse(abi ?: "not a contract or no ABI found")
    }

    fun cleanSolidityCode(code: String): String {
        // 1. Remove multi-line comments: /* ... */
        val withoutMultilineComments = code.replace(Regex("""/\*[\s\S]*?\*/"""), "")

        // 2. Remove single-line comments: // ...
        val withoutSingleLineComments = withoutMultilineComments.replace(Regex("""//.*"""), "")

        // 3. Split into lines, trim whitespace, and remove empty lines
        val lines = withoutSingleLineComments
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // 4. Join the lines with a single newline (or you could join with a space)
        return lines.joinToString("\n")
    }
}
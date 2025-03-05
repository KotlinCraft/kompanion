package agent.blockchain.tool

import agent.ContextManager
import agent.blockchain.tool.domain.GetContractAbiRequest
import agent.blockchain.tool.domain.GetContractAbiResponse
import agent.blockchain.tool.domain.GetContractSourceRequest
import agent.blockchain.tool.domain.GetContractSourceResponse
import agent.domain.context.ContextFile
import agent.interaction.InteractionHandler
import agent.interaction.ToolStatus
import agent.modes.Interactor
import agent.tool.Tool
import agent.tool.ToolsProvider
import ai.Action
import ai.ActionMethod
import arrow.core.getOrElse
import blockchain.etherscan.EtherscanClientManager
import blockchain.etherscan.ui.ContractAbiFetchIndicator
import blockchain.etherscan.ui.ContractSourceFetchIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.util.ReflectionUtils

class EtherscanTools(
    private val interactionHandler: InteractionHandler,
    private val contextManager: ContextManager
) : ToolsProvider, Interactor {

    val etherscanClientManager = EtherscanClientManager()

    val get_contract_source = Action(
        "get_contract_source", """Get the contract source for an address.""".trimMargin(), ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "getContractSource", GetContractSourceRequest::class.java),
            this
        )
    )

    val get_contract_abi = Action(
        "get_contract_abi",
        """Get the contract ABI for an address. Use this if the source was not available""".trimMargin(),
        ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "getContractAbi", GetContractAbiRequest::class.java),
            this
        )
    )

    fun getContractSource(request: GetContractSourceRequest): GetContractSourceResponse {


        val source = runBlocking(Dispatchers.IO) {
            val toolId = customToolUsage {
                ContractSourceFetchIndicator(
                    request.address, request.network, ToolStatus.RUNNING
                )
            }

            etherscanClientManager.getClient(request.network)?.getContractSource(request.address)?.map {
                it.result.joinToString {
                    it.sourceCode
                }
            }?.map {
                cleanSolidityCode(it).also {
                    if (it.isNotBlank()) {
                        runBlocking(Dispatchers.IO) {
                            customToolUsage(toolId) {
                                ContractSourceFetchIndicator(
                                    request.address, request.network, ToolStatus.COMPLETED
                                )
                            }
                        }
                        contextManager.updateFiles(
                            listOf(
                                ContextFile(request.address + "_" + request.network + "_source.sol", it)
                            )
                        )
                    }
                }
            }?.getOrElse {
                runBlocking(Dispatchers.IO) {
                    customToolUsage(toolId) {
                        ContractSourceFetchIndicator(
                            request.address, request.network, ToolStatus.FAILED
                        )
                    }
                }
                "not a contract or no source found"
            }
        }

        if ((source ?: "").split(" ").size > 10000) {
            return GetContractSourceResponse("source code too long, use get_contract_abi instead")
        }
        return GetContractSourceResponse(source ?: "not a contract or no source found")
    }

    fun getContractAbi(request: GetContractAbiRequest): GetContractAbiResponse {
        val abi = runBlocking(Dispatchers.IO) {
            etherscanClientManager.getClient(request.network)?.getContractAbi(request.address)?.map {
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
                                ContextFile(request.address + "_" + request.network + "_abi.json", it)
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

    override fun getTools(): List<Tool> {
        return listOf(
            Tool(get_contract_source),
            Tool(get_contract_abi)
        )
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
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
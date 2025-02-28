package agent.blockchain.tool

import agent.blockchain.tool.domain.GetContractSourceRequest
import agent.blockchain.tool.domain.GetContractSourceResponse
import agent.interaction.InteractionHandler
import agent.modes.Interactor
import agent.tool.Tool
import agent.tool.ToolsProvider
import ai.Action
import ai.ActionMethod
import arrow.core.getOrElse
import blockchain.etherscan.EtherscanClientManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.util.ReflectionUtils

class EtherscanTools(private val interactionHandler: InteractionHandler) : ToolsProvider, Interactor {

    val etherscanClientManager = EtherscanClientManager()

    val get_contract_source = Action(
        "get_contract_source", """Get the contract source for an address.""".trimMargin(), ActionMethod(
            ReflectionUtils.findMethod(this::class.java, "getContractSource", GetContractSourceRequest::class.java),
            this
        )
    )

    fun getContractSource(request: GetContractSourceRequest): GetContractSourceResponse {
        val source = runBlocking(Dispatchers.IO) {
            etherscanClientManager.getClient(request.network)?.getContractSource(request.address)?.map {
                it.result.joinToString {
                    it.sourceCode
                }
            }?.also {
                it.onRight {
                    runBlocking(Dispatchers.IO) {
                        sendMessage("📜Fetched contract source for address ${request.address} on network ${request.network}")
                    }
                }
            }?.getOrElse {
                "not a contract or no source found"
            }
        }
        return GetContractSourceResponse(source ?: "not a contract or no source found")
    }

    override fun getTools(): List<Tool> {
        return listOf(
            Tool(get_contract_source)
        )
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
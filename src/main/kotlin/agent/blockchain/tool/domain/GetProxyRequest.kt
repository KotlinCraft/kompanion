package agent.blockchain.tool.domain

data class GetProxyRequest(
    val network: String,
    val contract: String
)

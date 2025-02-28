package agent.blockchain.tool.domain

data class GetContractSourceRequest(
    val network: String,
    val address: String
)
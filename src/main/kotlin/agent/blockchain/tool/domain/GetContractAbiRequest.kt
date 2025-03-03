package agent.blockchain.tool.domain

data class GetContractAbiRequest(
    val network: String,
    val address: String
)
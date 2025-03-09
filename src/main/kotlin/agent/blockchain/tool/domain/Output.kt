package agent.blockchain.tool.domain

data class OutputRequest(
    val type: String,
    val indexed: Boolean? = null
)
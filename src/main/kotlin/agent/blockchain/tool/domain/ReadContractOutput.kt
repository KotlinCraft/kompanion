package agent.blockchain.tool.domain

data class ReadContractOutput(
    val type: String,
    val components: List<ReadContractOutput>
)
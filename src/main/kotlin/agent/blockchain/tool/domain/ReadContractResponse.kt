package agent.blockchain.tool.domain

data class ReadContractResponse(
        val results: List<Map<String, Any>>?,
        val error: String?
    )
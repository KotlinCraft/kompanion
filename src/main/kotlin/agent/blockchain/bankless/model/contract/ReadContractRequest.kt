package agent.blockchain.bankless.model.contract

data class ReadContractRequest(
        val network: String,
        val address: String,
        val method: String,
        val inputs: List<Input> = emptyList(),
        val outputs: List<Output> = emptyList()
    )
package agent.blockchain.bankless.model.event

data class EthLog(
    val id: Long?,
    val jsonrpc: String?,
    val result: List<LogResult>,
    val error: Any?,
    val rawResponse: Any?,
    val logs: List<LogResult>?
)

data class LogResult(
    val removed: Boolean,
    val logIndex: Long,
    val transactionIndex: Long,
    val transactionHash: String,
    val blockHash: String,
    val blockNumber: Long,
    val address: String,
    val data: String,
    val type: String?,
    val topics: List<String>,
    val transactionIndexRaw: String,
    val logIndexRaw: String,
    val blockNumberRaw: String
)

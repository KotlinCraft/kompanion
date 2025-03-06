package agent.blockchain.bankless.model.event

class GetEventLogsRequest(
    val addresses: List<String>,
    val topic: String,
    val optionalTopics: List<String?>? = emptyList(),
)

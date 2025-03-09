package agent.blockchain.bankless.model.event

import agent.blockchain.bankless.model.contract.Output

/**
 * Request model for building an event topic.
 */
data class BuildEventTopicRequest(
    val name: String,
    val arguments: List<Output>
)
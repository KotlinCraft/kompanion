package agent.blockchain.tool.domain

/**
 * Response model for the build event topic operation.
 * 
 * @property topic The calculated event topic signature, null if operation failed
 * @property error The error message if operation failed, null if successful
 */
data class BuildEventTopicResponse(
    val topic: String?,
    val error: String?
)

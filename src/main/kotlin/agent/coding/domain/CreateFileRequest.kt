package agent.coding.domain

data class CreateFileRequest(
    val absolutePath: String,
    val content: String,
    val explanation: String
)

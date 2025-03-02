package agent.coding.domain

data class ModifyFileRequest(
    val path: String,
    val content: String
)

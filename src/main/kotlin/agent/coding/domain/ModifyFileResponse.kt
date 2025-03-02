package agent.coding.domain

data class ModifyFileResponse(
    val path: String,
    val error: String?,
    val newContent: String?
)
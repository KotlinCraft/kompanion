package agent.coding.domain

data class ModifyFileRequest(
    val absolutePath: String,
    val newContent: String
)

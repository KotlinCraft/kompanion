package agent.coding.domain

data class ModifyFileRequest(
    val absolutePath: String,
    val searchContent: String,
    val replaceContent: String
)

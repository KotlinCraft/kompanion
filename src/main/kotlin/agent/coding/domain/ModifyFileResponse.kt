package agent.coding.domain

data class ModifyFileResponse(
    val error: String?,
    val modifiedContent: String?,
    val anythingChanged: Boolean
)
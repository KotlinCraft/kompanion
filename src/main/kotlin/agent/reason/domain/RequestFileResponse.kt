package agent.reason.domain

data class RequestFileResponse(
    val exists: Boolean,
    val fullPath: String?,
    val content: String?,
    val relatedFiles: List<String>
)
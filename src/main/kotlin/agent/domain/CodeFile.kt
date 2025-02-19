package agent.domain
data class CodeFile(
    val path: String,
    val content: String,
    val language: String
)
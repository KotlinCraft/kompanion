package agent.domain

data class UserRequest(
    val instruction: String,
    val codeContext: List<CodeFile> = emptyList(),
)
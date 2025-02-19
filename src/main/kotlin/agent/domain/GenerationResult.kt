package agent.domain

data class GenerationResult(
    val fileChanges: List<FileChange>,
    val explanation: String,
    val metadata: Map<String, Any> = emptyMap()
)

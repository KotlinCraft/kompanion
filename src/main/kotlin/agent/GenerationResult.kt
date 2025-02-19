package agent

data class GenerationResult(
    val code: String,
    val explanation: String,
    val metadata: Map<String, Any>
)
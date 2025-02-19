package agent.domain

data class CodingAgentResponse(
    val fileChanges: List<FileChange>,
    val explanation: String,
    val nextSteps: List<String>,
    val confidence: Float
)
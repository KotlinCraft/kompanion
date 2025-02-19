package agent.domain

data class AgentResponse(
    val generatedCode: String,
    val explanation: String,
    val nextSteps: List<String>,
    val confidence: Float
)
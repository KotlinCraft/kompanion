package agent.domain

data class Understanding(
    val objective: String,
    val requiredFeatures: List<String>,
    val contextRelevance: Map<String, Float>
)
package agent.domain

data class CodeEvaluation(
    val meetsRequirements: Boolean,
    val confidence: Float,
    val suggestedImprovements: List<String>
)
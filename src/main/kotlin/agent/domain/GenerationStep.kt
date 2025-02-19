package agent.domain

data class GenerationStep(
    val action: String,
    val input: Map<String, Any>,
    val expectedOutput: String
)
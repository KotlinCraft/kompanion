package agent

data class GenerationStep(
    val action: String,
    val input: Map<String, Any>,
    val expectedOutput: String
)
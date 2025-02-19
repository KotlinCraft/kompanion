package agent

data class GenerationPlan(
    val steps: List<GenerationStep>,
    val expectedOutcome: String,
    val validationCriteria: List<String>
)
package agent

interface CodeGenerator {
    suspend fun generate(plan: GenerationPlan, currentCode: String = ""): GenerationResult
}
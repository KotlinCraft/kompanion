package agent

import agent.domain.GenerationPlan
import agent.domain.GenerationResult

interface CodeGenerator {

    suspend fun generate(plan: GenerationPlan, currentCode: String): GenerationResult
    suspend fun execute(
        plan: GenerationPlan,
        generationResult: GenerationResult,
    ): DefaultCodeGenerator.CodingResult
}

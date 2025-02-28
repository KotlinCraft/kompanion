package agent

import agent.coding.domain.CodingResult
import agent.domain.GenerationPlan

interface CodeGenerator {
    suspend fun execute(
        plan: GenerationPlan,
    ): CodingResult
}

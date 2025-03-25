package agent.coding

import agent.coding.domain.CodingResult
import agent.domain.GenerationPlan

interface CodeGenerator {
    suspend fun execute(request: String, plan: GenerationPlan?): CodingResult
}

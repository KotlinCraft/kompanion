package agent.coding

import agent.ContextManager
import agent.ToolManager
import agent.coding.domain.CodingResult
import agent.domain.GenerationPlan
import ai.LLMProvider
import org.springframework.core.ParameterizedTypeReference

class FlowCodeGenerator(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager,
    private val toolManager: ToolManager
) : CodeGenerator {

    override suspend fun execute(
        request: String,
        plan: GenerationPlan,
    ): CodingResult {
       //todo: implement
        throw NotImplementedError("Not implemented")
    }
}

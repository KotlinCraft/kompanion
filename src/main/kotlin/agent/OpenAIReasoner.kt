package agent

import agent.domain.*
import ai.Action
import ai.Model
import org.springframework.core.ParameterizedTypeReference

class OpenAIReasoner(
    private val model: Model
) : Reasoner {

    override fun analyzeRequest(request: UserRequest): Understanding {
        val prompt = """
            Analyze the following code-related request and extract key information.
            
            User Request: ${request.instruction}
            ${if (request.codeContext.isNotEmpty()) "Code Context:\n" + request.codeContext.joinToString("\n") { "File: ${it.path}\n${it.content}" } else ""}
            
            Provide a structured analysis including:
            1. The main objective
            2. Required features or changes
            3. Relevance score (0.0-1.0) for each provided context file
        """.trimIndent()

        return model.prompt(
            input = prompt,
            action = emptyList(),
            temperature = 0.7,
            parameterizedTypeReference = object : ParameterizedTypeReference<Understanding>() {}
        )
    }

    override fun createPlan(understanding: Understanding): GenerationPlan {
        // TODO: Implement later
        return GenerationPlan(
            steps = emptyList(),
            expectedOutcome = "",
            validationCriteria = emptyList()
        )
    }

    override fun evaluateCode(result: GenerationResult, understanding: Understanding): CodeEvaluation {
        // TODO: Implement later
        return CodeEvaluation(
            meetsRequirements = false,
            confidence = 0.0f,
            suggestedImprovements = emptyList()
        )
    }

    override fun learn(feedback: UserFeedback) {
        // TODO: Implement later
    }
}

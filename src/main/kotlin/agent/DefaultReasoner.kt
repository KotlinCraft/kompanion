package agent

import agent.domain.*
import ai.Model
import org.springframework.core.ParameterizedTypeReference

class DefaultReasoner(
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
        val prompt = """
            Based on the following understanding of a code request, create a detailed plan:
            
            Objective: ${understanding.mainObjective}
            Required Features: ${understanding.requiredFeatures.joinToString("\n")}
            
            Create a structured plan including:
            1. Step-by-step implementation steps
            2. Expected outcome
            3. Validation criteria
        """.trimIndent()

        return model.prompt(
            input = prompt,
            action = emptyList(),
            temperature = 0.7,
            parameterizedTypeReference = object : ParameterizedTypeReference<GenerationPlan>() {}
        )
    }

    override fun evaluateCode(result: GenerationResult, understanding: Understanding): CodeEvaluation {
        val prompt = """
            Evaluate the following generated code against the original requirements:
            
            Original Requirements:
            - Objective: ${understanding.mainObjective}
            - Features: ${understanding.requiredFeatures.joinToString("\n")}
            
            Generated Code:
            ${result.code}
            
            Explanation:
            ${result.explanation}
            
            Evaluate and provide:
            1. Whether code meets requirements (true/false)
            2. Confidence score (0.0-1.0)
            3. List of suggested improvements
        """.trimIndent()

        return model.prompt(
            input = prompt,
            action = emptyList(),
            temperature = 0.7,
            parameterizedTypeReference = object : ParameterizedTypeReference<CodeEvaluation>() {}
        )
    }

    override fun learn(feedback: UserFeedback) {
        // TODO: Implement later
    }
}

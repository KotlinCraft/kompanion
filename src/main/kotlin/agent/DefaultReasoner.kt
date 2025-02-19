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
            Based on the following understanding of a code request, create a detailed generation plan.
            
            Objective: ${understanding.objective}
            Required Features: 
            ${understanding.requiredFeatures.joinToString("\n") { "- $it" }}
            Context Relevance:
            ${understanding.contextRelevance.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}
            
            Create a detailed plan with the following structure:
            1. A list of specific implementation steps, each containing:
               - The action to perform
               - Required inputs
               - Expected output
            2. A clear description of the expected final outcome
            3. A list of specific validation criteria to verify the implementation
            
            Ensure the response is structured to match:
            {
              "steps": [
                {
                  "action": "string describing the step",
                  "input": {key-value map of inputs needed},
                  "expectedOutput": "string describing expected output"
                }
              ],
              "expectedOutcome": "detailed description of final result",
              "validationCriteria": ["list", "of", "verification", "points"]
            }
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

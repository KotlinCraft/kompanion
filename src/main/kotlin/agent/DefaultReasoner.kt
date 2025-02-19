package agent

import agent.domain.*
import ai.Action
import ai.ActionMethod
import ai.Model
import org.springframework.core.ParameterizedTypeReference
import org.springframework.util.ReflectionUtils

class DefaultReasoner(
    private val model: Model
) : Reasoner {

    val workingDirectory = "/opt/projects/kotlincraft/kompanion"

    override fun analyzeRequest(request: UserRequest): PotentialUnderstanding {
        val prompt = """
            Current working directory is: /opt/projects/kotlincraft/kompanion
            
            Analyze the following code-related request and extract key information.
            The content for various files (but not all) is provided for context. 
            If you think a file already exists but its contents was not provided yet, request it using "request_file_context".

            Make sure you have access to every file mentioned in the request before continuing.
            
            If the request is not clear, ask for more information.
            
            User Request: ${request.instruction}
            ${if (request.codeContext.isNotEmpty()) "Code Context:\n" + request.codeContext.joinToString("\n") { "File: ${it.path}\n${it.content}" } else ""}
            
            Provide a structured analysis including:
            1. The main objective
            2. Required features or changes
            3. Relevance score (0.0-1.0) for each provided context file
        """.trimIndent()

        return model.prompt(
            input = prompt,
            action = listOf(
                Action(
                    "request_file_context",
                    """Provide a file in context for the request. 
                        |If the file does not exist yet, the response will contain an exists: false, else, the file will be provided.
                        |Only request an exact filename. Example: UserManager.kt, main.py or instruction.txt""".trimMargin(),
                    ActionMethod(
                        ReflectionUtils.findMethod(this::class.java, "requestFileContext", String::class.java),
                        this
                    )
                )
            ),
            temperature = 0.7,
            parameterizedTypeReference = object : ParameterizedTypeReference<PotentialUnderstanding>() {}
        )
    }

    fun requestFileContext(file: String): RequestFileResponse {
        return RequestFileResponse(false, null, null)
    }

    data class RequestFileResponse(
        val exists: Boolean,
        val fullPath: String?,
        val content: String?
    )

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
            - Objective: ${understanding.objective}
            - Required Features:
            ${understanding.requiredFeatures.joinToString("\n") { "- $it" }}
            
            Generated Code:
            ${
            result.fileChanges.joinToString("\n\n") { fileChange ->
                when (fileChange) {
                    is FileChange.CreateFile -> "New File ${fileChange.path}:\n${fileChange.content}"
                    is FileChange.ModifyFile -> "Modify ${fileChange.path}:\n" +
                            fileChange.changes.joinToString("\n") { change ->
                                "- ${change.description}"
                            }
                }
            }
        }
            
            Explanation:
            ${result.explanation}
            
            Evaluate and provide a response in this structure:
            {
                "meetsRequirements": boolean,
                "confidence": float between 0.0 and 1.0,
                "suggestedImprovements": [
                    "list of specific improvements or recommendations"
                ]
            }
            
            Consider:
            1. Does the code fully implement all required features?
            2. Is the implementation correct and efficient?
            3. Are there any potential issues or areas for improvement?
            4. Does it follow best practices?
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

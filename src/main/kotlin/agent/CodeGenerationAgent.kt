package agent

import agent.domain.*
import org.slf4j.LoggerFactory

class CodeGenerationAgent(
    private val reasoner: Reasoner,
    private val codeGenerator: CodeGenerator,
) : CodeAgent {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun process(request: UserRequest): CodingAgentResponse {
        logger.info("Processing user request: ${request.instruction}")
        // 1. Analyze request and current context
        val understanding = reasoner.analyzeRequest(request)

        logger.debug("Understanding generated: {}", understanding)
        val plan = reasoner.createPlan(understanding)

        logger.debug("Generation plan created: {}", plan)
        var currentCode = ""
        var iterations = 0
        val maxIterations = 1

        while (iterations < maxIterations) {
            logger.info("Iteration $iterations: Generating code")
            val generationResult = codeGenerator.generate(plan, currentCode)

            logger.debug("Generation result: {}", generationResult)
            val evaluation = reasoner.evaluateCode(generationResult, understanding)

            if (evaluation.meetsRequirements) {
                logger.info("Requirements met. Returning successful response.")
                return CodingAgentResponse(
                    fileChanges = generationResult.fileChanges,
                    explanation = generationResult.explanation,
                    nextSteps = evaluation.suggestedImprovements,
                    confidence = evaluation.confidence
                )
            }

            logger.info("Requirements not met. Updating current code and retrying.")
            currentCode = generationResult.fileChanges.joinToString("\n") {
                when (it) {
                    is FileChange.CreateFile -> it.content
                    is FileChange.ModifyFile -> it.changes.joinToString("\n") { change ->
                        change.replaceContent
                    }
                }
            }
            iterations++
        }

        logger.warn("Reached maximum iterations. Returning best attempt.")
        return CodingAgentResponse(
            fileChanges = generationResult.fileChanges,
            explanation = "Reached maximum iterations. Current best attempt provided.",
            nextSteps = listOf(
                "Consider providing more specific requirements",
                "Review current output and provide feedback"
            ),
            confidence = 0.7f
        )
    }

    override fun addFeedback(feedback: UserFeedback) {
        reasoner.learn(feedback)
    }
}

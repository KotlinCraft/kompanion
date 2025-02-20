package agent

import agent.domain.*
import agent.interaction.AgentQuestion
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import org.slf4j.LoggerFactory

class CodingAgent(
    private val reasoner: Reasoner,
    private val codeGenerator: CodeGenerator,
    private val codeApplier: CodeApplier
) : CodeAgent {

    lateinit var interactionHandler: InteractionHandler

    override fun registerHandler(interactionHandler: InteractionHandler) {
        this.interactionHandler = interactionHandler
    }

    private suspend fun sendMessage(message: String) {
        interactionHandler.interact(AgentResponse(message))
    }

    private suspend fun askQuestion(question: String): String {
        return interactionHandler.interact(AgentQuestion(question))
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    private suspend fun confirmWithUser(message: String): Boolean {
        while (true) {
            val response = askQuestion("$message\nPlease respond with Y or N:")
            when (response.trim().uppercase()) {
                "Y" -> return true
                "N" -> return false
                else -> sendMessage("Invalid response. Please answer with Y or N.")
            }
        }
    }

    private fun formatFileChanges(fileChanges: List<FileChange>): String {
        return buildString {
            appendLine("File changes overview:")
            appendLine("----------------------")
            fileChanges.forEach { change ->
                when (change) {
                    is FileChange.CreateFile -> {
                        appendLine("📄 CREATE NEW FILE: ${change.path}")
                        appendLine("   Content preview (first 200 chars):")
                        appendLine("   ${change.content.take(200)}${if (change.content.length > 200) "..." else ""}")
                        appendLine()
                    }

                    is FileChange.ModifyFile -> {
                        appendLine("📝 MODIFY FILE: ${change.path}")
                        change.changes.forEachIndexed { index, modification ->
                            appendLine("   Change #${index + 1}: ${modification.description}")
                            appendLine("   - Remove: ${modification.searchContent.take(100)}${if (modification.searchContent.length > 100) "..." else ""}")
                            appendLine("   + Add: ${modification.replaceContent.take(100)}${if (modification.replaceContent.length > 100) "..." else ""}")
                        }
                        appendLine()
                    }
                }
            }
        }
    }

    override suspend fun process(request: UserRequest): CodingAgentResponse {
        logger.info("Processing user request: ${request.instruction}")
        sendMessage("Analyzing your request...")
        // 1. Analyze request and current context
        val understanding = reasoner.analyzeRequest(request)
        sendMessage("I understand you want to: ${understanding.objective}")

        logger.debug("Understanding generated: {}", understanding)
        val plan = reasoner.createPlan(understanding)

        logger.debug("Generation plan created: {}", plan)
        var currentCode = ""
        var iterations = 0
        val maxIterations = 1

        var generationResult: GenerationResult? = null

        while (iterations < maxIterations) {
            logger.info("Iteration $iterations: Generating code")
            sendMessage("Generating code (iteration ${iterations + 1})...")
            generationResult = codeGenerator.generate(plan, currentCode)

            logger.debug("Generation result: {}", generationResult)
            val evaluation = reasoner.evaluateCode(generationResult, understanding)

            if (evaluation.meetsRequirements) {
                logger.info("Requirements met. Asking for user confirmation.")
                sendMessage("I've generated code that meets all requirements. Here's what I'm planning to change:")
                sendMessage(formatFileChanges(generationResult.fileChanges))
                sendMessage("\nExplanation of changes:")
                sendMessage(generationResult.explanation)

                val userConfirmed = confirmWithUser("Would you like me to apply these changes?")
                if (!userConfirmed) {
                    logger.info("User rejected changes.")
                    return CodingAgentResponse(
                        fileChanges = emptyList(),
                        explanation = "Changes were rejected by user.",
                        nextSteps = listOf("Consider providing different requirements or explaining what wasn't right"),
                        confidence = 0.9f
                    )
                }

                generationResult.fileChanges.forEach {
                    val result = codeApplier.apply(it)
                    logger.info("did it work? $result")
                }

                logger.info("User confirmed changes. Returning successful response.")
                sendMessage("Proceeding with the changes!")
                return CodingAgentResponse(
                    fileChanges = generationResult.fileChanges,
                    explanation = generationResult.explanation,
                    nextSteps = evaluation.suggestedImprovements,
                    confidence = evaluation.confidence
                )
            }

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

        return CodingAgentResponse(
            fileChanges = generationResult!!.fileChanges,
            explanation = "Reached maximum iterations. Current best attempt provided.",
            nextSteps = listOf(
                "Consider providing more specific requirements",
                "Review current output and provide feedback"
            ),
            confidence = 0.7f
        )
    }

    override suspend fun addFeedback(feedback: UserFeedback) {
        reasoner.learn(feedback)
    }
}

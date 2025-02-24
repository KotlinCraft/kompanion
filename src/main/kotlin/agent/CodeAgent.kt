package agent

import agent.domain.*
import agent.fileops.KompanionFileHandler
import agent.interaction.AgentQuestion
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import agent.reason.Reasoner
import agent.traits.Analyst
import agent.traits.Coder
import agent.traits.Interactor
import org.slf4j.LoggerFactory

open class CodeAgent internal constructor(
    private val contextManager: ContextManager,
    private val reasoner: Reasoner,
    private val codeGenerator: CodeGenerator
) : Coder, Analyst, Interactor {

    private val logger = LoggerFactory.getLogger(this::class.java)

    lateinit var interactionHandler: InteractionHandler

    suspend fun onLoad() {
        if (!KompanionFileHandler.folderExists()) {
            val result =
                confirmWithUser(
                    """Hello! I'm Kompanion 👋, your coding assistant. 
                    |Would you like to initialize this repository?
                    |This is not required, but will make me smarter and more helpful! 🧠
                    |""".trimMargin()
                )

            if (result) {
                if (!KompanionFileHandler.folderExists()) {
                    KompanionFileHandler.createFolder()
                    interactionHandler.interact(
                        AgentResponse(
                            """Repository initialized ✅.
                        |I'm ready to help you with your coding tasks! 🚀
                    """.trimMargin()
                        )
                    )
                }
            }
        }
    }

    override suspend fun processCodingRequest(request: UserRequest): CodingAgentResponse {
        logger.info("Processing user request: ${request.instruction}")
        sendMessage("Analyzing your request...")
        // 1. Analyze request and current context
        val understanding = reasoner.analyzeRequest(request)
        sendMessage("I understand you want to: ${understanding.objective}")

        logger.debug("Understanding generated: {}", understanding)
        val plan = reasoner.createPlan(understanding)

        logger.debug("Generation plan created: {}", plan)
        var iterations = 0
        val maxIterations = 2

        var generationResult: GenerationResult? = null

        while (iterations < maxIterations) {
            logger.info("Iteration $iterations: Generating code")
            sendMessage("Generating code (iteration ${iterations + 1})...")
            generationResult = codeGenerator.generate(plan, generationResult?.formatted() ?: "")

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

                sendMessage("✅Proceeding with the changes!")

                codeGenerator.execute(plan, generationResult)

                logger.info("User confirmed changes. Returning successful response.")
                return CodingAgentResponse(
                    fileChanges = generationResult.fileChanges,
                    explanation = generationResult.explanation,
                    nextSteps = evaluation.suggestedImprovements,
                    confidence = evaluation.confidence
                )
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

    override suspend fun askQuestion(question: String): CodebaseQuestionResponse {
        val request = UserRequest(question)
        val understanding = reasoner.analyzeRequest(request)
        return reasoner.askQuestion(question, understanding)
    }


    fun registerHandler(interactionHandler: InteractionHandler) {
        this.interactionHandler = interactionHandler
    }

    fun fetchContextManager(): ContextManager {
        return contextManager
    }

    override suspend fun sendMessage(message: String) {
        interactionHandler.interact(AgentResponse(message))
    }

    override suspend fun askUser(question: String): String {
        return interactionHandler.interact(AgentQuestion(question))
    }

    override suspend fun confirmWithUser(message: String): Boolean {
        while (true) {
            val response = askUser("$message\nPlease respond with Y or N:")
            when (response.trim().uppercase()) {
                "Y", "y", "yes" -> return true
                "N", "n", "no" -> return false
                else -> sendMessage("Invalid response. Please answer with Y or N.")
            }
        }
    }
}

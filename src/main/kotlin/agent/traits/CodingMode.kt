package agent.traits

import agent.CodeGenerator
import agent.domain.FileChange
import agent.domain.GenerationResult
import agent.interaction.InteractionHandler
import agent.reason.Reasoner
import org.slf4j.LoggerFactory


class CodingMode(
    private val reasoner: Reasoner,
    private val codeGenerator: CodeGenerator,
    private val interactionHandler: InteractionHandler
) : Mode, Interactor {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun perform(request: String): String {
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
                    return "Changes were rejected by user."
                }

                sendMessage("✅Proceeding with the changes!")

                codeGenerator.execute(plan, generationResult)

                logger.info("User confirmed changes. Returning successful response.")
                return generationResult.explanation
            }

            iterations++

        }

        return "I'm afraid I wasn't able to come up with a decent solution. Can you please be more specific in what you wanted to build?"
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

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
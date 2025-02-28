package agent.modes

import agent.CodeGenerator
import agent.domain.FileChange
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

        val userConfirmed = confirmWithUser("Would you like me to apply these changes?")
        if (!userConfirmed) {
            logger.info("User rejected changes.")
            return "Changes were rejected by user."
        }

        sendMessage("✅Proceeding with the changes!")

        val result = codeGenerator.execute(plan)

        logger.info("User confirmed changes. Returning successful response.")
        return result.explanation
    }


    override suspend fun getLoadedActionNames(): List<String> {
        return emptyList()
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
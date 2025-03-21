package agent.coding

import agent.ContextManager
import agent.ToolManager
import agent.coding.domain.CodingResult
import agent.coding.domain.FlowAction
import agent.domain.GenerationPlan
import agent.domain.context.ContextFile
import agent.interaction.InteractionHandler
import agent.modes.Interactor
import ai.LLMProvider
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class FlowCodeGenerator(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
) : CodeGenerator, Interactor {

    val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun execute(
        request: String,
        plan: GenerationPlan,
    ): CodingResult {
        // Starting point - initial prompt to the LLM
        var result = promptForNextAction(request, plan, null)
        var action = result.first

        // Continue interaction until we get a Complete action
        while (action !is FlowAction.Complete) {
            when (action) {
                is FlowAction.EditFile -> {
                    logger.info("File edited: {}", action.filePath)
                    // Update the file
                    val file = Path.of(action.filePath).toFile()
                    Files.writeString(file.toPath(), action.content)

                    // Update the context
                    contextManager.updateFiles(
                        listOf(
                            ContextFile(
                                id = UUID.randomUUID(),
                                name = file.absolutePath,
                                content = action.content,
                                displayName = file.name
                            )
                        )
                    )

                    // Prompt for the next action with feedback about the edit
                    val feedback = "File ${file.name} has been updated successfully."
                    result = promptForNextAction(request, plan, feedback)
                    action = result.first
                }

                is FlowAction.CreateFile -> {
                    logger.info("New file created: {}", action.filePath)
                    // Create the file
                    val file = Path.of(action.filePath).toFile()
                    file.parentFile?.mkdirs() // Create parent directories if they don't exist
                    Files.writeString(file.toPath(), action.content)

                    // Update the context
                    contextManager.updateFiles(
                        listOf(
                            ContextFile(
                                id = UUID.randomUUID(),
                                name = file.absolutePath,
                                content = action.content,
                                displayName = file.name
                            )
                        )
                    )

                    // Prompt for the next action with feedback about the creation
                    val feedback = "File ${file.name} has been created successfully."
                    result = promptForNextAction(request, plan, feedback)
                    action = result.first
                }

                is FlowAction.Complete -> {
                    // We'll never reach this case in the while loop, but Kotlin requires it for exhaustiveness
                    break
                }
            }
        }

        // Return the final result with the completion summary
        return CodingResult(
            explanation = action.summary(),
            success = true
        )
    }

    private suspend fun promptForNextAction(
        request: String,
        plan: GenerationPlan,
        feedback: String?
    ): Pair<FlowAction, String> {
        val contextPrompt = contextManager.currentContextPrompt(true)
        val planSteps = plan.steps.joinToString("\n") { step ->
            "- Action: ${step.action}\n  Input: ${step.input}\n  Expected Output: ${step.expectedOutput}"
        }
        val validationCriteria = plan.validationCriteria.joinToString("\n") { "- $it" }
        val feedbackSection = if (!feedback.isNullOrBlank()) "\n\n## Feedback from previous action:\n$feedback" else ""

        logger.info("feedback or request: " + (feedback ?: request))

        val prompt = """
            $contextPrompt

            You're an amazing developer, with many years of experience and a deep understanding of clean code and architecture.
            Based on the following generation plan you will make the necessary code changes.
            Use files in your current context to understand your changes.

            If the user doesn't ask for it specifically, don't add tests.

            ## Project Context:
            Based on the files in your current context, you understand the existing code structure and patterns.
            Look for similar implementations in the current codebase to maintain consistency.

            ## Coding Task:
            Based on the following generation plan, implement the necessary code changes.
            First explore the codebase to understand the current structure before making changes.

            Plan Steps:
            $planSteps

            Expected Outcome:
            ${plan.expectedOutcome}

            Validation Criteria:
            $validationCriteria

            ## Implementation Approach:
            1. You will be implementing changes one action at a time
            2. For each action, you must return ONE of the following responses:
               - EDIT_FILE: To modify an existing file
               - CREATE_FILE: To create a new file
               - COMPLETE: When all changes are done
            3. You only change or create what planned and asked. No freewheeling.
            4. Definitely do not overengineer things. Keep it simple and clean in as few steps as possible.

            ## Response Format:
            You must respond with ONE of these formats:

            1. To edit a file:
            ```
            ACTION: EDIT_FILE
            FILE_PATH: /absolute/path/to/file
            EXPLANATION: Brief explanation of changes
            CONTENT:
            // Complete new content of the file
            ```
            
            2. To create a file:
            ```
            ACTION: CREATE_FILE
            FILE_PATH: /absolute/path/to/file
            EXPLANATION: Brief explanation of the file purpose
            CONTENT:
            // Complete content of the new file
            ```
            
            3. When complete:
            ```
            ACTION: COMPLETE
            SUMMARY: Detailed explanation of all changes made
            ```
            $feedbackSection
        """.trimIndent()

        val response = LLMProvider.prompt<String>(
            system = prompt,
            userMessage = feedback ?: request,
            actions = emptyList(),
            temperature = 0.5,
            parameterizedTypeReference = null
        )

        // Parse the response to extract the action
        return parseActionResponse(response)
    }

    private fun parseActionResponse(response: String): Pair<FlowAction, String> {
        val lines = response.split("\n")

        // Extract action type
        val actionLine = lines.firstOrNull { it.startsWith("ACTION:") }?.substringAfter("ACTION:") ?: ""
        val action = actionLine.trim()

        return when {
            action.equals("EDIT_FILE", ignoreCase = true) -> {
                val filePath =
                    lines.firstOrNull { it.startsWith("FILE_PATH:") }?.substringAfter("FILE_PATH:")?.trim() ?: ""
                val explanation =
                    lines.firstOrNull { it.startsWith("EXPLANATION:") }?.substringAfter("EXPLANATION:")?.trim() ?: ""

                // Extract content between CONTENT: and the next ``` or end of string
                val contentStart = response.indexOf("CONTENT:")
                val contentEnd = response.lastIndexOf("```")
                val content = if (contentStart != -1 && contentEnd != -1 && contentStart < contentEnd) {
                    response.substring(contentStart + "CONTENT:".length, contentEnd).trim()
                } else if (contentStart != -1) {
                    response.substring(contentStart + "CONTENT:".length).trim()
                } else {
                    ""
                }

                Pair(FlowAction.EditFile(filePath, content, explanation), response)
            }

            action.equals("CREATE_FILE", ignoreCase = true) -> {
                val filePath =
                    lines.firstOrNull { it.startsWith("FILE_PATH:") }?.substringAfter("FILE_PATH:")?.trim() ?: ""
                val explanation =
                    lines.firstOrNull { it.startsWith("EXPLANATION:") }?.substringAfter("EXPLANATION:")?.trim() ?: ""

                // Extract content between CONTENT: and the next ``` or end of string
                val contentStart = response.indexOf("CONTENT:")
                val contentEnd = response.lastIndexOf("```")
                val content = if (contentStart != -1 && contentEnd != -1 && contentStart < contentEnd) {
                    response.substring(contentStart + "CONTENT:".length, contentEnd).trim()
                } else if (contentStart != -1) {
                    response.substring(contentStart + "CONTENT:".length).trim()
                } else {
                    ""
                }

                Pair(FlowAction.CreateFile(filePath, content, explanation), response)
            }

            action.equals("COMPLETE", ignoreCase = true) -> {
                val summary = lines.firstOrNull { it.startsWith("SUMMARY:") }?.substringAfter("SUMMARY:")?.trim() ?: ""

                Pair(FlowAction.Complete(summary), response)
            }

            else -> {
                // If we can't parse the action, return a Complete action with an error message
                Pair(FlowAction.Complete("Failed to parse action from LLM response. Raw response: $response"), response)
            }
        }
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
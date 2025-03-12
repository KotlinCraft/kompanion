package agent.coding

import agent.ContextManager
import agent.ToolManager
import agent.coding.domain.CodingResult
import agent.coding.domain.FlowAction
import agent.domain.GenerationPlan
import agent.domain.context.ContextFile
import ai.LLMProvider
import org.springframework.core.ParameterizedTypeReference
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class FlowCodeGenerator(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager,
) : CodeGenerator {

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
                    // We'll never reach this case in the while loop, but Kotlin needs it for exhaustive when
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
        val prompt = buildString {
            append(contextManager.currentContextPrompt(true))
            appendLine()
            appendLine("You're an amazing developer, with many years of experience and a deep understanding of clean code and architecture.")
            appendLine("Based on the following generation plan you will make the necessary code changes.")
            appendLine("Use files in your current context to understand your changes.")
            appendLine()
            appendLine("If the user doesn't ask for it specifically, don't add tests.")
            appendLine()
            appendLine("## Project Context:")
            appendLine("Based on the files in your current context, you understand the existing code structure and patterns.")
            appendLine("Look for similar implementations in the current codebase to maintain consistency.")
            appendLine()
            appendLine("## Coding Task:")
            appendLine("Based on the following generation plan, implement the necessary code changes.")
            appendLine("First explore the codebase to understand the current structure before making changes.")
            appendLine()
            appendLine("Plan Steps:")
            plan.steps.forEach { step ->
                appendLine("- Action: ${step.action}")
                appendLine("  Input: ${step.input}")
                appendLine("  Expected Output: ${step.expectedOutput}")
            }
            appendLine()
            appendLine("Expected Outcome:")
            appendLine(plan.expectedOutcome)
            appendLine()
            appendLine("Validation Criteria:")
            plan.validationCriteria.forEach { appendLine("- $it") }
            appendLine()
            appendLine("## Implementation Approach:")
            appendLine("1. You will be implementing changes one action at a time")
            appendLine("2. For each action, you must return ONE of the following responses:")
            appendLine("   - EDIT_FILE: To modify an existing file")
            appendLine("   - CREATE_FILE: To create a new file")
            appendLine("   - COMPLETE: When all changes are done")
            appendLine()
            appendLine("## Response Format:")
            appendLine("You must respond with ONE of these formats:")
            appendLine()
            appendLine("1. To edit a file:")
            appendLine("```")
            appendLine("ACTION: EDIT_FILE")
            appendLine("FILE_PATH: /absolute/path/to/file")
            appendLine("EXPLANATION: Brief explanation of changes")
            appendLine("CONTENT:")
            appendLine("// Complete new content of the file")
            appendLine("```")
            appendLine()
            appendLine("2. To create a file:")
            appendLine("```")
            appendLine("ACTION: CREATE_FILE")
            appendLine("FILE_PATH: /absolute/path/to/file")
            appendLine("EXPLANATION: Brief explanation of the file purpose")
            appendLine("CONTENT:")
            appendLine("// Complete content of the new file")
            appendLine("```")
            appendLine()
            appendLine("3. When complete:")
            appendLine("```")
            appendLine("ACTION: COMPLETE")
            appendLine("SUMMARY: Detailed explanation of all changes made")
            appendLine("```")

            // Add the feedback from previous action if available
            if (!feedback.isNullOrBlank()) {
                appendLine()
                appendLine("## Feedback from previous action:")
                appendLine(feedback)
            }
        }

        val response = LLMProvider.prompt(
            system = prompt,
            userMessage = request,
            actions = emptyList(),
            temperature = 0.5,
            parameterizedTypeReference = object : ParameterizedTypeReference<String>() {},
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
}

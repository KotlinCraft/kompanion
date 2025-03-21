package agent.reason

import agent.ContextManager
import agent.ToolManager
import agent.interaction.InteractionHandler
import agent.interaction.ToolStatus
import agent.modes.Interactor
import agent.modes.fullauto.FullAutoBreakdown
import agent.modes.fullauto.Step
import ai.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.core.ParameterizedTypeReference
import ui.chat.StepExecutionIndicator
import java.util.UUID

class AutomodeExecutor(
    private val LLMProvider: LLMProvider,
    private val toolManager: ToolManager,
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler
) : Interactor {

    suspend fun executeStep(
        fullAutoBreakdown: FullAutoBreakdown,
        step: Step,
        acc: List<TaskInstructionResult>,
    ): TaskInstructionResult {

        val fullAutoBreakdownText = fullAutoBreakdown.steps.joinToString("\n") {
            """
                ${it.id}: ${it.instruction}
                ${it.subTasks.joinToString("\n")}
            """.trimIndent()
        }

        val task = """
            
                ${step.id}: ${step.instruction}
                ${step.subTasks.joinToString("\n")}
            """.trimIndent()

        // Show RUNNING indicator
        val toolId = runBlocking(Dispatchers.IO) {
            customToolUsage {
                StepExecutionIndicator(
                    step = step,
                    stepNumber = step.stepNumber,
                    status = ToolStatus.RUNNING
                )
            }
        }

        val previousTaskResults = if (acc.isNotEmpty()) {
            """
                Here are the task results of the previous invocations: 
                ${acc.joinToString("\n") { """${it.stepId}: ${it.taskCompletion}""" }}
            """.trimIndent()
        } else ""

        val prompt = """
            
            You are an AI assistant tasked with completing a specific step from a larger task breakdown. The full task breakdown has been provided to you, along with the specific step you need to complete. Your goal is to focus solely on completing the current task to the best of your ability.

            INFO ON YOUR ENVIRONMENT:
•           Your context already includes: ${contextManager.currentContextPrompt(true)}  

            Here is the full task breakdown:

            <task_breakdown>
            $fullAutoBreakdownText
            </task_breakdown>
            
            ${previousTaskResults}
            
            Please follow these instructions to complete the current task:

            1. Carefully read and understand the current task.
            2. If the task requires any input or information that is not provided, make it available by pulling it in your context using your tools.
            3. Execute the task as described, focusing only on the specific action required.
            4. If the task involves creating or modifying content, provide the exact content or changes that would result from completing the task.
            5. If the task involves a decision or analysis, provide a clear explanation of your reasoning and the outcome.
            6. If the task is part of a larger process (e.g., processing a file), remember that you are only responsible for this specific step. Do not attempt to complete subsequent steps.

            Please provide your detailed response to the current task in the taskCompletion section below. Your response should include the specific output or changes resulting from completing the task. Do not include any additional information or commentary outside of the task_completion tags.:

            Remember, your output should consist of only the task completion content within the specified tags. Do not repeat the task description or provide any additional commentary outside of the task_completion tags.

            Focus solely on completing the current task as specified. Do not attempt to complete other steps in the task breakdown or provide information beyond what is required for the current task.
        """.trimIndent()

        // Attempt to leverage the same LLM approach used in DefaultReasoner, if available
        try {
            val result = LLMProvider.prompt(
                system = prompt,
                userMessage = """
                    The current task you need to complete is:
                <current_task>
                $task
                </current_task>
                """.trimIndent(),
                actions = toolManager.tools.map { it.toolCallback },
                temperature = 0.7,
                parameterizedTypeReference = object : ParameterizedTypeReference<TaskInstructionResponse>() {},
            )

            // Show COMPLETED indicator
            customToolUsage(id = toolId) {
                StepExecutionIndicator(
                    step = step,
                    stepNumber = step.stepNumber,
                    status = ToolStatus.COMPLETED,
                    result = result.taskCompletion
                )
            }

            return TaskInstructionResult(
                stepId = step.id,
                taskCompletion = result.taskCompletion
            )

        } catch (e: Exception) {
            // Show FAILED indicator
            runBlocking(Dispatchers.IO) {
                customToolUsage(id = toolId) {
                    StepExecutionIndicator(
                        step = step,
                        stepNumber = step.stepNumber,
                        status = ToolStatus.FAILED,
                        error = "Error executing step: ${e.message}"
                    )
                }
            }

            return TaskInstructionResult(
                stepId = step.id,
                "Error: ${e.message}"
            )
        }
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }

    data class TaskInstructionResult(
        val stepId: UUID,
        val taskCompletion: String
    )

    data class TaskInstructionResponse(
        val taskCompletion: String
    )
}
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

class AutoModeReasoner(
    private val LLMProvider: LLMProvider,
    private val toolManager: ToolManager,
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler
) : Interactor {

    suspend fun generatePlan(
        question: String
    ): FullAutoBreakdownResponse {
        val prompt = """
INFO ON YOUR ENVIRONMENT:
• Your context already includes: ${contextManager.currentContextPrompt(true)}  

You are an AI assistant tasked with breaking down complex questions or instructions into simple, actionable steps. 
Your goal is to create a step-by-step plan that can be easily followed to complete the given task.
Every task will be given to an AI assistant, and you need to provide a detailed breakdown of the task.

Please follow these instructions to break down the question or instruction:

1. Carefully read and analyze the given question or instruction.
2. Identify the main objective and any sub-tasks required to achieve it.
3. Break down the task into individual, isolated actions. Each step should represent a single, clear action.
4. If a task requires repetition, create separate steps for each iteration. For example, if the task is to summarize every file in a folder, create a separate step for each file.
5. Consider which of the available tools might be useful for each step, and incorporate them where appropriate.
6. Ensure that the steps are in a logical order and that each step builds upon the previous ones.
7. Use clear and concise language for each step, avoiding ambiguity.
8. Use the tools available to you to ask for clarification or additional information if needed.
9. Use the tools available to analyze the details of the task and provide a comprehensive breakdown.

When handling repetitive tasks:
- Identify the repeating element (e.g., files, items, sections)
- Create a template step that can be applied to each instance

Question: Create a landing page for every text file in inputs/

Broken down steps:
1. List all text files in the inputs/ directory.
2. Process File 1: 
   2.1. Read the content of the text file.
   2.2. Create a new HTML file for the landing page.
   2.3. Generate a basic HTML structure for the landing page.
   2.4. Insert the content from the text file into the HTML structure.
   2.5. Save the HTML file with a name corresponding to the original text file.
3. Process File 2:
   3.1. Read the content of the text file.
   3.2. Create a new HTML file for the landing page.
   3.3. Generate a basic HTML structure for the landing page.
   3.4. Insert the content from the text file into the HTML structure.
   3.5. Save the HTML file with a name corresponding to the original text file.
4. Verify that a landing page has been created for each text file.

Provide your step-by-step breakdown. Each step contains an instruction. If necessary, each instruction can have sub instructions. 
Your final output should consist of only the breakdown, without any additional explanation or commentary.
        """.trimIndent()

        // Attempt to leverage the same LLM approach used in DefaultReasoner, if available
        return LLMProvider.prompt(
            system = prompt,
            userMessage = question,
            actions = toolManager.tools.map { it.toolCallback },
            temperature = 0.7,
            parameterizedTypeReference = object : ParameterizedTypeReference<FullAutoBreakdownResponse>() {},
        )
    }

    suspend fun executeStep(
        fullAutoBreakdown: FullAutoBreakdown,
        step: Step,
    ): TaskInstructionResult {

        val fullAutoBreakdownText = fullAutoBreakdown.steps.joinToString("\n") {
            """
                ${it.instruction}
                ${it.subTasks.joinToString("\n")}
            """.trimIndent()
        }

        val task = """
                ${step.instruction}
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

        val prompt = """
            
            You are an AI assistant tasked with completing a specific step from a larger task breakdown. The full task breakdown has been provided to you, along with the specific step you need to complete. Your goal is to focus solely on completing the current task to the best of your ability.

            INFO ON YOUR ENVIRONMENT:
•           Your context already includes: ${contextManager.currentContextPrompt(true)}  

            Here is the full task breakdown:

            <task_breakdown>
            $fullAutoBreakdownText
            </task_breakdown>
            
•           Your context already includes: ${contextManager.currentContextPrompt(false)}  

            Please follow these instructions to complete the current task:

            1. Carefully read and understand the current task.
            2. If the task requires any input or information that is not provided, assume it is available to you or has been completed in previous steps.
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
                parameterizedTypeReference = object : ParameterizedTypeReference<TaskInstructionResult>() {},
            )

            // Show COMPLETED indicator
            runBlocking(Dispatchers.IO) {
                customToolUsage(id = toolId) {
                    StepExecutionIndicator(
                        step = step,
                        stepNumber = step.stepNumber,
                        status = ToolStatus.COMPLETED,
                        result = result.taskCompletion
                    )
                }
            }

            return result
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

            return TaskInstructionResult("Error: ${e.message}")
        }
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }

    data class TaskInstructionResult(
        val taskCompletion: String
    )

    data class FullAutoBreakdownResponse(
        val steps: List<StepResponse>
    )

    data class StepResponse(
        val instruction: String,
        val subTasks: List<String>
    )
}
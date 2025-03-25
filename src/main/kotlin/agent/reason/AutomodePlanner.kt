package agent.reason

import agent.ContextManager
import agent.ToolManager
import agent.interaction.InteractionHandler
import agent.interaction.ToolStatus
import agent.modes.Interactor
import agent.modes.fullauto.FullAutoBreakdown
import agent.modes.fullauto.Step
import agent.modes.fullauto.StepType
import ai.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.core.ParameterizedTypeReference
import ui.chat.StepExecutionIndicator

class AutomodePlanner(
    private val LLMProvider: LLMProvider,
    private val toolManager: ToolManager,
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler
) : Interactor {

    suspend fun generatePlan(
        question: String
    ): FullAutoBreakdownResponse {
        val prompt = """
You are an advanced AI assistant specializing in task breakdown and planning. Your primary function is to analyze complex questions or instructions and create detailed, step-by-step plans that can be easily followed to complete the given task.

Before providing your final step-by-step breakdown, wrap your analysis inside <task_analysis> tags in your thinking block. In your analysis phase:

1. Break down the main task into smaller sub-tasks.
2. Identify the main objective and any sub-tasks required to achieve it.
3. Look for any relevant files (such as llm.md or knowledge.md) that might provide context for the project. If found, incorporate this information into your planning.
4. List out relevant tools and explain their potential use for each step of the task.
5. Identify potential challenges or complexities in the task.
6. If the task involves repetitive actions, create a template that can be applied to multiple instances.
7. Ensure that your plan adheres to the following guidelines:
   - Break down the task into individual, isolated actions.
   - Each step should represent a single, clear action.
   - Steps are either General Actions (simple instructions that don't require coding) or Coding Actions (instructions that require coding).
   - Include whether to edit files or not in every step.
   - File operations (reading, writing) are considered General Actions unless they require actual code to be written.
   - Edits will be made without having to save, so don't plan 'saving' steps.

After your analysis phase, provide your final step-by-step breakdown. Each step should contain an instruction, and if necessary, each instruction can have sub-instructions. Number your steps and use clear, concise language for each one, avoiding ambiguity.

Your final output should consist of only the breakdown, without any additional explanation or commentary. The breakdown should be in the following format:

1. [First step]
   1.1. [Sub-step if necessary]
   1.2. [Sub-step if necessary]
2. [Second step]
3. [Third step]
   3.1. [Sub-step if necessary]
   ...

Remember, do not write or save any files; you are only to respond with the plan. Your final output should consist only of the step-by-step breakdown and should not duplicate or rehash any of the work you did in the task analysis block. If everything is clear, proceed with your analysis and then the step-by-step breakdown.
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

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }

    data class FullAutoBreakdownResponse(
        val steps: List<StepResponse>,
    )

    data class StepResponse(
        val instruction: String,
        val stepType: StepType,
        val subTasks: List<String>
    )
}
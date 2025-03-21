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
9. You already have the full outline of all files we're working with. There are no additional local files.
10. Don't ask for questions if everything is clear
11. Every Step is either a General Action or a Coding Action. General Actions are simple instructions that don't require coding. Coding Actions are instructions that require coding. Reading and writing files are general actions, unless they require actual code to be written.
12. Don't write any files, you are only to respond.

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
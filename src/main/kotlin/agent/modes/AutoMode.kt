package agent.modes

import agent.ContextManager
import agent.ToolManager
import agent.coding.FlowCodeGenerator
import agent.coding.tool.LocalCodingTools
import agent.interaction.InteractionHandler
import agent.interaction.ToolStatus
import agent.modes.fullauto.FullAutoBreakdown
import agent.modes.fullauto.Step
import agent.modes.fullauto.StepType
import agent.reason.AutomodeExecutor
import agent.reason.AutomodePlanner
import agent.tool.FileTools
import agent.tool.GeneralTools
import agent.tool.LoadedTool
import org.slf4j.LoggerFactory
import ui.chat.StepExecutionIndicator
import java.util.*

val logger = LoggerFactory.getLogger(AutoMode::class.java)

class AutoMode(
    private val planner: AutomodePlanner,
    private val executor: AutomodeExecutor,
    private val flowCodeGenerator: FlowCodeGenerator,
    private val toolManager: ToolManager,
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
) : Mode, Interactor {


    init {
        GeneralTools(interactionHandler).register(toolManager)
        FileTools(contextManager).register(toolManager)
        LocalCodingTools(interactionHandler, contextManager).register(toolManager)
    }

    override suspend fun perform(request: String): String {

        val planningId = planningIndicator()

        val breakdown = FullAutoBreakdown(
            steps = planner.generatePlan(request).steps.mapIndexed { index, it ->
                Step(
                    UUID.randomUUID(),
                    stepNumber = index + 1,
                    it.instruction,
                    it.subTasks,
                    type = it.stepType
                )
            }
        ).also {
            completePlanning(planningId)
        }


        val result = breakdown.steps.fold(emptyList<AutomodeExecutor.TaskInstructionResult>()) { acc, step ->
            val stepResult = executor.executeStep(
                breakdown, step, acc
            )
            contextManager.clearContext()
            acc + stepResult
        }
        return """
${result.joinToString("\n") { "👉 ${it.taskCompletion}" }}
        """.trimIndent()
    }

    private suspend fun AutoMode.completePlanning(planningId: UUID) {
        customToolUsage(id = planningId) {
            StepExecutionIndicator(
                Step(
                    stepNumber = 0,
                    instruction = "Planning out the task, breaking down the task into steps.",
                    subTasks = emptyList(),
                    type = StepType.GENERAL_ACTION
                ),
                stepNumber = 0,
                status = ToolStatus.COMPLETED
            )
        }
    }

    private suspend fun AutoMode.planningIndicator() = customToolUsage {
        StepExecutionIndicator(
            Step(
                UUID.randomUUID(),
                stepNumber = 0,
                instruction = "Planning out the task, breaking down the task into steps.",
                subTasks = emptyList(),
                type = StepType.GENERAL_ACTION
            ),
            stepNumber = 0,
            status = ToolStatus.RUNNING
        )
    }


    override suspend fun getLoadedTools(): List<LoadedTool> {
        return toolManager.tools.filter {
            it.showUpInTools
        }.map {
            LoadedTool(
                id = it.id,
                name = it.toolCallback.toolDefinition.name(),
                tool = it
            )
        }
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}
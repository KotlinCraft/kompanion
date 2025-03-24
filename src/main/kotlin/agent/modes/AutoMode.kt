package agent.modes

import agent.ContextManager
import agent.ToolManager
import agent.coding.FlowCodeGenerator
import agent.coding.tool.LocalCodingTools
import agent.domain.GenerationPlan
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
import agent.tool.Tool
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.nel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mcp.McpManager
import mcp.McpManager.Companion.mcpManager
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import ui.chat.StepExecutionIndicator
import java.util.*

val logger = LoggerFactory.getLogger(AutoMode::class.java)

class AutoMode(
    private val planner: AutomodePlanner,
    private val executor: AutomodeExecutor,
    private val codingMode: CodingMode,
    private val toolManager: ToolManager,
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
) : Mode, Interactor {


    init {
        GeneralTools(interactionHandler).register(toolManager)
        FileTools(contextManager).register(toolManager)
        LocalCodingTools(interactionHandler, contextManager).register(toolManager)
        val toolbacks = mcpManager.loadMcpServers().flatMap {
            Either.catch {
                SyncMcpToolCallbackProvider.syncToolCallbacks(it.nel())
            }.mapLeft {
                logger.error("unable to initialize mcp server: {}", it.message)
            }.getOrElse { emptyList() }
        }.distinctBy { it.toolDefinition.name() }

        toolbacks.forEach {
            toolManager.registerTool(
                Tool(
                    id = UUID.randomUUID().toString(),
                    toolCallback = it,
                    showUpInTools = true
                )
            )
        }
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
            val toolId = customToolUsage {
                StepExecutionIndicator(
                    step = step,
                    stepNumber = step.stepNumber,
                    status = ToolStatus.RUNNING
                )
            }
            when (step.type) {
                StepType.GENERAL_ACTION -> {
                    val stepResult = executor.executeStep(
                        breakdown, step, acc
                    )
                    acc + stepResult
                }

                StepType.CODING_ACTION -> {
                    val result = codingMode.perform(request = step.instructionAsString())
                    acc + AutomodeExecutor.TaskInstructionResult(step.id, result)
                }
            }.also {
                customToolUsage(id = toolId) {
                    StepExecutionIndicator(
                        step = step,
                        stepNumber = step.stepNumber,
                        status = ToolStatus.COMPLETED,
                        result = it.last().taskCompletion
                    )
                }
            }
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
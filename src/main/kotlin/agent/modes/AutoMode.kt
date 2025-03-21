package agent.modes

import agent.ContextManager
import agent.ToolManager
import agent.coding.tool.LocalCodingTools
import agent.interaction.InteractionHandler
import agent.modes.fullauto.FullAutoBreakdown
import agent.modes.fullauto.Step
import agent.reason.AutoModeReasoner
import agent.tool.FileTools
import agent.tool.GeneralTools
import agent.tool.LoadedTool
import org.slf4j.LoggerFactory
import java.util.*

val logger = LoggerFactory.getLogger(AutoMode::class.java)

class AutoMode(
    private val reasoner: AutoModeReasoner,
    private val toolManager: ToolManager,
    contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
) : Mode, Interactor {


    init {
        GeneralTools(interactionHandler).register(toolManager)
        FileTools(contextManager).register(toolManager)
        LocalCodingTools(interactionHandler, contextManager).register(toolManager)
    }

    override suspend fun perform(request: String): String {
        val breakdown = FullAutoBreakdown(
            id = UUID.randomUUID(),
            steps = reasoner.generatePlan(request).steps.mapIndexed { index, it ->
                Step(
                    UUID.randomUUID(),
                    stepNumber = index,
                    it.instruction,
                    it.subTasks
                )
            }
        )

        breakdown.steps.map {
            val result = reasoner.executeStep(
                breakdown, it
            )
        }
        return "Task broken down into ${breakdown.steps.size} steps."
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
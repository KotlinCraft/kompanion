package agent.modes

import agent.ContextManager
import agent.ToolManager
import agent.coding.CodeGenerator
import agent.coding.tool.LocalCodingTools
import agent.domain.GenerationPlan
import agent.fileops.KompanionFileHandler
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import agent.reason.CodingAnalyst
import agent.reason.CodingPlanner
import agent.tool.FileTools
import agent.tool.LoadedTool
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class CodingMode(
    private val codingAnalyst: CodingAnalyst,
    private val codingPlanner: CodingPlanner,
    private val codeGenerator: CodeGenerator,
    private val interactionHandler: InteractionHandler,
    private val toolManager: ToolManager,
    contextManager: ContextManager,
    private val analyze: Boolean = true,
) : Mode, Interactor {


    init {
        GlobalScope.launch {
            if (!KompanionFileHandler.kompanionFolderExists()) {
                val result =
                    confirmWithUser(
                        """Hello! I'm Kompanion 👋, your coding assistant. 
Would you like to initialize this repository?
This is not required, but will make me smarter and more helpful! 🧠
""".trimMargin()
                    )

                if (result) {
                    if (!KompanionFileHandler.kompanionFolderExists()) {
                        KompanionFileHandler.createFolder()
                        interactionHandler.interact(
                            AgentResponse(
                                """Repository initialized ✅.
I'm ready to help you with your coding tasks! 🚀
                    """.trimMargin()
                            )
                        )
                    }
                }
            }
        }
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {

        try {
            LocalCodingTools(interactionHandler, contextManager).register(toolManager)
            FileTools(contextManager).register(toolManager)
        } catch (ex: Exception) {
            logger.error("Failed to connect to MCP server, no intellij support")
        }
    }

    override suspend fun perform(request: String): String {

        val plan = if (analyze) {
            // Step 1: Analyze the request to understand it
            val understanding = codingAnalyst.analyzeRequest(request)

            // Step 2: Create a generation plan (now enhanced with reasoning)
            val plan = codingPlanner.createPlan(request, understanding)
            sendGenerationPlanToUser(plan)
            plan
        } else null


        // Step 3: Execute the plan with the code generator
        val result = codeGenerator.execute(request, plan)

        return result.explanation
    }

    private suspend fun sendGenerationPlanToUser(plan: GenerationPlan) {
        sendMessage(
            """
Here's the detailed plan: 
Steps: 
${plan.steps.joinToString("\n") { "👉 ${it.action}" }}
            
Expected Outcome: 
${plan.expectedOutcome}
""".trimMargin().trimIndent()
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

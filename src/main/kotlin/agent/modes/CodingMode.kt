package agent.modes

import agent.ContextManager
import agent.ToolManager
import agent.coding.CodeGenerator
import agent.coding.tool.LocalCodingTools
import agent.domain.GenerationPlan
import agent.fileops.KompanionFileHandler
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import agent.reason.Reasoner
import agent.tool.FileTools
import agent.tool.LoadedTool
import agent.tool.Tool
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.nel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mcp.McpManager
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import java.util.*

class CodingMode(
    private val reasoner: Reasoner,
    private val codeGenerator: CodeGenerator,
    private val interactionHandler: InteractionHandler,
    private val toolManager: ToolManager,
    mcpManager: McpManager,
    contextManager: ContextManager,
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
            val toolbacks = mcpManager.getMcpServers().flatMap {
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
            LocalCodingTools(interactionHandler, contextManager).register(toolManager)
            FileTools(contextManager).register(toolManager)
        } catch (ex: Exception) {
            logger.error("Failed to connect to MCP server, no intellij support")
        }
    }

    override suspend fun perform(request: String): String {
        val understanding = reasoner.analyzeRequest(request)
        sendMessage("I understand you want to: ${understanding.objective}")

        logger.debug("Understanding generated: {}", understanding)
        val plan = reasoner.createPlan(request, understanding)
        sendGenerationPlanToUser(plan)

        logger.debug("Generation plan created: {}", plan)

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

package agent.modes

import agent.ContextManager
import agent.ToolManager
import agent.coding.CodeGenerator
import agent.coding.tool.LocalFileCodingTools
import agent.domain.GenerationPlan
import agent.fileops.KompanionFileHandler
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import agent.reason.Reasoner
import agent.tool.FileTools
import ai.Action
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class CodingMode(
    private val reasoner: Reasoner,
    private val codeGenerator: CodeGenerator,
    private val interactionHandler: InteractionHandler,
    private val toolManager: ToolManager,
    contextManager: ContextManager,
) : Mode, Interactor {


    init {
        GlobalScope.launch {
            if (!KompanionFileHandler.kompanionFolderExists()) {
                val result =
                    confirmWithUser(
                        """Hello! I'm Kompanion 👋, your coding assistant. 
                    |Would you like to initialize this repository?
                    |This is not required, but will make me smarter and more helpful! 🧠
                    |""".trimMargin()
                    )

                if (result) {
                    if (!KompanionFileHandler.kompanionFolderExists()) {
                        KompanionFileHandler.createFolder()
                        interactionHandler.interact(
                            AgentResponse(
                                """Repository initialized ✅.
                        |I'm ready to help you with your coding tasks! 🚀
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
            /*  var params: ServerParameters = ServerParameters.builder("npx")
                  .args("-y", "@jetbrains/mcp-proxy")
                  .build()
              var transport = StdioClientTransport(params)

              var client: McpSyncClient = McpClient.sync(transport)
                  .requestTimeout(Duration.ofSeconds(5))
                  .capabilities(
                      McpSchema.ClientCapabilities.builder()
                          .build()
                  ).build()

              val callbacks = McpToolUtils.getToolCallbacksFromSyncClients(client)
              toolManager.registerCallbacks(callbacks) */
            throw UnsupportedOperationException("MCP server is not supported yet")
        } catch (ex: Exception) {
            logger.error("Failed to connect to MCP server, no intellij support")
            LocalFileCodingTools(interactionHandler, contextManager).register(toolManager)
            FileTools(contextManager).register(toolManager)
        }
    }

    override suspend fun perform(request: String): String {
        val understanding = reasoner.analyzeRequest(request)
        sendMessage("I understand you want to: ${understanding.objective}")
        sendGenerationPlanToUser(reasoner.createPlan(understanding))

        logger.debug("Understanding generated: {}", understanding)
        val plan = reasoner.createPlan(understanding)

        logger.debug("Generation plan created: {}", plan)

        val result = codeGenerator.execute(plan)

        return result.explanation
    }

    private suspend fun sendGenerationPlanToUser(plan: GenerationPlan) {
        sendMessage(
            """Here's the detailed plan: 
            Steps: ${plan.steps.joinToString { "👉$it)\n" }}
            
            Expected Outcome: ${plan.expectedOutcome}
            """
        )
    }

    override suspend fun getLoadedActionNames(): List<String> {
        return toolManager.tools.map { it.action }.filter(Action::showUpInTools).map {
            it.name
        } + toolManager.toolCallbacks.map {
            it.toolDefinition.name()
        }
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}

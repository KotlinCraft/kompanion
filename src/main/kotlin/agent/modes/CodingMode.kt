package agent.modes

import agent.ContextManager
import agent.ToolManager
import agent.coding.CodeGenerator
import agent.coding.tool.LocalFileCodingTools
import agent.interaction.InteractionHandler
import agent.reason.Reasoner
import agent.tool.FileTools
import ai.Action
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.spec.McpSchema
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.McpToolUtils
import java.time.Duration


class CodingMode(
    private val reasoner: Reasoner,
    private val codeGenerator: CodeGenerator,
    private val interactionHandler: InteractionHandler,
    private val toolManager: ToolManager,
    contextManager: ContextManager,
) : Mode, Interactor {


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

        logger.debug("Understanding generated: {}", understanding)
        val plan = reasoner.createPlan(understanding)

        logger.debug("Generation plan created: {}", plan)

        //val userConfirmed = confirmWithUser("Would you like me to apply these changes?")
        if (false) {
            logger.info("User rejected changes.")
            return "Changes were rejected by user."
        }

        sendMessage("✅Proceeding with the changes!")

        val result = codeGenerator.execute(plan)

        logger.info("User confirmed changes. Returning successful response.")
        return result.explanation
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
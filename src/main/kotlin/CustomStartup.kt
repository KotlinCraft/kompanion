import agent.*
import agent.domain.FileSystemCodeApplier
import agent.domain.UserRequest
import agent.interaction.AgentMessage
import agent.interaction.AgentQuestion
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import ai.OpenAIModel
import config.AppConfig
import kotlinx.coroutines.runBlocking

fun main() {

    val config = AppConfig.load()
    val defaultModel = OpenAIModel(config)

    val workingDirectory = "/opt/projects/decentrifi/humanless"
    runBlocking {

        val contextManager = InMemoryContextManager(
            workingDirectory = workingDirectory
        )

        val filelist = contextManager.getFullFileList()

        val interactionHandler = StubInteractionHandler()

        val agent = CodingAgent(
            DefaultReasoner(defaultModel, contextManager),
            DefaultCodeGenerator(defaultModel, contextManager),
            FileSystemCodeApplier(contextManager)
        ).also {
            it.registerHandler(interactionHandler)
        }

        val response = agent.process(
            UserRequest(
                instruction = "Let's add excerpt to the GeneratedNews class",
            )
        )
    }
}


class StubInteractionHandler() : InteractionHandler {
    override suspend fun interact(agentMessage: AgentMessage): String {
        when (agentMessage) {
            is AgentQuestion -> {
                println("Question from agent: ${agentMessage.message}")
                print("> ") // Add prompt for user input
                return readlnOrNull() ?: "no response" // Read from terminal
            }

            is AgentResponse -> {
                println("Message from agent: ${agentMessage.message}")
                return "" // No response needed for messages
            }
        }
    }
}

import agent.*
import agent.domain.UserRequest
import agent.interaction.AgentMessage
import agent.interaction.InteractionHandler
import ai.OpenAIModel
import config.AppConfig
import kotlinx.coroutines.runBlocking

fun main() {

    val config = AppConfig.load()
    val defaultModel = OpenAIModel(config)

    val workingDirectory = "/opt/projects/kotlincraft/kompanion"
    runBlocking {

        val contextManager = InMemoryContextManager(
            workingDirectory = workingDirectory
        )

        val interactionHandler = StubInteractionHandler()

        val agent = CodingAgent(
            DefaultReasoner(defaultModel, contextManager),
            DefaultCodeGenerator(defaultModel, contextManager),
        ).also {
            it.registerHandler(interactionHandler)
        }

        val response = agent.process(
            UserRequest(
                instruction = "add a function that takes two numbers and returns their sum"
            )
        )
    }
}


class StubInteractionHandler() : InteractionHandler {
    override suspend fun interact(agentMessage: AgentMessage): String {
        println("Interacting with agent: $agentMessage")
        return "Stubbed response"
    }
}
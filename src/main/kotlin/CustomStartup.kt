import agent.*
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
        when (agentMessage) {
            is AgentQuestion -> {
                println("Question from agent: ${agentMessage.message}")
                return "Stubbed response"
            }

            is AgentResponse -> {
                println("Message from agent: ${agentMessage.message}")
                return "Stubbed response"
            }
        }
    }
}
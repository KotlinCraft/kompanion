import agent.domain.UserRequest
import agent.interaction.AgentMessage
import agent.interaction.AgentQuestion
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import kotlinx.coroutines.runBlocking

fun main() {

    val workingDirectory = "/opt/projects/decentrifi/humanless"
    runBlocking {
        val interactionHandler = StubInteractionHandler()

        val kompanion = Kompanion.builder().build()

        val agent = kompanion.agent.also {
            it.registerHandler(interactionHandler)
        }

        val response = agent.processCodingRequest(
            UserRequest(
                instruction = "add comments to the customstartup"
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

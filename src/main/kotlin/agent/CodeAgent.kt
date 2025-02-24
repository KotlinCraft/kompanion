package agent

import agent.interaction.InteractionHandler
import agent.traits.Analyst
import agent.traits.Coder
import agent.traits.Interactor

interface CodeAgent : Coder, Analyst, Interactor {
    fun registerHandler(interactionHandler: InteractionHandler)
    fun fetchContextManager(): ContextManager

    override suspend fun confirmWithUser(message: String): Boolean {
        while (true) {
            val response = askUser("$message\nPlease respond with Y or N:")
            when (response.trim().uppercase()) {
                "Y", "y", "yes" -> return true
                "N", "n", "no" -> return false
                else -> sendMessage("Invalid response. Please answer with Y or N.")
            }
        }
    }
}
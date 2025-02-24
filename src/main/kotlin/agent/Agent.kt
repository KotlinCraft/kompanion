package agent

import agent.fileops.KompanionFileHandler
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import agent.traits.Interactor
import agent.traits.Mode
import org.slf4j.LoggerFactory

class Agent internal constructor(
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
    private val mode: Mode
) : Interactor {

    fun fetchContextManager(): ContextManager {
        return contextManager
    }

    suspend fun perform(request: String): String {
        return mode.perform(request)
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun onLoad() {
        if (!KompanionFileHandler.folderExists()) {
            val result =
                confirmWithUser(
                    """Hello! I'm Kompanion 👋, your coding assistant. 
                    |Would you like to initialize this repository?
                    |This is not required, but will make me smarter and more helpful! 🧠
                    |""".trimMargin()
                )

            if (result) {
                if (!KompanionFileHandler.folderExists()) {
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

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}

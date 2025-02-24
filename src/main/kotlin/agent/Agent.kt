package agent

import agent.fileops.KompanionFile
import agent.fileops.KompanionFileHandler
import agent.interaction.AgentResponse
import agent.interaction.InteractionHandler
import agent.modes.Interactor
import agent.modes.Mode

class Agent internal constructor(
    private val contextManager: ContextManager,
    private val interactionHandler: InteractionHandler,
    private val mode: Mode
) : Interactor {

    fun fetchContextManager(): ContextManager {
        return contextManager
    }

    suspend fun perform(request: String): String {
        return mode.perform(request).also { response ->
            if (KompanionFileHandler.kompanionFolderExists()) {
                KompanionFileHandler.append(KompanionFile.MESSAGE_HISTORY.fileName, "User: $request")
                KompanionFileHandler.append(KompanionFile.MESSAGE_HISTORY.fileName, "Kompanion: $response")
            }
        }
    }

    suspend fun onload() {
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

        mode.onload()
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}

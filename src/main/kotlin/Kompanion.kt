import agent.*
import agent.coding.DefaultCodeGenerator
import agent.domain.CodeApplier
import agent.fileops.KompanionFile
import agent.fileops.KompanionFileHandler
import agent.fileops.KompanionFileHandler.Companion.kompanionFolderExists
import agent.interaction.AgentMessage
import agent.interaction.InteractionHandler
import agent.reason.DefaultReasoner
import agent.reason.Reasoner
import agent.modes.AnalystMode
import agent.modes.CodingMode
import ai.LLMProvider
import ai.LLMRegistry
import arrow.core.Either
import arrow.core.getOrElse
import config.AppConfig

class Kompanion(
    val agent: Agent
) {
    companion object {
        fun builder(): KompanionBuilder {
            LLMProvider.load()

            return KompanionBuilder()
        }

        fun default(interactionHandler: InteractionHandler): Kompanion {
            return builder()
                .withInteractionHandler(interactionHandler)
                .build()
        }
    }
}

class KompanionBuilder {

    enum class AgentMode {
        ASK, CODE
    }

    private var reasoner: Reasoner? = null
    private var codeGenerator: CodeGenerator? = null
    private var codeApplier: CodeApplier? = null
    private var contextManager: ContextManager? = null
    private var smallLlmProvider: LLMProvider? = null
    private var bigLlmProvider: LLMProvider? = null
    private var interactionHandler: InteractionHandler? = null
    private var mode: AgentMode = AgentMode.ASK


    fun withMode(mode: AgentMode) = apply {
        this.mode = mode
    }

    fun withContextManager(customContextManager: ContextManager) = apply {
        contextManager = customContextManager
    }

    fun withSmallLLMProvider(provider: LLMProvider) = apply {
        smallLlmProvider = provider
    }

    fun withBigLLMProvider(provider: LLMProvider) = apply {
        bigLlmProvider = provider
    }

    fun withCustomReasoner(customReasoner: Reasoner) = apply {
        reasoner = customReasoner
    }

    fun withCustomCodeGenerator(generator: CodeGenerator) = apply {
        codeGenerator = generator
    }

    fun withCustomCodeApplier(applier: CodeApplier) = apply {
        codeApplier = applier
    }

    fun build(): Kompanion {
        val finalContextManager = contextManager ?: InMemoryContextManager()
        val smallProvider = Either.catch {
            getFinalLLMProvider(AppConfig.load().model.small)
        }.getOrElse { getFinalLLMProvider("gpt-4o-mini") }

        val bigProvider = Either.catch {
            getFinalLLMProvider(AppConfig.load().model.big)
        }.getOrElse { getFinalLLMProvider("gpt-4o") }

        val finalReasoner = reasoner ?: DefaultReasoner(smallProvider, finalContextManager)
        val finalGenerator = codeGenerator ?: DefaultCodeGenerator(bigProvider, finalContextManager)


        val mode = when (mode) {
            AgentMode.ASK -> AnalystMode(finalReasoner)
            AgentMode.CODE -> CodingMode(finalReasoner, finalGenerator, interactionHandler!!)
        }

        val interactionSavingWrapper = object : InteractionHandler {
            override suspend fun interact(agentMessage: AgentMessage): String {
                if (agentMessage.important && kompanionFolderExists()) {
                    KompanionFileHandler.append(
                        KompanionFile.MESSAGE_HISTORY.fileName,
                        "Kompanion: ${agentMessage.message}"
                    )
                }
                return interactionHandler!!.interact(agentMessage)
            }
        }

        val agent = Agent(
            finalContextManager,
            interactionSavingWrapper,
            mode
        )
        return Kompanion(agent)
    }

    private fun getFinalLLMProvider(name: String): LLMProvider {
        return LLMRegistry.getProviderForModel(name)
            ?: throw IllegalArgumentException("Unable to find provider for model $name")
    }

    fun withInteractionHandler(interactionHandler: InteractionHandler) = apply {
        this.interactionHandler = interactionHandler
    }
}

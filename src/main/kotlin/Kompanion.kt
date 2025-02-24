import agent.*
import agent.domain.CodeApplier
import agent.interaction.InteractionHandler
import ai.LLMProvider
import ai.LLMRegistry
import config.AppConfig

class Kompanion constructor(
    val agent: CodingAgent
) {
    companion object {
        fun builder(): KompanionBuilder {
            LLMProvider.load()

            return KompanionBuilder()
        }

        fun default(): Kompanion {
            return builder().build()
        }
    }
}

class KompanionBuilder {
    private var reasoner: Reasoner? = null
    private var codeGenerator: CodeGenerator? = null
    private var codeApplier: CodeApplier? = null
    private var contextManager: ContextManager? = null
    private var smallLlmProvider: LLMProvider? = null
    private var bigLlmProvider: LLMProvider? = null
    private var interactionHandler: InteractionHandler? = null

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
        val smallProvider = getFinalLLMProvider(AppConfig.load().model.small)
        val bigProvider = getFinalLLMProvider(AppConfig.load().model.big)

        val finalReasoner = reasoner ?: DefaultReasoner(smallProvider, finalContextManager)
        val finalGenerator = codeGenerator ?: DefaultCodeGenerator(bigProvider, finalContextManager)

        val agent = CodingAgent(
            finalContextManager,
            AutomatedCoder(finalReasoner, finalGenerator),
            AutomatedAnalyst(finalReasoner)
        ).also {
            if (interactionHandler != null) {
                it.registerHandler(interactionHandler!!)
            }
        }
        return Kompanion(agent)
    }

    private fun getFinalLLMProvider(name: String): LLMProvider {
        return LLMRegistry.getProviderForModel(name)
            ?: throw IllegalArgumentException("Unable to find provider for model $name")
    }

    fun withInteractionHandler(interactionHandler: StubInteractionHandler) = apply {
        this.interactionHandler = interactionHandler
    }
}

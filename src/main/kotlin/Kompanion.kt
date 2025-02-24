import agent.CodeGenerator
import agent.CodingAgent
import agent.ContextManager
import agent.InMemoryContextManager
import agent.coding.DefaultCodeGenerator
import agent.domain.CodeApplier
import agent.interaction.InteractionHandler
import agent.reason.DefaultReasoner
import agent.reason.Reasoner
import ai.LLMProvider
import ai.LLMRegistry
import arrow.core.Either
import arrow.core.getOrElse
import config.AppConfig

class Kompanion(
    val agent: CodingAgent
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
        val smallProvider = Either.catch {
            getFinalLLMProvider(AppConfig.load().model.small)
        }.getOrElse { getFinalLLMProvider("gpt-4o-mini") }

        val bigProvider = Either.catch {
            getFinalLLMProvider(AppConfig.load().model.big)
        }.getOrElse { getFinalLLMProvider("gpt-4o") }

        val finalReasoner = reasoner ?: DefaultReasoner(smallProvider, finalContextManager)
        val finalGenerator = codeGenerator ?: DefaultCodeGenerator(bigProvider, finalContextManager)

        val agent = CodingAgent(
            finalContextManager,
            finalReasoner,
            finalGenerator,
            interactionHandler!!,
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

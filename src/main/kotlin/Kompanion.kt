import agent.*
import agent.coding.CodeGenerator
import agent.domain.CodeApplier
import agent.interaction.AgentMessage
import agent.interaction.InteractionHandler
import agent.modes.BlockchainMode
import agent.modes.CodingMode
import agent.modes.Mode
import agent.reason.BlockchainReasoner
import agent.reason.DefaultReasoner
import agent.reason.Reasoner
import ai.LLMProvider
import ai.LLMRegistry
import arrow.core.Either
import arrow.core.getOrElse
import config.AppConfig
import config.Provider
import mcp.McpManager
import java.util.*

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
        CODE, BLOCKCHAIN
    }

    private var reasoner: Reasoner? = null
    private var codeGenerator: CodeGenerator? = null
    private var codeApplier: CodeApplier? = null
    private var contextManager: ContextManager? = null
    private var smallLlmProvider: LLMProvider? = null
    private var bigLlmProvider: LLMProvider? = null
    private var interactionHandler: InteractionHandler? = null
    private var mode: AgentMode = AgentMode.BLOCKCHAIN
    private var appConfig: AppConfig? = null
    private var provider: Provider? = null


    fun withMode(mode: AgentMode) = apply {
        this.mode = mode
    }

    fun withContextManager(customContextManager: ContextManager) = apply {
        contextManager = customContextManager
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

    fun withAppConfig(config: AppConfig) = apply {
        appConfig = config
    }

    fun withProvider(provider: Provider) = apply {
        this.provider = provider
    }

    fun build(): Kompanion {
        val toolManager = ToolManager()
        val finalAppConfig = appConfig ?: AppConfig.load()
        val finalContextManager = contextManager ?: InMemoryContextManager()

        // Determine which provider to use
        val selectedProvider = this.provider ?: Provider.OPENAI


        val smallProvider = Either.catch {
            getFinalLLMProvider(selectedProvider.small)
        }.getOrElse { getFinalLLMProvider("gpt-4o-mini") }

        val bigProvider = Either.catch {
            getFinalLLMProvider(selectedProvider.big)
        }.getOrElse { getFinalLLMProvider("gpt-4o") }

        val finalReasoner = reasoner
        val finalGenerator = codeGenerator ?: CodeGenerator(bigProvider, finalContextManager, toolManager)

        // Ensure we have an interaction handler
        if (interactionHandler == null) {
            throw IllegalStateException("An interaction handler must be provided")
        }

        val selectedMode: Mode = when (mode) {
            AgentMode.CODE -> CodingMode(
                finalReasoner ?: DefaultReasoner(smallProvider, finalContextManager, toolManager),
                finalGenerator,
                interactionHandler!!,
                toolManager,
                McpManager(),
                finalContextManager,
            )

            AgentMode.BLOCKCHAIN -> BlockchainMode(
                BlockchainReasoner(
                    bigProvider,
                    toolManager,
                    finalContextManager
                ),
                toolManager,
                McpManager(),
                finalContextManager,
                interactionHandler!!
            )
        }

        val interactionSavingWrapper = object : InteractionHandler {
            override suspend fun interact(agentMessage: AgentMessage): String {
                return interactionHandler!!.interact(agentMessage)
            }

            override fun removeChat(id: UUID) {
                interactionHandler!!.removeChat(id)
            }
        }

        val agent = Agent(
            finalContextManager,
            interactionSavingWrapper,
            selectedMode
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
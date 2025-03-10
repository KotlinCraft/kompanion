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
import blockchain.etherscan.EtherscanClientManager
import config.AppConfig
import config.Provider
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
    private var etherscanClientManager: EtherscanClientManager? = null
    private var mode: AgentMode = AgentMode.BLOCKCHAIN
    private var appConfig: AppConfig? = null
    private var provider: Provider? = null


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

    fun withEtherscanClientManager(manager: EtherscanClientManager) = apply {
        etherscanClientManager = manager
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
                finalContextManager,
            )

            AgentMode.BLOCKCHAIN -> BlockchainMode(
                BlockchainReasoner(
                    bigProvider,
                    toolManager,
                    finalContextManager
                ),
                toolManager,
                finalContextManager,
                interactionHandler!!
            )
        }

        val interactionSavingWrapper = object : InteractionHandler {
            override suspend fun interact(agentMessage: AgentMessage): String {
                // Store important agent messages in the context manager instead of writing directly to file
                if (agentMessage.important) {
                    (finalContextManager as? InMemoryContextManager)?.addAgentMessage(agentMessage.message)
                        ?: finalContextManager.storeMessage("Kompanion: ${agentMessage.message}")
                }
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
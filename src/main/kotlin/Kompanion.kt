import agent.Agent
import agent.CodeGenerator
import agent.ContextManager
import agent.InMemoryContextManager
import agent.blockchain.bankless.BanklessClient
import agent.coding.DefaultCodeGenerator
import agent.domain.CodeApplier
import agent.fileops.KompanionFile
import agent.fileops.KompanionFileHandler
import agent.fileops.KompanionFileHandler.Companion.kompanionFolderExists
import agent.interaction.AgentMessage
import agent.interaction.InteractionHandler
import agent.modes.AnalystMode
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
        ASK, CODE, BLOCKCHAIN
    }

    private var reasoner: Reasoner? = null
    private var codeGenerator: CodeGenerator? = null
    private var codeApplier: CodeApplier? = null
    private var contextManager: ContextManager? = null
    private var smallLlmProvider: LLMProvider? = null
    private var bigLlmProvider: LLMProvider? = null
    private var interactionHandler: InteractionHandler? = null
    private var etherscanClientManager: EtherscanClientManager? = null
    private var mode: AgentMode = AgentMode.ASK
    private var appConfig: AppConfig? = null


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

    fun build(): Kompanion {
        val finalAppConfig = appConfig ?: AppConfig.load()
        val finalContextManager = contextManager ?: InMemoryContextManager()
        val smallProvider = Either.catch {
            getFinalLLMProvider(finalAppConfig.currentProvider.small)
        }.getOrElse { getFinalLLMProvider("gpt-4o-mini") }

        val bigProvider = Either.catch {
            getFinalLLMProvider(finalAppConfig.currentProvider.big)
        }.getOrElse { getFinalLLMProvider("gpt-4o") }

        val finalReasoner = reasoner
        val finalGenerator = codeGenerator ?: DefaultCodeGenerator(bigProvider, finalContextManager)
        val finalEtherscanClientManager = etherscanClientManager ?: EtherscanClientManager()

        // Ensure we have an interaction handler
        if (interactionHandler == null) {
            throw IllegalStateException("An interaction handler must be provided")
        }

        val selectedMode: Mode = when (mode) {
            AgentMode.ASK -> AnalystMode(finalReasoner ?: DefaultReasoner(smallProvider, finalContextManager))
            AgentMode.CODE -> CodingMode(
                finalReasoner ?: DefaultReasoner(smallProvider, finalContextManager),
                finalGenerator,
                interactionHandler!!
            )

            AgentMode.BLOCKCHAIN -> BlockchainMode(
                BlockchainReasoner(
                    bigProvider,
                    finalContextManager
                ), finalEtherscanClientManager, BanklessClient(), interactionHandler!!
            )
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
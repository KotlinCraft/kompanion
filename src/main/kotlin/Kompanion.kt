import agent.*
import agent.coding.CodeGenerator
import agent.domain.CodeApplier
import agent.interaction.AgentMessage
import agent.interaction.InteractionHandler
import agent.modes.BlockchainMode
import agent.modes.CodingMode
import agent.modes.Mode
import agent.reason.*
import ai.LLMProvider
import ai.LLMRegistry
import arrow.core.Either
import arrow.core.getOrElse
import config.AppConfig
import config.Provider
import mcp.McpManager
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.InMemoryChatMemory
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
            return builder().withInteractionHandler(interactionHandler).build()
        }
    }
}

class KompanionBuilder {

    enum class AgentMode {
        CODE, BLOCKCHAIN
    }

    private var codeGenerator: CodeGenerator? = null
    private var codeApplier: CodeApplier? = null
    private var contextManager: ContextManager? = null
    private var smallLlmProvider: LLMProvider? = null
    private var bigLlmProvider: LLMProvider? = null
    private var interactionHandler: InteractionHandler? = null
    private var mode: AgentMode = AgentMode.BLOCKCHAIN
    private var appConfig: AppConfig? = null
    private var provider: Provider? = null
    private var chatMemory: ChatMemory = InMemoryChatMemory()

    fun withMode(mode: AgentMode) = apply {
        this.mode = mode
    }

    fun withChatMemory(memory: ChatMemory) = apply {
        this.chatMemory = memory
    }

    fun withContextManager(customContextManager: ContextManager) = apply {
        contextManager = customContextManager
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
        val finalContextManager = contextManager ?: InMemoryContextManager()

        // Determine which provider to use
        val selectedProvider = this.provider ?: Provider.OPENAI

        val memoryAdvisor = PromptChatMemoryAdvisor(chatMemory)


        val llmProvider = Either.catch {
            getFinalLLMProvider(selectedProvider.normal)
        }.getOrElse { getFinalLLMProvider("gpt-4o") }.addAdvisor(memoryAdvisor)

        val reasoningProvider = Either.catch {
            getFinalLLMProvider(selectedProvider.reasoning)
        }.getOrElse { getFinalLLMProvider("o3-mini") }.addAdvisor(memoryAdvisor)

        val finalGenerator = codeGenerator ?: CodeGenerator(llmProvider, finalContextManager, toolManager)

        // Ensure we have an interaction handler
        if (interactionHandler == null) {
            throw IllegalStateException("An interaction handler must be provided")
        }

        val selectedMode: Mode = when (mode) {
            AgentMode.CODE -> CodingMode(
                CodingAnalyst(finalContextManager, llmProvider, toolManager),
                CodingPlanner(
                    SimplePlanner(
                        llmProvider, finalContextManager, toolManager
                    ),
                    DefaultReasoningStrategy(
                        reasoningProvider, finalContextManager
                    )
                ),
                finalGenerator, interactionHandler!!, toolManager, McpManager(), finalContextManager
            )

            AgentMode.BLOCKCHAIN -> BlockchainMode(
                BlockchainReasoner(
                    llmProvider, toolManager, finalContextManager
                ), toolManager, McpManager(), finalContextManager, interactionHandler!!
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
            finalContextManager, interactionSavingWrapper, chatMemory, selectedMode
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
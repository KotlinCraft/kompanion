import agent.Agent
import agent.ContextManager
import agent.InMemoryContextManager
import agent.ToolManager
import agent.coding.ExtensibleFlowCodeGenerator
import agent.interaction.InteractionHandler
import agent.modes.AutoMode
import agent.modes.CodingMode
import agent.reason.*
import ai.LLMProvider
import ai.LLMRegistry
import config.AppConfig
import config.Provider
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.InMemoryChatMemory

class Kompanion(val agent: Agent) {
    companion object {
        fun builder(): KompanionBuilder {
            LLMProvider.load()
            return KompanionBuilder()
        }

        fun default(interactionHandler: InteractionHandler): Kompanion =
            builder().withInteractionHandler(interactionHandler).build()
    }
}

class KompanionBuilder {
    enum class AgentMode { CODE, FULL_AUTO }

    private var contextManager: ContextManager? = null
    private var interactionHandler: InteractionHandler? = null
    private var mode: AgentMode = AgentMode.FULL_AUTO
    private var appConfig: AppConfig? = null
    private var provider: Provider? = null
    private var chatMemory: ChatMemory = InMemoryChatMemory()

    fun withMode(mode: AgentMode) = apply { this.mode = mode }
    fun withChatMemory(memory: ChatMemory) = apply { this.chatMemory = memory }
    fun withContextManager(customContextManager: ContextManager) = apply { contextManager = customContextManager }
    fun withAppConfig(config: AppConfig) = apply { appConfig = config }
    fun withProvider(provider: Provider) = apply { this.provider = provider }
    fun withInteractionHandler(interactionHandler: InteractionHandler) =
        apply { this.interactionHandler = interactionHandler }

    fun build(): Kompanion {
        requireNotNull(interactionHandler) { "An interaction handler must be provided" }

        val toolManager = ToolManager()
        val finalContextManager = contextManager ?: InMemoryContextManager()
        val selectedProvider = provider ?: Provider.OPENAI
        val memoryAdvisor = PromptChatMemoryAdvisor(chatMemory)

        // Create LLM providers with error handling
        val normalLLMProvider = runCatching {
            getFinalLLMProvider(selectedProvider.normal)
        }.getOrElse {
            getFinalLLMProvider("gpt-4o")
        }.addAdvisor(memoryAdvisor)

        val reasoningProvider = runCatching {
            getFinalLLMProvider(selectedProvider.reasoning)
        }.getOrElse {
            getFinalLLMProvider("o3-mini")
        }.addAdvisor(memoryAdvisor)

        // Create code generators
        val codeGenerator = ExtensibleFlowCodeGenerator(reasoningProvider, finalContextManager, interactionHandler!!)

        // Create the selected mode
        val selectedMode = when (mode) {
            AgentMode.CODE -> createCodingMode(
                finalContextManager, normalLLMProvider, toolManager,
                reasoningProvider, codeGenerator, standalone = true
            )

            AgentMode.FULL_AUTO -> createAutoMode(
                finalContextManager, normalLLMProvider, toolManager,
                reasoningProvider, codeGenerator
            )
        }

        // Create the agent
        val agent = Agent(
            finalContextManager,
            interactionHandler!!,
            chatMemory,
            selectedMode
        )

        return Kompanion(agent)
    }

    private fun createCodingMode(
        contextManager: ContextManager,
        llmProvider: LLMProvider,
        toolManager: ToolManager,
        reasoningProvider: LLMProvider,
        codeGenerator: ExtensibleFlowCodeGenerator,
        standalone: Boolean
    ) = CodingMode(
        CodingAnalyst(contextManager, llmProvider, toolManager),
        CodingPlanner(
            SimplePlanner(llmProvider, contextManager, toolManager),
            DefaultReasoningStrategy(reasoningProvider, contextManager)
        ),
        codeGenerator,
        interactionHandler!!,
        toolManager,
        contextManager,
        standalone
    )

    private fun createAutoMode(
        contextManager: ContextManager,
        llmProvider: LLMProvider,
        toolManager: ToolManager,
        reasoningProvider: LLMProvider,
        codeGenerator: ExtensibleFlowCodeGenerator
    ) = AutoMode(
        AutomodePlanner(reasoningProvider, toolManager, contextManager, interactionHandler!!),
        AutomodeExecutor(llmProvider, toolManager, contextManager, interactionHandler!!),
        codingMode = createCodingMode(
            contextManager, llmProvider, toolManager,
            reasoningProvider, codeGenerator, standalone = false
        ),
        toolManager,
        contextManager,
        interactionHandler!!
    )

    private fun getFinalLLMProvider(name: String): LLMProvider =
        LLMRegistry.getProviderForModel(name)
            ?: throw IllegalArgumentException("Unable to find provider for model $name")
}
class Kompanion private constructor(
    private val agent: CodingAgent
) {
    companion object {
        fun builder(): KompanionBuilder {
            return KompanionBuilder()
        }
    }

    fun getAgent(): CodingAgent = agent
}

class KompanionBuilder {
    private var reasoner: Reasoner? = null
    private var codeGenerator: CodeGenerator? = null
    private var codeApplier: CodeApplier? = null
    private var contextManager: ContextManager? = null
    private var llmProvider: LLMProvider? = null

    fun withWorkingDirectory(directory: String) = apply {
        contextManager = InMemoryContextManager(directory)
    }

    fun withLLMProvider(provider: LLMProvider) = apply {
        llmProvider = provider
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
        val finalContextManager = contextManager ?: InMemoryContextManager(System.getProperty("user.dir"))
        val finalLLMProvider = llmProvider ?: OpenAILLMProvider()
        
        val finalReasoner = reasoner ?: DefaultReasoner(finalLLMProvider, finalContextManager)
        val finalGenerator = codeGenerator ?: DefaultCodeGenerator(finalLLMProvider, finalContextManager)
        val finalApplier = codeApplier ?: FileSystemCodeApplier(finalContextManager)

        val agent = CodingAgent(
            reasoner = finalReasoner,
            codeGenerator = finalGenerator,
            codeApplier = finalApplier
        )

        return Kompanion(agent)
    }
}

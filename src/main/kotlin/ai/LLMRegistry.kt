package ai

/**
 * Registry for LLM models and their providers
 */
object LLMRegistry {
    private val modelProviders = mutableMapOf<String, LLMProvider>()

    fun registerModel(provider: LLMProvider, model: String) {
        modelProviders[model] = provider.setModel(model)
    }

    fun getProviderForModel(model: String): LLMProvider? {
        return modelProviders[model]
    }
}

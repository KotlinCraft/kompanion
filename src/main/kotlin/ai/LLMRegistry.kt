package ai

/**
 * Registry for LLM models and their providers
 */
object LLMRegistry {
    private val modelProviders = mutableMapOf<String, LLMProvider>()

    fun registerModels(provider: LLMProvider, models: List<String>) {
        models.forEach { model ->
            modelProviders[model] = provider
        }
    }

    fun getProviderForModel(model: String): LLMProvider? {
        return modelProviders[model]
    }

    fun getAllRegisteredModels(): Set<String> {
        return modelProviders.keys
    }

    fun clear() {
        modelProviders.clear()
    }
}

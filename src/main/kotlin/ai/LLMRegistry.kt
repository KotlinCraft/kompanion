package ai

/**
 * Registry for LLM models and their providers
 */
object LLMRegistry {
    private val modelProviders = mutableMapOf<String, MutableSet<Class<out LLMProvider>>>()
    
    fun registerModels(providerClass: Class<out LLMProvider>, models: List<String>) {
        models.forEach { model ->
            modelProviders.getOrPut(model) { mutableSetOf() }.add(providerClass)
        }
    }
    
    fun getProvidersForModel(model: String): Set<Class<out LLMProvider>> {
        return modelProviders[model] ?: emptySet()
    }
    
    fun getAllRegisteredModels(): Set<String> {
        return modelProviders.keys
    }
    
    fun clear() {
        modelProviders.clear()
    }
}

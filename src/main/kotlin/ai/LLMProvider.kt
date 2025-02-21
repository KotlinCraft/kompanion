package ai

import org.reflections.Reflections
import org.springframework.core.ParameterizedTypeReference

/**
 * Provider interface for Large Language Models
 */
interface LLMProvider {

    companion object {
        fun register(providerClass: Class<out LLMProvider>) {
            try {
                val instance = providerClass.getDeclaredConstructor().newInstance()
                LLMRegistry.registerModels(instance, instance.getSupportedModels())
            } catch (e: Exception) {
                // Skip registration if we can't instantiate
            }
        }

        fun load() {
            val reflections = Reflections("ai") // Scans the ai package where providers live
            val providers = reflections.getSubTypesOf(LLMProvider::class.java)

            providers.forEach { providerClass ->
                try {
                    // Skip the interface itself
                    if (!providerClass.isInterface) {
                        register(providerClass)
                    }
                } catch (e: Exception) {
                    println("Failed to register provider ${providerClass.simpleName}: ${e.message}")
                }
            }
        }
    }

    suspend fun <T> prompt(
        input: String,
        action: List<Action>,
        temperature: Double,
        parameterizedTypeReference: ParameterizedTypeReference<T>,
        retry: Boolean = true
    ): T

    fun getSupportedModels(): List<String>
}

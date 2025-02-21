package ai

import org.springframework.core.ParameterizedTypeReference

/**
 * Provider interface for Large Language Models
 */
interface LLMProvider {
    companion object {
        fun register(providerClass: Class<out LLMProvider>) {
            try {
                val instance = providerClass.getDeclaredConstructor().newInstance()
                LLMRegistry.registerModels(providerClass, instance.getSupportedModels())
            } catch (e: Exception) {
                // Skip registration if we can't instantiate
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

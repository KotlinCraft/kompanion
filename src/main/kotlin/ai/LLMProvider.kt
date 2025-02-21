package ai

import org.springframework.core.ParameterizedTypeReference

/**
 * Provider interface for Large Language Models
 */
interface LLMProvider {
    suspend fun <T> prompt(
        input: String,
        action: List<Action>,
        temperature: Double,
        parameterizedTypeReference: ParameterizedTypeReference<T>,
        retry: Boolean = true
    ): T

    fun getSupportedModels(): List<String>
}

package ai

import org.reflections.Reflections
import org.springframework.ai.tool.ToolCallback
import org.springframework.core.ParameterizedTypeReference

/**
 * Provider interface for Large Language Models
 */
abstract class LLMProvider {

    lateinit var model: String

    fun setModel(model: String): LLMProvider {
        if (getSupportedModels().contains(model)) {
            this.model = model
            return this
        } else {
            throw IllegalArgumentException("Model $model is not supported by this provider")
        }
    }

    companion object {
        fun register(providerClass: Class<out LLMProvider>) {
            try {
                val stub = providerClass.getDeclaredConstructor().newInstance()
                stub.getSupportedModels().forEach {
                    LLMRegistry.registerModel(
                        providerClass.getDeclaredConstructor().newInstance(),
                        it
                    )
                }
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

    abstract suspend fun <T> prompt(
        system: String,
        userMessage: String?,
        actions: List<Action>,
        temperature: Double,
        parameterizedTypeReference: ParameterizedTypeReference<T>,
        retry: Boolean = true,
        toolcallbacks: MutableList<ToolCallback>
    ): T

    abstract fun getSupportedModels(): List<String>
}

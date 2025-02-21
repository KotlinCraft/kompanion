package ai

import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

/**
 * Registry that discovers and manages LLM providers at runtime
 */
object LLMProviderRegistry {
    fun getAllProviderTypes(): Set<Class<out LLMProvider>> {
        val reflections = Reflections(
            ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("ai"))
                .setScanners(Scanners.SubTypes)
        )
        return reflections.getSubTypesOf(LLMProvider::class.java)
    }

    fun getAllSupportedModels(): Map<String, List<String>> {
        return getAllProviderTypes()
            .mapNotNull { providerClass ->
                try {
                    val instance = providerClass.getDeclaredConstructor().newInstance()
                    providerClass.simpleName to instance.getSupportedModels()
                } catch (e: Exception) {
                    null // Skip if we can't instantiate
                }
            }.toMap()
    }
}

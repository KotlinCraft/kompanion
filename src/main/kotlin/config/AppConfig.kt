package config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Paths

enum class Provider(val small: String, val big: String) {
    ANTHROPIC(
        "claude-3-5-haiku-latest",
        "claude-3-7-sonnet-latest"
    ),
    OPENAI(
        "gpt-4o-mini",
        "o3-mini"
    )
}

data class AppConfig(
    val openAiKey: String = "",
    val anthropicKey: String = "",
    val latestDirectory: String = "~",
    val etherscan: EtherscanConfig = EtherscanConfig(),
    val currentProvider: Provider = Provider.OPENAI,
    val banklessToken: String = ""
) {
    companion object {
        private val configPath = Paths.get(System.getProperty("user.home"), ".kompanion", "config.yml")
        private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

        fun load(): AppConfig {
            val configFile = configPath.toFile()

            if (!configFile.exists()) {
                configFile.parentFile.mkdirs()
                configFile.createNewFile()
                return AppConfig()
            }

            return try {
                mapper.readValue(configFile, AppConfig::class.java)
            } catch (e: Exception) {
                println("Failed to load config: ${e.message}")
                AppConfig()
            }
        }

        fun save(config: AppConfig) {
            try {
                mapper.writeValue(configPath.toFile(), config)
            } catch (e: Exception) {
                println("Failed to save config: ${e.message}")
            }
        }
    }
}

data class ConfigModel(
    val small: String,
    val big: String
)

data class EtherscanConfig(
    val baseApiKey: String = "",
    val ethereumApiKey: String = ""
)
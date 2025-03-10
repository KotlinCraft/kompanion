package mcp

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Configuration data classes for JSON deserialization
 */
private data class McpConfig(
    val mcpServers: Map<String, McpServerConfig> = emptyMap()
)

private data class McpServerConfig(
    val command: String,
    val env: Map<String, String>? = null,
    val args: List<String>? = null
)

/**
 * Reader class for parsing MCP server configurations from JSON using Jackson
 */
class McpReader {
    companion object {
        private val objectMapper = ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        /**
         * Reads and parses MCP server configurations from ~/.kompanion/mcp.json
         * @return List of McpServer objects, or empty list if file doesn't exist or has errors
         */
        fun readMcpServers(): List<McpServer> {
            val configFile = File(System.getProperty("user.home") + "/.kompanion/mcp.json")

            if (!configFile.exists()) {
                println("MCP configuration file not found: ${configFile.absolutePath}")
                return emptyList()
            }

            try {
                // Read and deserialize the JSON file using Jackson
                val mcpConfig = objectMapper.readValue<McpConfig>(configFile)

                // Convert the configuration into McpServer objects
                return mcpConfig.mcpServers.map { (name, config) ->
                    McpServer(
                        name = name,
                        command = config.command,
                        env = config.env ?: emptyMap(),
                        args = config.args ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                println("Error parsing MCP configuration: ${e.message}")
                return emptyList()
            }
        }
    }
}
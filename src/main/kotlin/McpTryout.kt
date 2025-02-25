import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.spec.McpSchema
import java.time.Duration


fun main() {
    var params: ServerParameters = ServerParameters.builder("npx")
        .env(mapOf("LOG_ENABLED" to "true"))
        .args("-y", "@jetbrains/mcp-proxy")
        .build()
    var transport = StdioClientTransport(params)

    var client: McpSyncClient = McpClient.sync(transport)
        .requestTimeout(Duration.ofSeconds(10))
        .capabilities(
            McpSchema.ClientCapabilities.builder()
                .roots(true) // Enable roots capability
                .sampling() // Enable sampling capability
                .build()
        ).build()

    client.listTools().tools.forEach {
        println(it)
    }
}

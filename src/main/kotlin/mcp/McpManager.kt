package mcp

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.spec.McpSchema
import java.time.Duration

class McpManager {

    fun getMcpServers(): List<McpSyncClient> {
        val servers = McpReader.readMcpServers()
        return servers.map {
            val params: ServerParameters = ServerParameters.builder(it.command)
                .args(it.args)
                .env(it.env)
                .build()
            val transport = StdioClientTransport(params)
            McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(5))
                .capabilities(
                    McpSchema.ClientCapabilities.builder()
                        .build()
                ).build()
        }
    }
}
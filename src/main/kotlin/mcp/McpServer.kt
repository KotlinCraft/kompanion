package mcp

data class McpServer(
    val name: String,
    val command: String,
    val env: Map<String, String> = emptyMap(),
    val args: List<String> = emptyList()
)
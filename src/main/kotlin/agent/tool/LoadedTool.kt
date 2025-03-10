package agent.tool

data class LoadedTool(
    val id: String,
    val name: String,
    val tool: Tool
) {
    fun toggleStatus() {
        tool.allowedStatus = when (tool.allowedStatus) {
            ToolAllowedStatus.ALLOWED -> ToolAllowedStatus.NOT_ALLOWED
            ToolAllowedStatus.NOT_ALLOWED -> ToolAllowedStatus.ALLOWED
            null -> ToolAllowedStatus.ALLOWED
        }
    }
}
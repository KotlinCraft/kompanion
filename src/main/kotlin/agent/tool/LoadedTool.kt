package agent.tool

data class LoadedTool(
    val id: String,
    val name: String,
    var allowedStatus: ToolAllowedStatus? = null
)
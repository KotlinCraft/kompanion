package agent.tool

import ai.Action

class Tool(
    val id: String,
    val action: Action,
    var allowedStatus: ToolAllowedStatus? = null
) {
    companion object {
        fun from(action: Action): Tool {
            return Tool(action.name, action)
        }
    }
}

enum class ToolAllowedStatus {
    ALLOWED,
    NOT_ALLOWED
}
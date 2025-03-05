package agent.tool

import ai.Action

class Tool(
    val id: String,
    val action: Action,
    var allowedStatus: ToolAllowedStatus? = null
) {
    companion object {
        fun from(action: Action, allowedStatus: ToolAllowedStatus? = null): Tool {
            return Tool(action.name, action, allowedStatus)
        }
    }
}

enum class ToolAllowedStatus {
    ALLOWED,
    NOT_ALLOWED
}
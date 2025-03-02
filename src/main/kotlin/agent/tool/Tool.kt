package agent.tool

import ai.Action

class Tool(
    val action: Action,
    var allowedStatus: ToolAllowedStatus? = null
)

enum class ToolAllowedStatus {
    ALLOWED,
    NOT_ALLOWED
}
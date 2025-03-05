package agent.tool

import org.springframework.ai.tool.ToolCallback
import java.util.*

class Tool(
    val id: String,
    val toolCallback: ToolCallback,
    var allowedStatus: ToolAllowedStatus? = null,
    val showUpInTools: Boolean,
) {
    companion object {
        fun from(toolCallback: ToolCallback, allowedStatus: ToolAllowedStatus? = null, showUpInTools: Boolean = true): Tool {
            return Tool(UUID.randomUUID().toString(), toolCallback, allowedStatus, showUpInTools)
        }
    }
}

enum class ToolAllowedStatus {
    ALLOWED,
    NOT_ALLOWED
}
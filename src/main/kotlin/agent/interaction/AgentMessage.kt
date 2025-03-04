package agent.interaction

import androidx.compose.runtime.Composable
import ui.chat.ToolUsageIndicator
import java.util.UUID

sealed class AgentMessage(val message: String, val important: Boolean)

class AgentQuestion(message: String, important: Boolean = false) : AgentMessage(message, important)
class AgentResponse(message: String, important: Boolean = false) : AgentMessage(message, important)
class AgentAskConfirmation(message: String, important: Boolean = false) : AgentMessage(message, important)
class ToolUsageMessage(
    action: String = "",
    val status: ToolStatus = ToolStatus.RUNNING,
    val id: UUID = UUID.randomUUID(),
    val toolIndicator: @Composable () -> Unit
) : AgentMessage(action, important = false)

enum class ToolStatus {
    RUNNING, COMPLETED, FAILED
}
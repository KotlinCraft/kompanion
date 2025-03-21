package agent.interaction

import androidx.compose.runtime.Composable
import java.util.UUID

sealed class AgentMessage(val id: UUID, val message: String, val important: Boolean)

class AgentQuestion(message: String, important: Boolean = false) : AgentMessage(UUID.randomUUID(), message, important)
class AgentResponse(message: String, important: Boolean = false) : AgentMessage(UUID.randomUUID(), message, important)
class AgentAskConfirmation(message: String, important: Boolean = false) : AgentMessage(UUID.randomUUID(), message, important)
class ToolUsageMessage(
    action: String = "",
    id: UUID = UUID.randomUUID(),
    val toolIndicator: @Composable () -> Unit
) : AgentMessage(id, action, important = false)

enum class ToolStatus {
    RUNNING, COMPLETED, FAILED
}
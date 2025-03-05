package agent.domain.context

import java.util.UUID

data class ContextFile(
    val id: UUID,
    val name: String,
    val content: String
)
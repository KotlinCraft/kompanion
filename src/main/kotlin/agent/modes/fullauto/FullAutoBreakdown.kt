package agent.modes.fullauto

import java.util.UUID


data class FullAutoBreakdown(
    val id: UUID,
    val steps: List<Step>
)

data class Step(
    val id: UUID,
    val instruction: String,
    val subTasks: List<String>
)
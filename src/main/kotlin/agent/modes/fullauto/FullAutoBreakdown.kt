package agent.modes.fullauto

import java.util.UUID


data class FullAutoBreakdown(
    val id: UUID = UUID.randomUUID(),
    val steps: List<Step>
)

data class Step(
    val id: UUID = UUID.randomUUID(),
    val stepNumber: Int,
    val instruction: String,
    val subTasks: List<String>
)
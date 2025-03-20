package ui.task

data class TaskItem(
    val id: String,
    val description: String,
    val status: TaskStatus,
    val timestamp: Long
)
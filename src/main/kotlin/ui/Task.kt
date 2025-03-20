package ui

enum class TaskStatus {
    TODO,
    DOING,
    DONE
}

data class Task(
    val name: String,
    val status: TaskStatus
)
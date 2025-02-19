package agent.domain

data class UserFeedback(
    val responseId: String,
    val isSuccess: Boolean,
    val comments: String?
)
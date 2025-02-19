package agent

interface CodeAgent {
    suspend fun process(request: UserRequest): AgentResponse
    fun updateContext(codeFiles: List<CodeFile>)
    fun addFeedback(feedback: UserFeedback)
}
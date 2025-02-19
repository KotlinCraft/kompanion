package agent

interface ContextManager {
    fun updateFiles(files: List<CodeFile>)
    fun getRelevantContext(request: UserRequest): List<CodeFile>
    fun clearContext()
}
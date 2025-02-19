package agent

import agent.domain.CodeFile
import agent.domain.UserRequest

interface ContextManager {
    fun updateFiles(files: List<CodeFile>)
    fun getRelevantContext(request: UserRequest): List<CodeFile>
    fun clearContext()
}
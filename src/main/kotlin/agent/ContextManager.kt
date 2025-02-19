package agent

import agent.domain.CodeFile
import agent.domain.UserRequest

interface ContextManager {
    //TODO: make this smart?
    fun getContext(): List<CodeFile>
    fun updateFiles(files: List<CodeFile>)
    fun clearContext()
}
package agent

import agent.domain.CodeFile
import agent.domain.UserRequest

class InMemoryContextManager : ContextManager {

    private val files = mutableMapOf<String, CodeFile>()

    override fun updateFiles(newFiles: List<CodeFile>) {
        newFiles.forEach { file ->
            files[file.path] = file
        }
    }

    override fun getRelevantContext(request: UserRequest): List<CodeFile> {
        // For simplicity, return all files. This can be enhanced to filter based on relevance.
        return files.values.toList()
    }

    override fun clearContext() {
        files.clear()
    }
}

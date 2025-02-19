package agent

import agent.domain.CodeFile

class InMemoryContextManager : ContextManager {

    private val files = mutableListOf<CodeFile>()

    override fun updateFiles(files: List<CodeFile>) {
        this.files.clear()
        this.files.addAll(files)
    }

    override fun getRelevantContext(request: UserRequest): List<CodeFile> {
        // For simplicity, return all files. This can be enhanced to filter based on relevance.
        return files
    }

    override fun clearContext() {
        files.clear()
    }
}

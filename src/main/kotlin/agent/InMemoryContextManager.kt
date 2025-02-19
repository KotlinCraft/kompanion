package agent

import agent.domain.CodeFile
import agent.domain.UserRequest

class InMemoryContextManager : ContextManager {

    private val files = mutableMapOf<String, CodeFile>()
    override fun getContext(): List<CodeFile> {
        return files.values.toList()
    }

    override fun updateFiles(files: List<CodeFile>) {
        files.forEach { file ->
            this.files[file.path] = file
        }
    }

    override fun clearContext() {
        files.clear()
    }
}

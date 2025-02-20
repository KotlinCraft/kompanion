package agent

import agent.domain.CodeFile
import utils.walkDirectory
import java.io.File

class InMemoryContextManager(
    val workingDirectory: String
) : ContextManager {

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

    override fun fetchWorkingDirectory(): String {
        return workingDirectory
    }

    override fun getFullFileList(): String {
        return buildString {
            walkDirectory(File(workingDirectory), this, 0)
        }
    }
}

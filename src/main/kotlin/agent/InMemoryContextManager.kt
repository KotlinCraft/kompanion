package agent

import agent.domain.CodeFile
import config.AppConfig
import utils.walkDirectory
import java.io.File

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

    override fun fetchWorkingDirectory(): String {
        return AppConfig.load().latestDirectory
    }

    override fun getFullFileList(): String {
        return buildString {
            walkDirectory(File(AppConfig.load().latestDirectory.trim()), this, 0)
        }
    }


}

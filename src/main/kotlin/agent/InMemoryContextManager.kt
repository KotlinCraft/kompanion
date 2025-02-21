package agent

import agent.domain.CodeFile
import config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import utils.walkDirectory
import java.io.File

class InMemoryContextManager : ContextManager {

    private val _files = MutableStateFlow<List<CodeFile>>(listOf())
    val files: StateFlow<List<CodeFile>> = _files.asStateFlow()

    override fun getContext(): StateFlow<List<CodeFile>> {
        return files
    }

    override fun updateFiles(files: List<CodeFile>) {
        files.forEach { file ->
            val unique = files + this.files.value
            this._files.value = unique
        }
    }

    override fun clearContext() {
        _files.value = emptyList()
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

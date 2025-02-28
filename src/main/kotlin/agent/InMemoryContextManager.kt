package agent

import agent.domain.context.ContextFile
import config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import utils.walkDirectory
import java.io.File

class InMemoryContextManager : ContextManager {

    private val _files = MutableStateFlow<Set<ContextFile>>(setOf())
    val files: StateFlow<Set<ContextFile>> = _files.asStateFlow()

    override fun getContext(): StateFlow<Set<ContextFile>> {
        return files
    }

    override fun updateFiles(files: List<ContextFile>) {
        files.forEach { file ->
            val removedPreviousEntries = this.files.value.filter { it.name != file.name }
            val unique = (files + removedPreviousEntries).toSet()
            this._files.value = unique
        }
    }

    override fun clearContext() {
        _files.value = emptySet()
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

package agent

import agent.coding.graph.CodeNode
import agent.coding.graph.KotlinCodeGraphBuilder
import agent.domain.context.ContextFile
import config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import utils.walkDirectory
import java.io.File
import java.util.*

class InMemoryContextManager : ContextManager {

    private val _files = MutableStateFlow<Set<ContextFile>>(emptySet())
    val files: StateFlow<Set<ContextFile>> = _files.asStateFlow()

    val graph = KotlinCodeGraphBuilder().buildFromDirectory(AppConfig.load().latestDirectory)

    override fun getContext(): StateFlow<Set<ContextFile>> = files


    override fun updateFiles(files: List<ContextFile>) {
        _files.value = (_files.value + files).distinctBy { it.name }.toSet()
    }

    override fun removeFile(id: UUID) {
        this._files.value = (_files.value.filter { it.id != id }).toSet()
    }

    override fun clearContext() {
        _files.value = emptySet()
    }

    override fun fetchWorkingDirectory(): String = AppConfig.load().latestDirectory

    override fun getFullFileList(): String = buildString {
        walkDirectory(
            File(AppConfig.load().latestDirectory.trim()),
            this,
            File(AppConfig.load().latestDirectory).absolutePath
        )
    }

    override fun findRelatedFiles(relatedFiles: String): List<File> =
        graph.queryNodes(relatedFiles).filterIsInstance<CodeNode.FileNode>().flatMap {
            graph.getContextForNode(it.id, 2).filterIsInstance<CodeNode.FileNode>()
        }.map {
            File(AppConfig.load().latestDirectory.trim() + "/" + it.path)
        }
}
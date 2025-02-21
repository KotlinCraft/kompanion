package agent

import agent.domain.CodeFile
import config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import utils.walkDirectory
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class InMemoryContextManager : ContextManager {

    private val _files = MutableStateFlow<List<CodeFile>>(listOf())
    val files: StateFlow<List<CodeFile>> = _files.asStateFlow()

    init {
        updateFiles(
            listOf(
                CodeFile(
                    Path("/opt/projects/kotlincraft/kompanion/build.gradle.kts"),
                    "package agent\n\nclass InMemoryContextManager : ContextManager {\n\n\n    private val _files = MutableStateFlow<MutableMap<String, CodeFile>>(mutableMapOf())\n    val files: StateFlow<MutableMap<String, CodeFile>> = _files.asStateFlow()\n    \n    init {\n        updateFiles(listOf(\n            CodeFile(\"src/main/kotlin/agent/InMemoryContextManager.kt\", \"package agent\\n\\nimport agent.domain.CodeFile\\nimport config.AppConfig\\nimport kotlinx.coroutines.flow.MutableStateFlow\\nimport kotlinx.coroutines.flow.StateFlow\\nimport kotlinx.coroutines.flow.asStateFlow\\nimport utils.walkDirectory\\nimport java.io.File\\n\\nclass InMemoryContextManager : ContextManager {\\n\\n\\n    private val _files = MutableStateFlow<MutableMap<String, CodeFile>>(mutableMapOf())\\n    val files: StateFlow<MutableMap<String, CodeFile>> = _files.asStateFlow()\\n    \\n    init {\\n        updateFiles(listOf(\\n            CodeFile(\"src/main/kotlin/agent/InMemoryContextManager.kt\", \"package agent\\n\\nimport agent.domain.CodeFile\\nimport config.AppConfig\\nimport kotlinx.coroutines.flow.MutableStateFlow\\nimport kotlinx.coroutines.flow.StateFlow\\nimport kotlinx.coroutines.flow.asStateFlow\\nimport utils.walkDirectory\\nimport java.io.File\\n\\nclass InMemoryContextManager : ContextManager {\\n\\n\\n    private val _files = MutableStateFlow<MutableMap<String, CodeFile>>(mutableMapOf())\\n    val files: StateFlow<MutableMap<String, CodeFile>> = _files.asStateFlow()\\n    \\n    init {\\n        updateFiles(listOf(\\n            CodeFile(\"src/main/kotlin/agent/InMemoryContextManager.kt\", \"package agent\\n\\nimport agent.domain.CodeFile\\nimport config.AppConfig\\nimport kotlinx.coroutines.flow.MutableStateFlow\\nimport kotlinx.coroutines.flow.StateFlow\\nimport kotlinx.coroutines.flow.asStateFlow\\nimport utils.walkDirectory\\nimport java.io.File\\n\\nclass InMemoryContextManager : ContextManager {\\n\\n\\n",
                    language = "kotlin"
                ),
            )
        )
    }

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

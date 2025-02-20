package agent.domain

import agent.ContextManager
import java.nio.file.Paths
import kotlin.io.path.exists

class FileSystemCodeApplier(private val contextManager: ContextManager) : CodeApplier {
    override fun apply(fileChange: FileChange): Boolean {
        return when (fileChange) {
            is FileChange.CreateFile -> {
                true // Keep existing implementation for now
            }

            is FileChange.ModifyFile -> {
                try {
                    val fullPath = if(Paths.get(fileChange.path).exists()) Paths.get(fileChange.path) else Paths.get(contextManager.fetchWorkingDirectory(), fileChange.path)


                    if (!fullPath.exists()) {
                        println("Error: File ${fullPath} does not exist")
                        return false
                    }

                    val file = fullPath.toFile()

                    var content = file.readText()

                    // Apply each change sequentially
                    fileChange.changes.forEach { change ->
                        content = content.replace(change.searchContent, change.replaceContent)
                    }

                    // Write the modified content back to the file
                    file.writeText(content)
                    true
                } catch (e: Exception) {
                    println("Error modifying file ${fileChange.path}: ${e.message}")
                    false
                }
            }
        }
    }
}

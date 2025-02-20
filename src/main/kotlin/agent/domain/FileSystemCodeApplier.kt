package agent.domain

import agent.ContextManager
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class FileSystemCodeApplier(private val contextManager: ContextManager) : CodeApplier {
    override fun apply(fileChange: FileChange): Boolean {
        return when (fileChange) {
            is FileChange.CreateFile -> {
                true // Keep existing implementation for now
            }

            is FileChange.ModifyFile -> {
                try {
                    val fullPath = Paths.get(contextManager.fetchWorkingDirectory(), fileChange.path)
                    val file = fullPath.toFile()
                    
                    if (!file.exists()) {
                        println("Error: File ${file.absolutePath} does not exist")
                        return false
                    }

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

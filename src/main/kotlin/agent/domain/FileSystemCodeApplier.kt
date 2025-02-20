package agent.domain

import agent.ContextManager

class FileSystemCodeApplier(contextManager: ContextManager) : CodeApplier {
    override fun apply(fileChange: FileChange): Boolean {
        return when (fileChange) {
            is FileChange.CreateFile -> {
                true
            }

            is FileChange.ModifyFile -> {
                true
            }
        }
    }
}
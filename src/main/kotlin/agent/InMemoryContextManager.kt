package agent

import agent.domain.context.ContextFile
import config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import utils.walkDirectory
import java.io.File
import java.nio.file.Path

class InMemoryContextManager : ContextManager {

    private val _files = MutableStateFlow<Set<ContextFile>>(setOf())
    val files: StateFlow<Set<ContextFile>> = _files.asStateFlow()

    // Replace direct messages list with MessageManager
    private val messageManager = MessageManager()

    init {
        // Load message history from file on initialization
        messageManager.loadFromFile()
    }

    override fun getContext(): StateFlow<Set<ContextFile>> {
        return files
    }

    override fun fetchMessages(): List<String> {
        // Convert Message objects to simple strings for backward compatibility
        return messageManager.getMessages().map { 
            when (it.type) {
                MessageType.USER -> "User: ${it.content}"
                MessageType.AGENT -> "Kompanion: ${it.content}"
            }
        }
    }

    override fun storeMessage(message: String) {
        // Determine message type and store in MessageManager
        if (message.startsWith("User: ")) {
            messageManager.addUserMessage(message.substringAfter("User: "))
        } else if (message.startsWith("Kompanion: ")) {
            messageManager.addAgentMessage(message.substringAfter("Kompanion: "))
        } else {
            // If no prefix, assume it's a user message
            messageManager.addUserMessage(message)
        }
    }

    // Add convenience methods for adding user and agent messages directly
    fun addUserMessage(content: String) {
        messageManager.addUserMessage(content)
    }

    fun addAgentMessage(content: String) {
        messageManager.addAgentMessage(content)
    }

    // Get formatted history for prompts
    fun getFormattedMessageHistory(): String {
        return messageManager.getFormattedHistory()
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
            walkDirectory(File(AppConfig.load().latestDirectory.trim()), this, File(AppConfig.load().latestDirectory).absolutePath)
        }
    }
}

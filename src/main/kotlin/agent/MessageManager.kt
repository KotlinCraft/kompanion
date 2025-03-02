package agent

import agent.fileops.KompanionFile
import agent.fileops.KompanionFileHandler

/**
 * Manages message history for a specific agent
 */
class MessageManager {
    private val messages = mutableListOf<Message>()

    /**
     * Add a user message to the history and persist it
     */
    fun addUserMessage(content: String) {
        val message = Message(MessageType.USER, content)
        messages.add(message)
        persistMessage(message)
    }

    /**
     * Add an agent message to the history and persist it
     */
    fun addAgentMessage(content: String) {
        val message = Message(MessageType.AGENT, content)
        messages.add(message)
        persistMessage(message)
    }

    /**
     * Get all messages in the history
     */
    fun getMessages(): List<Message> {
        return messages.toList()
    }

    /**
     * Get formatted message history for prompts
     */
    fun getFormattedHistory(): String {
        return messages.joinToString("\n") { 
            when (it.type) {
                MessageType.USER -> "User: ${it.content}"
                MessageType.AGENT -> "Kompanion: ${it.content}"
            }
        }
    }

    /**
     * Clear the message history
     */
    fun clear() {
        messages.clear()
    }

    private fun persistMessage(message: Message) {
        if (KompanionFileHandler.kompanionFolderExists()) {
            val formattedMessage = when (message.type) {
                MessageType.USER -> "User: ${message.content}"
                MessageType.AGENT -> "Kompanion: ${message.content}"
            }
            KompanionFileHandler.append(KompanionFile.MESSAGE_HISTORY.fileName, formattedMessage)
        }
    }

    /**
     * Load message history from file
     */
    fun loadFromFile() {
        if (KompanionFileHandler.kompanionFolderExists()) {
            val history = KompanionFileHandler.readFromKompanionDirectory(KompanionFile.MESSAGE_HISTORY.fileName)
            if (history.isNotEmpty()) {
                val lines = history.lines()
                lines.forEach { line ->
                    when {
                        line.startsWith("User: ") -> {
                            val content = line.substringAfter("User: ")
                            messages.add(Message(MessageType.USER, content))
                        }
                        line.startsWith("Kompanion: ") -> {
                            val content = line.substringAfter("Kompanion: ")
                            messages.add(Message(MessageType.AGENT, content))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Represents a message in the conversation
 */
data class Message(
    val type: MessageType,
    val content: String
)

/**
 * Type of message
 */
enum class MessageType {
    USER,
    AGENT
}

package ui.info

import config.AppConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Static manager for application information and notification messages.
 * Information messages are identified by a unique ID for later removal or replacement.
 */
object InfoManager {
    private val messages = ConcurrentHashMap<String, String>()

    /**
     * Add or update an information message with a specific ID
     * @param id Unique identifier for this information message
     * @param message The content of the information message
     * @return The InfoManager instance for method chaining
     */
    fun addInfo(id: String, message: String): InfoManager {
        messages[id] = message
        return this
    }

    /**
     * Remove an information message by ID
     * @param id The ID of the message to remove
     * @return The InfoManager instance for method chaining
     */
    fun removeInfo(id: String): InfoManager {
        messages.remove(id)
        return this
    }

    /**
     * Clear all information messages
     * @return The InfoManager instance for method chaining
     */
    fun clearAll(): InfoManager {
        messages.clear()
        return this
    }

    /**
     * Get all current information messages
     * @return Map of message IDs to message content
     */
    fun getInfoMessages(): Map<String, String> {
        return messages.toMap()
    }

    /**
     * Check if any information messages exist
     * @return True if there are any information messages
     */
    fun hasMessages(): Boolean {
        return messages.isNotEmpty()
    }

}
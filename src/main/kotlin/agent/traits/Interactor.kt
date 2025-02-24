package agent.traits

interface Interactor {
    suspend fun sendMessage(message: String)
    suspend fun askUser(question: String): String
    suspend fun confirmWithUser(message: String): Boolean
}
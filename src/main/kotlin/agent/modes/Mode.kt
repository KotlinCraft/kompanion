package agent.modes

interface Mode {
    suspend fun perform(request: String): String
    suspend fun getLoadedActionNames(): List<String>
}
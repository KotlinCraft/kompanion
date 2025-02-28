package agent.modes

interface Mode {
    suspend fun perform(request: String): String
    suspend fun onload() {}
    suspend fun getLoadedActionNames(): List<String>
}
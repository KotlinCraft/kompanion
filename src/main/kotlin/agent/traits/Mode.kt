package agent.traits

interface Mode {
    suspend fun perform(request: String): String
    suspend fun onload() {}
}
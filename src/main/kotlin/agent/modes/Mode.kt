package agent.modes

import agent.tool.LoadedTool

interface Mode {
    suspend fun perform(request: String): String
    suspend fun getLoadedTools(): List<LoadedTool>
    suspend fun disableAction(id: String)
    suspend fun enableAction(id: String)
}


package agent.tool

import agent.ToolManager

interface ToolsProvider {

    fun register(toolManager: ToolManager) {
        getTools().forEach(toolManager::registerTool)
    }

    fun getTools(): List<Tool>

}
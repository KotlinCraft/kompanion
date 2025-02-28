package agent.blockchain.tool

import agent.ToolManager
import agent.tool.Tool

interface ToolsProvider {

    fun register(toolManager: ToolManager) {
        getTools().forEach(toolManager::registerTool)
    }

    fun getTools(): List<Tool>

}
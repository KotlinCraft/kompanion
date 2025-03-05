package agent.tool

import agent.ToolManager
import org.springframework.ai.tool.ToolCallbacks

interface ToolsProvider {

    fun register(toolManager: ToolManager) {
        getTools().forEach(toolManager::registerTool)
    }

    fun getTools(): List<Tool> {
        return ToolCallbacks.from(this).map { Tool.from(it) }
    }
}
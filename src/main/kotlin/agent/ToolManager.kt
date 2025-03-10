package agent

import agent.tool.Tool

class ToolManager {
    val tools = mutableListOf<Tool>()

    fun registerTool(tool: Tool) {
        tools.add(tool)
    }
}
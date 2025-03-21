package agent

import agent.tool.Tool

class ToolManager {
    val tools = mutableSetOf<Tool>()

    fun registerTool(tool: Tool) {
        if (tools.none { it.toolCallback.toolDefinition.name() == tool.toolCallback.toolDefinition.name() }) {
            tools.add(tool)
        }
    }
}
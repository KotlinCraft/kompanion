package agent

import agent.tool.Tool
import org.springframework.ai.tool.ToolCallback

class ToolManager {
    val tools = mutableListOf<Tool>()
    val toolCallbacks = mutableListOf<ToolCallback>()

    fun registerTool(tool: Tool) {
        tools.add(tool)
    }

    fun registerCallbacks(callbacks: List<ToolCallback>) {
        this.toolCallbacks.addAll(callbacks)
    }
}
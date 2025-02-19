package ai

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.ai.tool.method.MethodToolCallback
import java.lang.reflect.Method

data class Action(
    val name: String,
    val description: String,
    val actionMethod: ActionMethod,
) {

    fun toFunctionCallback(): ToolCallback? {
        return MethodToolCallback.builder()
            .toolDefinition(
                ToolDefinition.builder(actionMethod.method)
                    .description(description)
                    .build()
            )
            .toolMethod(actionMethod.method)
            .toolObject(actionMethod.objectLocation)
            .build()
    }

    fun enrichPrompt(spec: ChatClient.ChatClientRequestSpec): ChatClient.ChatClientRequestSpec {
        return spec.tools(listOf(toFunctionCallback()))
    }
}

data class ActionMethod(
    val method: Method,
    val objectLocation: Any
)
package ai

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.model.function.FunctionCallback
import org.springframework.core.ParameterizedTypeReference
import java.util.function.Function

data class Action<I : Any, O>(
    val name: String,
    val description: String,
    val function: FunctionHandler<I, O>,
    val inputType: ParameterizedTypeReference<I> = object : ParameterizedTypeReference<I>() {}
) {

    val logger = LoggerFactory.getLogger(this::class.java)

    fun interface FunctionHandler<I, O> {
        fun run(input: I): O

        fun toFunction(): Function<I, O> {
            return Function { input -> run(input) }
        }
    }


    fun toFunctionCallback(): FunctionCallback? {
        return function?.toFunction()?.let {
            FunctionCallback.builder()
                .function(
                    name,
                    it,
                ).description(description).inputType(inputType).build()
        }
    }

    fun enrichPrompt(spec: ChatClient.ChatClientRequestSpec): ChatClient.ChatClientRequestSpec {
        return if (function == null) {
            logger.debug("not enriching prompt with action: {}", name)
            spec
        } else {
            spec.functions<I, O>(
                toFunctionCallback()
            )
        }
    }

    data class FunctionRequest(
        val request: String
    )

    data class FunctionResponse(
        val response: String
    )
}
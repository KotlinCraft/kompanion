package ai

import arrow.core.Either
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.util.JacksonUtils
import org.springframework.core.ParameterizedTypeReference

class AnthropicLLMProvider : LLMProvider() {
    val logger = LoggerFactory.getLogger(this::class.java)

    val objectmapper = jacksonObjectMapper().apply {
        registerModules(JacksonUtils.instantiateAvailableModules())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
    }

    var temperature = 1.0

    val client by lazy {
        createClient()
    }

    fun createModel(): AnthropicChatModel {
        val key = AppConfig.load().anthropicKey
        if (key.isBlank()) {
            throw IllegalArgumentException("anthropicKey is not set")
        }

        return AnthropicChatModel.builder()
            .anthropicApi(AnthropicApi(key))
            .defaultOptions(
                AnthropicChatOptions.builder()
                    .maxTokens(4000)
                    .model(model)
                    .temperature(temperature)
                    .build()
            ).build()
    }

    fun createClient(): ChatClient {
        return ChatClient.create(createModel())
    }

    override suspend fun <T> prompt(
        input: String,
        actions: List<Action>,
        temperature: Double,
        parameterizedTypeReference: ParameterizedTypeReference<T>,
        retry: Boolean
    ): T {
        this.temperature = temperature

        val converter = BeanOutputConverter(parameterizedTypeReference, objectmapper)

        val outputMessage = PromptTemplate(
            "{format}", mapOf("format" to converter.format)
        ).createMessage()

        var prompt = client.prompt(
            Prompt(
                UserMessage(input), outputMessage
            )
        )

        actions.forEach { action ->
            prompt = action.enrichPrompt(prompt)
        }

        val content = withContext(Dispatchers.IO) { prompt.call() }.content() ?: ""

        return Either.catch {
            converter.convert(content)
        }.fold({
            if (retry) {
                logger.info("response was: $content")
                logger.info("retrying, because we got the following error: $it")
                var prompt = client.prompt(
                    Prompt(
                        UserMessage(input),
                        UserMessage("we previously asked you this question as well, but when trying to parse your result, we got the following exception: ${it.message}. Please make sure this error doesn't happen again"),
                        outputMessage
                    )
                )

                actions.forEach { action ->
                    prompt = action.enrichPrompt(prompt)
                }

                val content = prompt.call().content() ?: ""
                converter.convert(content)
            } else {
                throw it
            }
        }, {
            it
        })
    }

    override fun getSupportedModels(): List<String> {
        return listOf(
            "claude-3-7-sonnet-latest",
            "claude-3-5-haiku-latest",
        )
    }
}
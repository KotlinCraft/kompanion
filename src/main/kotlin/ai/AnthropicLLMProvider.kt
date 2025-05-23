package ai

import agent.model.LLMException
import arrow.core.Either
import arrow.core.getOrElse
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
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.tool.ToolCallback
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
        return ChatClient.builder(createModel())
            .defaultAdvisors(defaultAdvisors.toList())
            .build()
    }

    override suspend fun <T> prompt(
        system: String,
        userMessage: String?,
        actions: List<ToolCallback>,
        temperature: Double,
        parameterizedTypeReference: ParameterizedTypeReference<T>?,
        retry: Boolean,
    ): T {
        this.temperature = temperature

        var converter: BeanOutputConverter<T>? = null
        var outputMessage: String? = null

        if (parameterizedTypeReference != null) {
            converter = BeanOutputConverter(parameterizedTypeReference, objectmapper)

            outputMessage =
                PromptTemplate(
                    "{format}", mapOf("format" to converter.format)
                ).createMessage().text
        }

        val messages = listOf(
            SystemMessage("Be sure to use your tools to provide the best possible answer."),
            SystemMessage(system),
            SystemMessage(outputMessage),
            userMessage?.let { UserMessage(it) },
        ).filterNotNull()

        val prompt = client.prompt(
            Prompt(
                messages
            )
        ).tools(actions)

        val content = Either.catch {
            withContext(Dispatchers.IO) { prompt.call() }.content() ?: ""
        }.mapLeft { logger.error("problem trying to call LLM", it) }.getOrElse { "" }

        return Either.catch {
            converter?.convert(content) ?: content as T
        }.fold({
            if (retry) {
                if (it.message?.contains("429") == true) {
                    throw LLMException("Rate limit exceeded. Make sure you're on a higher tier than Tier 1 when using anthropic.")
                }
                logger.info("retrying, because we got the following error: $it")
                val prompt = createClient().prompt(
                    Prompt(
                        UserMessage(system),
                        UserMessage("we previously asked you this question as well, but when trying to parse your result, we got the following exception: ${it.message}. Please make sure this error doesn't happen again"),
                        SystemMessage(outputMessage)
                    )
                ).tools(actions)

                val content = prompt.call().content() ?: ""
                converter?.convert(content) ?: content as T
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

class InvalidStructuredResponse : IllegalArgumentException()
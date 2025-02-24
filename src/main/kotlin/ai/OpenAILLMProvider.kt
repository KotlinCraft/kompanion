package ai

import arrow.core.Either
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.util.JacksonUtils
import org.springframework.core.ParameterizedTypeReference

class OpenAILLMProvider : LLMProvider {
    val logger = LoggerFactory.getLogger(this::class.java)

    val objectmapper = jacksonObjectMapper().apply {
        registerModules(JacksonUtils.instantiateAvailableModules())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
    }

    var temperature = 1.0
    var modelName = "gpt-4o"

    val client by lazy {
        createClient()
    }

    fun createModel(): OpenAiChatModel {
        val key = AppConfig.load().openAiKey
        if (key.isBlank()) {
            throw IllegalArgumentException("openAiKey is not set")
        }

        return OpenAiChatModel.builder().openAiApi(OpenAiApi.builder().apiKey(key).build()).defaultOptions(
                OpenAiChatOptions.builder().model(modelName).temperature(temperature).build()
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
            "gpt-4o-mini",
            "gpt-4o",
        )
    }
}
package ai

import arrow.core.Either
import arrow.core.getOrElse
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.InMemoryChatMemory
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.util.JacksonUtils
import org.springframework.core.ParameterizedTypeReference

class OpenAILLMProvider : LLMProvider() {
    val logger = LoggerFactory.getLogger(this::class.java)

    val objectmapper = jacksonObjectMapper().apply {
        registerModules(JacksonUtils.instantiateAvailableModules())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
    }


    val client by lazy {
        createClient()
    }

    fun createModel(): OpenAiChatModel {
        val key = AppConfig.load().openAiKey
        if (key.isBlank()) {
            throw IllegalArgumentException("openAiKey is not set")
        }

        return OpenAiChatModel.builder().openAiApi(OpenAiApi.builder().apiKey(key).build()).defaultOptions(
            OpenAiChatOptions.builder()
                .model(model).apply {
                    if (model == "o3-mini" || model == "o1") {
                        this.reasoningEffort("medium")
                    }
                }
                .temperature(1.0)
                .build()
        ).build()
    }


    fun createClient(): ChatClient {
        return ChatClient.builder(createModel())
            .defaultAdvisors(PromptChatMemoryAdvisor(InMemoryChatMemory()))
            .build()
    }

    override suspend fun <T> prompt(
        system: String,
        userMessage: String?,
        actions: List<ToolCallback>,
        temperature: Double,
        parameterizedTypeReference: ParameterizedTypeReference<T>,
        retry: Boolean
    ): T {

        val converter = BeanOutputConverter(parameterizedTypeReference, objectmapper)

        val outputMessage =
            PromptTemplate(
                "{format}", mapOf("format" to converter.format)
            ).createMessage().text


        val messages = listOf(
            SystemMessage("Be sure to use your tools to provide the best possible answer."),
            SystemMessage(system),
            SystemMessage(outputMessage),
            userMessage?.let { UserMessage(it) },
        ).filterNotNull()

        var prompt = client.prompt(
            Prompt(
                messages,
            )
        ).tools(actions)

        val content = Either.catch {
            withContext(Dispatchers.IO) { prompt.call() }.content() ?: ""
        }.mapLeft { logger.error("problem trying to call LLM", it) }.getOrElse { "" }

        return Either.catch {
            converter.convert(content)
        }.fold({
            if (retry) {
                logger.info("response was: $content")
                logger.info("retrying, because we got the following error: $it")
                val prompt = client.prompt(
                    Prompt(
                        UserMessage(system),
                        UserMessage("we previously asked you this question as well, but when trying to parse your result, we got the following exception: ${it.message}. Please make sure this error doesn't happen again"),
                        SystemMessage(outputMessage)
                    )
                ).tools(actions)

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
            "gpt-4o",
            "gpt-4o-mini",
            "o3-mini",
            "o1",
            "gpt-4.5-preview"
        )
    }
}
package agent.reason

import agent.ContextManager
import agent.InMemoryContextManager
import agent.ToolManager
import agent.domain.CodebaseQuestionResponse
import ai.LLMProvider
import org.springframework.core.ParameterizedTypeReference

class BlockchainReasoner(
    private val LLMProvider: LLMProvider,
    private val toolManager: ToolManager,
    private val contextManager: ContextManager
) {

    suspend fun askQuestion(
        question: String
    ): CodebaseQuestionResponse {
        val prompt = """
            You're a blockchain expert and EVM sleuth.
            Provide the best possible answer to a user's question by using your available tools and resources. 
            You are able to fetch contract source codes, fetch abis and read contracts. 
            
            By fetching a contract, you are able to understand what the smart contract is able to accomplish and can provide a more accurate answer. It also aids 
            you into navigating the code.
            Always fetch the abi if you need to read the contract state. Fetch the abi if the source code was too long.
            
            How to handle proxy contracts:
            - If the contract is a proxy contract, you can fetch the implementation contract by calling your get_proxy tool, fallback: the `implementation` method on the proxy. Alternatively, if that fails, try the `_implementation` function. 
            - you can then call the `get_contract_source` action with the implementation contract address to fetch the source code.
            - you can then read the contract state calling functions from the implementation contract on the proxy contract. This is important. You need to call it on the proxy, not the implementation.
            
            Not only tell the user how to do things, but navigate the contracts in order to provide the best possible answer.
            
            ${getMessageHistoryPrompt()}
            
            
            Your context already consists of:
            ${contextManager.currentContextPrompt(false)}
            
            Return your answer as a string. Use bullet points if it's a list of steps.
            If you had to navigate contracts, explain in bullet points how you got to your answer.
            If the question is not related to smart contracts, you don't have to fetch any contracts. 
        """.trimIndent()

        // Attempt to leverage the same LLM approach used in DefaultReasoner, if available
        return LLMProvider.prompt(
            system = prompt,
            userMessage = question,
            actions = toolManager.tools.map { it.action },
            temperature = 0.3,
            parameterizedTypeReference = object : ParameterizedTypeReference<CodebaseQuestionResponse>() {},
            toolcallbacks = mutableListOf()
        )
    }

    /**
     * Get formatted message history for prompts
     */
    private fun getMessageHistoryPrompt(): String {
        // If we have an InMemoryContextManager, use its formatted history
        val formattedHistory = (contextManager as? InMemoryContextManager)?.getFormattedMessageHistory()
            ?: buildMessageHistoryFromContext()

        return if (formattedHistory.isNotEmpty()) {
            """
            Previous conversation history (consider this as context for this continuation):
            $formattedHistory
            """
        } else {
            "" // Empty string if no history
        }
    }


    /**
     * Build message history from context if not using InMemoryContextManager
     */
    private fun buildMessageHistoryFromContext(): String {
        val messages = contextManager.fetchMessages()
        return if (messages.isNotEmpty()) {
            messages.joinToString("\n")
        } else {
            ""
        }
    }
}

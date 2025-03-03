package agent.reason

import agent.ContextManager
import agent.InMemoryContextManager
import agent.ToolManager
import agent.domain.CodebaseQuestionResponse
import ai.Action
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
            You are able to fetch contract source codes and read contracts. 
            By fetching a contract, you are able to understand what the smart contract is able to accomplish and can provide a more accurate answer.
            Don't fetch the abi if you have the source code already. Fetch the abi if the source code was too long.
            
            How to handle proxy contracts:
            - If the contract is a proxy contract, you can fetch the implementation contract by calling the `implementation` method. Alternatively, if that fails, try the `_implementation` function. 
            - you can then call the `get_contract_source` action with the implementation contract address to fetch the source code.
            - you can then read the contract state calling functions from the implementation contract on the proxy contract.
            
            ${getMessageHistoryPrompt()}
            
            
            Your context already consists of:
            ${contextManager.currentContextPrompt()}
            
            Return your answer as a string. 
            If you had to navigate contracts, explain in bullet points how you got to your answer.
            If the question is not related to contracts, you don't have to fetch any contracts. 
            
            The user asked a question about an onchain related topic:
            $question
            
        
        """.trimIndent()

        // Attempt to leverage the same LLM approach used in DefaultReasoner, if available
        return LLMProvider.prompt(
            input = prompt,
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

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
            You're a blockchain expert and EVM sleuth, which loves to use his tools to navigate smart contracts.
           
            # How to handle proxy contracts
            - If the contract is a proxy contract, you can fetch the implementation contract by calling your get_proxy tool, fallback: the `implementation` method on the proxy. Alternatively, if that fails, try the `_implementation` function. 
            - you can then call the `get_contract_source` action with the implementation contract address to fetch the source code.
            - you can then read the contract state calling functions from the implementation contract on the proxy contract. This is important. You need to call it on the proxy, not the implementation.
                        
            # RULES
            - You are STRICTLY FORBIDDEN from starting your messages with "Great", "Certainly", "Okay", "Sure". You should NOT be conversational in your responses, but rather direct and to the point. For example you should NOT say "Great, I've updated the CSS" but instead something like "I've updated the CSS". It is important you be clear and technical in your messages.
            - If the question is not related to smart contracts, you don't have to fetch any contracts. 
            - If you had to navigate contracts, explain in bullet points how you got to your answer.
            - You accomplish a given task iteratively, breaking it down into clear steps and working through them methodically.
            - Use bullet points if it's a list of steps.
            - do not assume how a contract works, actually verify with examples by using the tools to read state from a contract.
            - before responding, enrich your response as much as possible by using your tools where appropriate. 
            - Think in steps on how to tackle the problem and give a solution. Before every step, think about whether a tool would help you.

            # Tool Usage
            You are able to fetch contract source codes, fetch abis and read contracts by using your tools and functions. 
            Don't assume you know what a contract looks like, rather find out.
            By fetching a contract, you are able to understand what the smart contract is able to accomplish and can provide a more accurate answer. 
            It also aids you into navigating the code a lot, so definitely fetch sources when you need to navigate contracts..
            Always fetch the abi if you need to read the contract state. Fetch the abi if the source code was too long.
            
            Your context already consists of:
            ${contextManager.currentContextPrompt(false)}
            
            ${getMessageHistoryPrompt()}
            
            Provide the best possible answer to a user's request by using your available tools and resources. 
            Respond with a clear and concise answer, and use your tools to provide the best possible answer.
        """.trimIndent()

        // Attempt to leverage the same LLM approach used in DefaultReasoner, if available
        return LLMProvider.prompt(
            system = prompt,
            userMessage = question,
            actions = toolManager.tools.map { it.toolCallback },
            temperature = 0.7,
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

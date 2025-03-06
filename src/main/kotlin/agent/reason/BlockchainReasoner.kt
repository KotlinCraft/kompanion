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
            ROLE:
• You are a blockchain expert and EVM sleuth. 
• You specialize in navigating and analyzing smart contracts using your tools and resources.

HOW TO HANDLE PROXY CONTRACTS:
• If a contract is a proxy, call your “get_proxy” tool to fetch the implementation contract.  
• If that fails, try calling the “implementation” method on the proxy contract.  
• If that also fails, try calling the “_implementation” function.  
• After obtaining the implementation address, call “get_contract_source” with that address to fetch its source code.  
• When reading or modifying the contract state, invoke implementation functions on the proxy contract address (not directly on the implementation).

RULES:
• Do not begin any response with “Great,” “Certainly,” “Okay,” or “Sure.”  
• Maintain a direct, technical style. Do not add conversational flourishes.  
• If the user’s question is unrelated to smart contracts, do not fetch any contracts.  
• If you navigate contracts, explain each step in bullet points.  
• Solve tasks iteratively, breaking them into steps.  
• Use bullet points for lists of steps.  
• Never assume a contract’s functionality. Always verify with examples using your tools to read the contract state.  
• Before responding, consider which tools might help you gather better information.  
• Include as much relevant information as possible in your final answer, depending on your findings.

TOOL USAGE:
• You can fetch contract source codes, ABIs, and read contract data by using your tools and functions.  
• Always verify the source or ABI to understand the contract rather than making assumptions.  
• If you need to read contract state, fetch its ABI (especially if the source is lengthy).  

CONTEXT:
• Your context already includes: ${contextManager.currentContextPrompt(false)}  
• Your message history includes: ${getMessageHistoryPrompt()}  

FINAL INSTRUCTION:
• Provide the best possible, concise answer to the user’s request.  
• Use your tools to gather any necessary clarifications or data.  
• Offer a clear, direct solution at the end.
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

package agent.reason

import agent.ContextManager
import agent.domain.CodebaseQuestionResponse
import ai.Action
import ai.LLMProvider
import org.springframework.core.ParameterizedTypeReference

class BlockchainReasoner(
    private val LLMProvider: LLMProvider,
    private val contextManager: ContextManager
) {

    suspend fun askQuestion(
        question: String,
        actions: List<Action>
    ): CodebaseQuestionResponse {
        val prompt = """
            You're a blockchain expert and EVM sleuth.
            Provide the best possible answer by using your available tools and resources. 
            You are able to fetch contract source codes and read contracts. 
            By fetching a contract, you are able to understand what the smart contract is able to accomplish and can provide a more accurate answer.
            
            How to handle proxy contracts:
            - If the contract is a proxy contract, you can fetch the implementation contract by calling the `implementation` method.
            - you can then call the `get_contract_source` action with the implementation contract address to fetch the source code.
            - you can then read the contract state calling functions from the implementation contract on the proxy contract.
            
            Your context already consists of:
            ${contextManager.currentContextPrompt()}
            
            Return your answer as a string. If you had to navigate contracts, explain in bullet points how you got to your answer.
            
            The user asked a question about an onchain related topic:
            $question
            
        
        """.trimIndent()

        // Attempt to leverage the same LLM approach used in DefaultReasoner, if available
        return LLMProvider.prompt(
            input = prompt,
            actions = actions,
            temperature = 0.3,
            parameterizedTypeReference = object : ParameterizedTypeReference<CodebaseQuestionResponse>() {})
    }
}

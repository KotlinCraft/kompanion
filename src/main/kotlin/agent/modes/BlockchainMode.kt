package agent.modes

import agent.interaction.InteractionHandler
import agent.reason.Reasoner
import blockchain.etherscan.EtherscanClientManager
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

/**
 * Mode for blockchain-related operations.
 * Allows querying contract source code, ABIs, and other blockchain data.
 */
class BlockchainMode(
    private val reasoner: Reasoner,
    private val etherscanClientManager: EtherscanClientManager,
    private val interactionHandler: InteractionHandler
) : Mode, Interactor {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // Pattern to extract Ethereum addresses from text
    private val addressPattern = Pattern.compile("0x[a-fA-F0-9]{40}")

    override suspend fun perform(request: String): String {
        // First, analyze the request to understand what the user wants
        val understanding = reasoner.analyzeRequest(request)

        // Extract potential Ethereum addresses from the request
        val addresses = extractAddresses(request)

        // Parse the request to determine which operation to perform
        if (request.contains("source", ignoreCase = true) ||
            request.contains("abi", ignoreCase = true) ||
            request.contains("contract", ignoreCase = true)
        ) {

            // If we found addresses, try to get contract source
            if (addresses.isNotEmpty()) {
                return getContractSourceForAddresses(addresses, request)
            }

            // If no address found, ask the user for one
            val address = askForAddress()
            return if (address.isNotBlank()) {
                getContractSourceForAddresses(listOf(address), request)
            } else {
                "No valid Ethereum address provided. Please provide a contract address to fetch its source code."
            }
        }

        // Handle other blockchain-related requests
        return reasoner.askQuestion(request, understanding).reply
    }

    /**
     * Extracts Ethereum addresses from text
     */
    private fun extractAddresses(text: String): List<String> {
        val matcher = addressPattern.matcher(text)
        val addresses = mutableListOf<String>()

        while (matcher.find()) {
            addresses.add(matcher.group())
        }

        return addresses
    }

    /**
     * Asks the user to provide a contract address
     */
    private suspend fun askForAddress(): String {
        val response = askUser("Please provide a contract address to fetch its source code:")

        val addresses = extractAddresses(response)
        return addresses.firstOrNull() ?: ""
    }

    /**
     * Gets contract source code for a list of addresses
     */
    private suspend fun getContractSourceForAddresses(addresses: List<String>, request: String): String {
        // Determine which network to use (default to Ethereum)
        val networkName = when {
            request.contains("base", ignoreCase = true) -> "base"
            else -> "ethereum"
        }

        val client = etherscanClientManager.getClient(networkName)
        if (client == null) {
            return "Sorry, I don't have access to the $networkName network. Available networks: ${
                etherscanClientManager.getRegisteredNetworks().joinToString()
            }"
        }

        val results = StringBuilder()
        results.appendLine("Fetching contract data from $networkName network:")

        for (address in addresses) {
            sendMessage("Fetching contract source for address $address on $networkName...")

            val result = client.getContractSource(address)
            result.fold(
                ifLeft = { error ->
                    results.appendLine("- Error for $address: $error")
                },
                ifRight = { response ->
                    if (response.isSuccess() && response.result.isNotEmpty()) {
                        val contractData = response.result.first()
                        results.appendLine("- Contract: ${contractData.contractName}")
                        results.appendLine("- Compiler: ${contractData.compilerVersion}")

                        // Only include ABI if specifically requested
                        if (request.contains("abi", ignoreCase = true)) {
                            results.appendLine("- ABI: ${contractData.abi.take(100)}...")
                        }

                        // Only include source code if specifically requested
                        if (request.contains("source", ignoreCase = true)) {
                            val sourcePreview = if (contractData.sourceCode.length > 200) {
                                "${contractData.sourceCode.take(200)}..."
                            } else {
                                contractData.sourceCode
                            }
                            results.appendLine("- Source Code Preview: $sourcePreview")
                        }
                        results
                    } else {
                        results.appendLine("- No contract data found for $address")
                    }
                }
            )
        }

        return results.toString()
    }

    override fun interactionHandler(): InteractionHandler {
        return interactionHandler
    }
}

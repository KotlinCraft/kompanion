package blockchain.etherscan

import org.slf4j.LoggerFactory

/**
 * Manages multiple Etherscan API clients for different networks.
 */
class EtherscanClientManager {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val clients = mutableMapOf<String, EtherscanClient>()
    
    init {
        // Initialize with default clients
        initializeDefaultClients()
    }
    
    /**
     * Initializes default clients for common networks
     */
    private fun initializeDefaultClients() {
        logger.info("Initializing default Etherscan clients")
        
        // Note: in production, these API keys should be read from secure configuration
        // This is just for demonstration
        val placeholderApiKey = "YourApiKeyToken"
        
        registerClient(
            networkName = "ethereum",
            client = EtherscanClient(
                baseUrl = "https://api.etherscan.io/api",
                apiKey = placeholderApiKey
            )
        )
        
        registerClient(
            networkName = "base",
            client = EtherscanClient(
                baseUrl = "https://api.basescan.org/api",
                apiKey = placeholderApiKey
            )
        )
    }
    
    /**
     * Registers a new Etherscan client for a specific network
     * 
     * @param networkName The name of the network (e.g., "ethereum", "base", etc.)
     * @param client The Etherscan client instance
     */
    fun registerClient(networkName: String, client: EtherscanClient) {
        clients[networkName.lowercase()] = client
        logger.info("Registered Etherscan client for network: $networkName")
    }
    
    /**
     * Gets an Etherscan client for a specific network
     * 
     * @param networkName The name of the network
     * @return The corresponding Etherscan client or null if not found
     */
    fun getClient(networkName: String): EtherscanClient? {
        return clients[networkName.lowercase()].also {
            if (it == null) {
                logger.warn("No Etherscan client found for network: $networkName")
            }
        }
    }
    
    /**
     * Gets all registered network names
     * 
     * @return List of registered network names
     */
    fun getRegisteredNetworks(): List<String> {
        return clients.keys.toList()
    }
}
package blockchain.etherscan

import config.AppConfig
import org.slf4j.LoggerFactory

/**
 * Manages multiple Etherscan API clients for different networks.
 *
 * @property appConfig Application configuration that contains API keys
 */
class EtherscanClientManager {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val clients = mutableMapOf<String, EtherscanClient>()

    init {
        // Initialize with default clients
        initializeDefaultClients()
    }

    /**
     * Initializes default clients for common networks using API keys from AppConfig
     */
    private fun initializeDefaultClients() {
        logger.info("Initializing default Etherscan clients")

        // Create Ethereum client
        val ethereumApiKey = AppConfig.load().etherscan.ethereumApiKey
        registerClient(
            networkName = "ethereum",
            client = EtherscanClient(
                baseUrl = "https://api.etherscan.io/api",
                apiKey = ethereumApiKey,
                networkType = "ethereum"
            )
        )

        // Create Base client
        val baseApiKey = AppConfig.load().etherscan.baseApiKey
        registerClient(
            networkName = "base",
            client = EtherscanClient(
                baseUrl = "https://api.basescan.org/api",
                apiKey = baseApiKey,
                networkType = "base"
            )
        )
    }

    /**
     * Refreshes clients with updated API keys from AppConfig
     */
    fun refreshClients(newAppConfig: AppConfig) {
        // Update Ethereum client
        getClient("ethereum")?.let {
            registerClient(
                networkName = "ethereum",
                client = EtherscanClient(
                    baseUrl = "https://api.etherscan.io/api",
                    apiKey = newAppConfig.etherscan.ethereumApiKey,
                    networkType = "ethereum"
                )
            )
        }

        // Update Base client
        getClient("base")?.let {
            registerClient(
                networkName = "base",
                client = EtherscanClient(
                    baseUrl = "https://api.basescan.org/api",
                    apiKey = newAppConfig.etherscan.baseApiKey,
                    networkType = "base"
                )
            )
        }
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
            } else if (!it.hasValidApiKey()) {
                logger.warn("Etherscan client for network $networkName has no API key")
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

    /**
     * Checks if the client for the specified network has a valid API key
     *
     * @param networkName The name of the network
     * @return True if the client exists and has a valid API key, false otherwise
     */
    fun hasValidApiKey(networkName: String): Boolean {
        return getClient(networkName)?.hasValidApiKey() ?: false
    }
}
package blockchain.etherscan

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.slf4j.LoggerFactory

/**
 * Client for interacting with Etherscan-compatible APIs.
 * 
 * @property baseUrl The base URL for the Etherscan API (e.g., "https://api.etherscan.io/api")
 * @property apiKey API key for accessing the Etherscan API
 * @property networkType The type of network this client is for (e.g., "ethereum", "base")
 */
class EtherscanClient(
    private val baseUrl: String,
    private val apiKey: String,
    val networkType: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val httpClient = HttpClient.newBuilder().build()
    
    /**
     * Fetches contract source code for a given contract address.
     * 
     * @param contractAddress The contract address to fetch the source code for
     * @return Either an error message or the contract source data
     */
    suspend fun getContractSource(contractAddress: String): Either<String, ContractSourceResponse> {
        val endpoint = "$baseUrl?module=contract&action=getsourcecode&address=$contractAddress&apikey=$apiKey"
        
        return try {
            val response = makeRequest(endpoint)
            
            // Parse the JSON response
            Either.catch {
                // Simplified example - in a real app, use a proper JSON parser like Jackson
                ContractSourceResponse.fromJson(response)
            }.mapLeft { error ->
                logger.error("Error parsing contract source response: ${error.message}", error)
                "Failed to parse contract source data: ${error.message}"
            }
        } catch (e: Exception) {
            logger.error("Error fetching contract source: ${e.message}", e)
            "Failed to fetch contract source: ${e.message}".left()
        }
    }
    
    /**
     * Makes an HTTP request to the specified endpoint.
     * 
     * @param endpoint The complete URL to request
     * @return The response body as a string
     */
    private suspend fun makeRequest(endpoint: String): String {
        return withContext(Dispatchers.IO) {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .GET()
                .build()
                
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() != 200) {
                throw RuntimeException("API request failed with status code: ${response.statusCode()}")
            }
            
            response.body()
        }
    }
    
    /**
     * Check if the client has a valid API key.
     * 
     * @return True if the API key is not empty, false otherwise
     */
    fun hasValidApiKey(): Boolean {
        return apiKey.isNotBlank()
    }
}
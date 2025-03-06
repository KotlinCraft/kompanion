package agent.blockchain.bankless

import agent.blockchain.bankless.model.contract.Input
import agent.blockchain.bankless.model.contract.Output
import agent.blockchain.bankless.model.event.EthLog
import agent.blockchain.bankless.model.event.GetEventLogsRequest
import agent.blockchain.bankless.model.proxy.Proxy
import agent.blockchain.bankless.model.token.FungibleTokenVO
import arrow.core.Either
import arrow.core.left
import com.bankless.claimable.rest.vo.ClaimableVO
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Client for interacting with Bankless API.
 *
 * @property baseUrl The base URL for the Bankless API
 */
class BanklessClient {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val httpClient = HttpClient.newBuilder().build()
    private val objectMapper = ObjectMapper().registerKotlinModule().also {
        it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private val baseUrl = "https://api.bankless.com/internal/chains"
    private val claimablesBaseUrl = "https://api.bankless.com/claimables"


    /**
     * Reads contract state for a given network and contract.
     *
     * @param network The blockchain network (e.g., "ethereum", "base")
     * @param request The contract state read request containing address, method, and parameters
     * @return Either an error message or the contract state response
     */
    suspend fun readContractState(
        network: String,
        request: EvmReadContractStateRequest
    ): Either<String, List<EthCallResultToTypeConverter.Result>> {
        val endpoint = "$baseUrl/$network/contract/read"

        return try {
            val response = makePostRequest(endpoint, request)

            // Parse the JSON response
            Either.catch {
                objectMapper.readValue<List<EthCallResultToTypeConverter.Result>>(response)
            }.mapLeft { error ->
                "Failed to parse contract state data: ${error.message}"
            }
        } catch (e: Exception) {
            logger.error("Error reading contract state: ${e.message}", e)
            logger.info("request was ${objectMapper.writeValueAsString(request)}")
            "Failed to read contract state: ${e.message}".left()
        }
    }

    /**
     * Gets the proxy address for a given network and contract.
     *
     * @param network The blockchain network (e.g., "ethereum", "base")
     * @param contract The contract address
     * @return Either an error message or the proxy information
     */
    suspend fun getProxy(network: String, contract: String): Either<String, Proxy> {
        val endpoint = "$baseUrl/$network/contract/$contract/find-proxy"

        return try {
            val response = makeGetRequest(endpoint)

            // Parse the JSON response
            Either.catch {
                objectMapper.readValue<Proxy>(response)
            }.mapLeft { error ->
                logger.error("Error parsing proxy response: ${error.message}", error)
                "Failed to parse proxy data: ${error.message}"
            }
        } catch (e: Exception) {
            logger.error("Error finding proxy: ${e.message}", e)
            "Failed to find proxy: ${e.message}".left()
        }
    }

    /**
     * Fetches the token information for a given address and chain.
     *
     * @param chain The blockchain network (e.g., "ethereum", "base")
     * @param address The token address
     * @return Either an error message or the token information
     */
    suspend fun fetchTokenInformation(chain: String, address: String): Either<String, FungibleTokenVO> {
        val endpoint = "https://api.bankless.com/internal/token/$chain/$address/token"

        return try {
            val response = makeGetRequest(endpoint)

            // Parse the JSON response
            Either.catch {
                objectMapper.readValue<FungibleTokenVO>(response)
            }.mapLeft { error ->
                logger.error("Error parsing token information response: ${error.message}", error)
                "Failed to parse token information data: ${error.message}"
            }
        } catch (e: Exception) {
            logger.error("Error fetching token information: ${e.message}", e)
            "Failed to fetch token information: ${e.message}".left()
        }
    }

    /**
     * Fetches event logs for a given network and filter criteria.
     *
     * @param network The blockchain network (e.g., "ethereum", "base")
     * @param request The request containing addresses and topics to filter event logs
     * @return Either an error message or the event logs response
     */
    suspend fun getEvents(network: String, request: GetEventLogsRequest): Either<String, EthLog> {
        val endpoint = "$baseUrl/$network/events/logs"

        return try {
            val response = makePostRequest(endpoint, request)

            // Parse the JSON response
            Either.catch {
                objectMapper.readValue<EthLog>(response)
            }.mapLeft { error ->
                logger.error("Error parsing event logs response: ${error.message}", error)
                "Failed to parse event logs data: ${error.message}"
            }
        } catch (e: Exception) {
            logger.error("Error fetching event logs: ${e.message}", e)
            "Failed to fetch event logs: ${e.message}".left()
        }
    }

    /**
     * Makes an HTTP GET request to the specified endpoint.
     *
     * @param endpoint The complete URL to request
     * @return The response body as a string
     */
    private suspend fun makeGetRequest(endpoint: String): String {
        return withContext(Dispatchers.IO) {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("X-BANKLESS-TOKEN", AppConfig.load().banklessToken ?: "")
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
     * Fetches all claimables for a given wallet address.
     *
     * @param address The wallet address to fetch claimables for
     * @return Either an error message or a list of ClaimableVO objects
     */
    suspend fun getClaimables(address: String): Either<String, List<ClaimableVO>> {
        val endpoint = "$claimablesBaseUrl/$address"

        return try {
            val response = makeGetRequest(endpoint)

            // Parse the JSON response
            Either.catch {
                objectMapper.readValue<List<ClaimableVO>>(response)
            }.mapLeft { error ->
                logger.error("Error parsing claimables response: ${error.message}", error)
                "Failed to parse claimables data: ${error.message}"
            }
        } catch (e: Exception) {
            logger.error("Error fetching claimables: ${e.message}", e)
            "Failed to fetch claimables: ${e.message}".left()
        }
    }

    /**
     * Makes an HTTP POST request to the specified endpoint.
     *
     * @param endpoint The complete URL to request
     * @param requestBody The body to send with the POST request
     * @return The response body as a string
     */
    private suspend fun makePostRequest(endpoint: String, requestBody: Any): String {
        return withContext(Dispatchers.IO) {
            val jsonBody = objectMapper.writeValueAsString(requestBody)
            println(jsonBody)
            val request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("X-BANKLESS-TOKEN", AppConfig.load().banklessToken ?: "")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                throw RuntimeException("API request failed with status code: ${response.statusCode()}")
            }

            response.body()
        }
    }
}

/**
 * Request model for reading EVM contract state.
 */
data class EvmReadContractStateRequest(
    val contract: String,
    val method: String,
    val inputs: List<Input>,
    val outputs: List<Output>
)

/**
 * Converter for Ethereum call results.
 */
class EthCallResultToTypeConverter {
    /**
     * Result of an Ethereum contract state read.
     */
    data class Result(
        val value: Any,
        val type: String,
        val error: String?
    )
}

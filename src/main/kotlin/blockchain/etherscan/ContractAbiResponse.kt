package blockchain.etherscan

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Represents a response from the Etherscan API for contract ABI.
 * 
 * @property status API response status ("1" for success, "0" for failure)
 * @property message Status message from the API
 * @property result The ABI JSON string
 */
data class ContractAbiResponse(
    val status: String,
    val message: String,
    val result: String
) {
    companion object {
        private val mapper: ObjectMapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
        
        /**
         * Parse JSON response into ContractAbiResponse object
         */
        fun fromJson(json: String): ContractAbiResponse {
            return mapper.readValue(json)
        }
    }
    
    /**
     * Check if the API call was successful
     */
    fun isSuccess(): Boolean = status == "1"
}
package blockchain.etherscan

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Represents a response from the Etherscan API for contract source code.
 * 
 * @property status API response status ("1" for success, "0" for failure)
 * @property message Status message from the API
 * @property result List of contract source results
 */
data class ContractSourceResponse(
    val status: String,
    val message: String,
    val result: List<ContractSourceResult>
) {
    companion object {
        private val mapper: ObjectMapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
        
        /**
         * Parse JSON response into ContractSourceResponse object
         */
        fun fromJson(json: String): ContractSourceResponse {
            return mapper.readValue(json)
        }
    }
    
    /**
     * Check if the API call was successful
     */
    fun isSuccess(): Boolean = status == "1"
}

/**
 * Represents the contract source code details from the Etherscan API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ContractSourceResult(
    @JsonProperty("SourceCode")
    val sourceCode: String,
    
    @JsonProperty("ABI")
    val abi: String,
    
    @JsonProperty("ContractName")
    val contractName: String,
    
    @JsonProperty("CompilerVersion")
    val compilerVersion: String,
    
    @JsonProperty("OptimizationUsed")
    val optimizationUsed: String,
    
    @JsonProperty("Runs")
    val runs: String,
    
    @JsonProperty("ConstructorArguments")
    val constructorArguments: String,
    
    @JsonProperty("EVMVersion")
    val evmVersion: String,
    
    @JsonProperty("Library")
    val library: String,
    
    @JsonProperty("LicenseType")
    val licenseType: String,
    
    @JsonProperty("Proxy")
    val proxy: String,
    
    @JsonProperty("Implementation")
    val implementation: String,
    
    @JsonProperty("SwarmSource")
    val swarmSource: String
)
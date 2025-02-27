package agent.blockchain.bankless.model.input

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonSubTypes(
    JsonSubTypes.Type(value = Uint256::class, name = "uint256"),
    JsonSubTypes.Type(value = Address::class, name = "address"),
    JsonSubTypes.Type(value = InputString::class, name = "string"),
    JsonSubTypes.Type(value = InputString::class, name = "string"),
    JsonSubTypes.Type(value = Bytes32::class, name = "bytes32"),
    JsonSubTypes.Type(value = Bytes4::class, name = "bytes4"),
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, visible = true, property = "type")
abstract class Input<T>(
    val type: String,
    val value: T
)


package agent.blockchain.bankless.model.output

import com.fasterxml.jackson.annotation.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, visible = true, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Uint256::class, name = "uint256"),
    JsonSubTypes.Type(value = Bool::class, name = "bool"),
    JsonSubTypes.Type(value = OutputString::class, name = "string"),
    JsonSubTypes.Type(value = Bytes4::class, name = "bytes4"),
)
abstract class Output<T>(
    val type: String
)


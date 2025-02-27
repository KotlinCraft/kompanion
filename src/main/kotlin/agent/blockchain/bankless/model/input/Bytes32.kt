package agent.blockchain.bankless.model.input

class Bytes32(
    type: String,
    value: ByteArray
) : Input<ByteArray>(type, value)
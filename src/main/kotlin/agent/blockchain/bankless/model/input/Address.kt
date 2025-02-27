package agent.blockchain.bankless.model.input


class Address(
    type: String,
    value: String
) : Input<String>(type, value)
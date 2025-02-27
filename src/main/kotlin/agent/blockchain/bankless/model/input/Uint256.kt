package agent.blockchain.bankless.model.input

import java.math.BigInteger

class Uint256(
    type: String,
    value: BigInteger
) : Input<BigInteger>(type, value)
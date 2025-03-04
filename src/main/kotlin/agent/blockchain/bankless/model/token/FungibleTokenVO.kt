package agent.blockchain.bankless.model.token

class FungibleTokenVO(
    val network: String,
    val name: String,
    val symbol: String,
    val address: String,
    val decimals: Int,
    val underlyingTokens: List<FungibleTokenVO> = emptyList(),
    val verified: Boolean
)
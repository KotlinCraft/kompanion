package com.bankless.claimable.rest.vo

import agent.blockchain.bankless.model.claimable.ActionVO
import agent.blockchain.bankless.model.claimable.SupplierVO
import agent.blockchain.bankless.model.claimable.WorthVO
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class ClaimableVO(
    val source: String,
    val type: String,
    val title: String,
    val action: ActionVO,
    val claimStatus: String,
    val description: String,
    val walletAddress: String,
    val supplier: SupplierVO,
    val imageUrl: String,
    val expires: Long?,
    val tokenAmount: BigDecimal?,
    val worth: WorthVO?,
    val created: Long?,
    @JsonProperty("is_premium")
    val premium: Boolean,
)





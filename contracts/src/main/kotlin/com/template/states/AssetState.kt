package com.template.states

import net.corda.core.identity.Party
import com.template.contracts.AssetContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState

// *********
// * State *
// *********
@BelongsToContract(AssetContract::class)
class AssetState(val transferValue : Int,
                 val currentOwner : Party,
                 val newOwner : Party) : ContractState {
    override val participants get() = listOf(currentOwner, newOwner)
}


//data class AssetState(val data: String, override val participants: List<AbstractParty> = listOf()) : ContractState
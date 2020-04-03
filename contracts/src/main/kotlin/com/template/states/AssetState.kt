package com.template.states

import com.template.contracts.AssetContract
import com.template.schema.AssetSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

// *********
// * State *
// *********
@BelongsToContract(AssetContract::class)

class AssetState(val transferValue : Int,
                 val currentOwner : Party,
                 val newOwner : Party,
                 override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {
    override val participants get() = listOf(currentOwner, newOwner)
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is AssetSchemaV1 -> AssetSchemaV1.PersistentAsset(
                    this.currentOwner.name.toString(),
                    this.newOwner.name.toString(),
                    this.transferValue,
                    this.linearId.id)
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(AssetSchemaV1)

}


//data class AssetState(val data: String, override val participants: List<AbstractParty> = listOf()) : ContractState
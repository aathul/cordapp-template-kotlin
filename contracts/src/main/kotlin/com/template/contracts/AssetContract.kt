package com.template.contracts

import com.template.states.AssetState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
// ************
// * Contract *
// ************
class AssetContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.AssetContract"
    }

    class Create : CommandData

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Create>()

        requireThat {
            // Constraints on the shape of the transaction.
            "No inputs should be consumed when issuing an Asset." using (tx.inputs.isEmpty())
            "There should be only one output state." using (tx.outputs.size == 1)

            // ASSET-specific constraints.
            val output = tx.outputsOfType<AssetState>().single()
            "The Asset transfer value must be non-negative." using (output.transferValue > 0)
            "The currentowner and the newowner cannot be the same entity." using (output.newOwner != output.currentOwner)

            // Constraints on the signers.
            val expectedSigners = listOf(output.currentOwner.owningKey, output.newOwner.owningKey)
            "There must be two signers." using (command.signers.toSet().size == 2)
            "The currentowner and newowner must be signers." using (command.signers.containsAll(expectedSigners))
        }
    }
}
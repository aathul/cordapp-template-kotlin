package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.AssetContract
import com.template.states.AssetState
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.SignedTransaction


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class AssetFlow(val transferValue : Int,
                val newOwner : Party,
                val attachmentHash: SecureHash) : FlowLogic<SignedTransaction>() {

    /** The flow logic is encapsulated within the call() method. */
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        /** create the transaction components. **/
        val outputState = AssetState(transferValue, ourIdentity, newOwner)
        val command = Command(AssetContract.Create(), listOf(ourIdentity.owningKey, newOwner.owningKey))

        /**  create a transaction builder and add the components.  **/
        val txBuilder = TransactionBuilder(notary = notary)
                .addOutputState(outputState, AssetContract.ID)
                .addCommand(command)
                .addAttachment(attachmentHash)

        /**    Verifying the transaction**/
        txBuilder.verify(serviceHub)

        /**     Signing the transaction.**/
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        /**      Creating a session with the other party.**/
        val otherPartySession = initiateFlow(newOwner)

        /**      Obtaining the counterparty's signature. **/
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(otherPartySession)))

        /**      Finalising the transaction.. **/
        return subFlow(FinalityFlow(fullySignedTx, otherPartySession))


    }

}

@InitiatedBy(AssetFlow::class)

class AssetFlowResponder(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {

            override fun checkTransaction(stx: SignedTransaction) = requireThat {

                val output = stx.tx.outputs.single().data

                "This must be an Asset transaction." using (output is AssetState)

                val assetValue = output as AssetState

                "The Asset transfer value must be greater than zero." using (assetValue.transferValue > 0)

            }

        }

        val expectedTxId = subFlow(signTransactionFlow).id

        subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId))

    }
}
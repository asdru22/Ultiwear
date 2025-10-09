package com.aln.ultiwear.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aln.ultiwear.model.PendingTrade
import com.aln.ultiwear.model.TradeSessionState
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TradeSessionViewModel(
    private val wardrobeViewModel: WardrobeViewModel,
    private val manualTradeViewModel: ManualTradeViewModel
) : ViewModel() {

    private val tag = "TradeSessionViewModel"

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val _sessionState = MutableStateFlow<TradeSessionState?>(null)
    val sessionState = _sessionState.asStateFlow()

    private var sessionListener: ListenerRegistration? = null


    suspend fun createTradeSession(): String {
        // create session id
        val sessionId = UUID.randomUUID().toString()
        // get current user
        val currentUser = auth.currentUser
            ?: throw Exception("User not logged in")

        // define session data
        val sessionData = mapOf(
            "createdBy" to currentUser.uid,
            "participants" to listOf(currentUser.uid),
            "createdAt" to FieldValue.serverTimestamp(),
            "isReady" to false
        )

        // create the document in firebase
        firestore.collection("trade_sessions")
            .document(sessionId)
            .set(sessionData)
            .await()

        listenToSessionUpdates(sessionId)
        return sessionId
    }

    suspend fun joinTradeSession(sessionId: String) {
        val currentUser = auth.currentUser
            ?: throw Exception("User not logged in")
        val sessionRef = firestore.collection("trade_sessions")
            .document(sessionId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(sessionRef)
            if (!snapshot.exists()) throw Exception("Session does not exist")

            val participants = snapshot.get("participants") as? List<*>
                ?: emptyList<String>()
            val updatedParticipants = if (participants.contains(currentUser.uid)) {
                participants
            } else {
                participants + currentUser.uid
            }

            val isReady = updatedParticipants.size >= 2
            transaction.update(
                sessionRef, mapOf(
                    "participants" to updatedParticipants,
                    "isReady" to isReady
                )
            )
        }.await()

        listenToSessionUpdates(sessionId)
    }

    private fun listenToSessionUpdates(sessionId: String) {
        // remove any previous listener to avoid duplicate uploads
        sessionListener?.remove()

        // add a new listener to listen to session updates
        sessionListener = firestore.collection("trade_sessions")
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // if there is an error, exit the callback
                    // prevents using invalid snapshots
                    Log.e(tag, "Error listening to session", error)
                    return@addSnapshotListener // only exit the lambda, "scoped" return
                }

                // if the session document no longer exists
                // set the session state to null,
                // signaling the UI to return to the default screen
                if (snapshot == null || !snapshot.exists()) {
                    // if the session is deleted, remove the state
                    _sessionState.value = null
                    return@addSnapshotListener
                }

                // get the key-value map of the document’s current data
                val data = snapshot.data ?: return@addSnapshotListener

                // parse session fields
                val participants =
                    (data["participants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                // get the ready field, false by default
                val isReady = data["isReady"] as? Boolean ?: false

                // update the internal state
                _sessionState.value = TradeSessionState(
                    sessionId = snapshot.id,
                    participants = participants,
                    isReady = isReady
                )
            }
    }

    suspend fun sendItemToOtherUser(sessionId: String, itemId: String) {
        val currentUser = auth.currentUser
            ?: throw Exception("User not logged in")

        // get reference to the trade session
        val sessionRef = firestore
            .collection("trade_sessions")
            .document(sessionId)

        val snapshot = sessionRef.get().await()
        val participants = (snapshot.get("participants") as? List<*>)
            ?.mapNotNull { it as? String } ?: emptyList()

        // determine the other participant
        val otherUser = participants.firstOrNull { it != currentUser.uid }
            ?: throw Exception("No recipient")

        // fetch the item to send from the current users wardrobe
        val item = firestore.collection("wardrobe")
            .document(itemId)
            .get()
            .await()
            .toObject(WardrobeItem::class.java)
            ?: throw Exception("Item not found")

        // create a pendingTrade
        val trade = PendingTrade(
            fromUser = currentUser.uid,
            toUser = otherUser,
            itemId = item.id,
            frontImageUrl = item.frontImageUrl,
            confirmedBySender = false,
            confirmedByReceiver = false,
            timestamp = Timestamp.now()
        )

        // add the pending trade to Firestore
        sessionRef.collection("pending_trades").add(trade).await()
    }

    suspend fun finalizeTrade(sessionId: String) {
        // get the current user, trade session, and all pending trades
        val currentUser = auth.currentUser
            ?: throw Exception("User not logged in")
        val sessionRef = firestore
            .collection("trade_sessions")
            .document(sessionId)
        val pendingRef = sessionRef.collection("pending_trades")

        // get the documents for this session
        val snapshot = pendingRef.get().await()

        // for each pending trade
        for (doc in snapshot.documents) {
            // convert the firestore into a PendingTrade data class
            val trade = doc.toObject(PendingTrade::class.java) ?: continue
            val updates = mutableMapOf<String, Any>()

            // confirm trade based on user
            if (currentUser.uid == trade.fromUser) updates["confirmedBySender"] = true
            if (currentUser.uid == trade.toUser) updates["confirmedByReceiver"] = true
            // update firestore with confirmation
            doc.reference.update(updates).await()
        }

        // fetch again
        val updatedSnapshot = pendingRef.get().await()
        val finalizedTrades = mutableListOf<PendingTrade>()
        for (doc in updatedSnapshot.documents) {
            val trade = doc.toObject(PendingTrade::class.java) ?: continue
            // get trades that are confirmed by both users
            if (trade.confirmedBySender && trade.confirmedByReceiver) {
                // add them to the finalized trades
                finalizedTrades.add(trade)
                val itemSnapshot = firestore
                    .collection("wardrobe")
                    .document(trade.itemId)
                    .get()
                    .await()

                // convert the document into a WardrobeItem
                val item = itemSnapshot.toObject(WardrobeItem::class.java)
                    ?: continue

                // hide the item that was sent
                wardrobeViewModel.hideItem(item.id)

                // create a copy
                val newItem = item.copy(
                    id = firestore.collection("wardrobe").document().id,
                    owner = trade.toUser,
                    owned = true,
                    posted = false
                )

                firestore.collection("wardrobe")
                    .document(newItem.id)
                    .set(newItem)
                    .await()


                Log.d(tag, "Created new item ${newItem.id} for ${trade.toUser}")

                // delete the pending trade document
                doc.reference.delete().await()
            }
        }

        // check if the trade record should be uploaded
        val sessionSnapshot = sessionRef.get().await()
        val tradeAlreadyUploaded = sessionSnapshot.getBoolean("tradeUploaded")
            ?: false

        // only upload if theres at least one finalized trade and it hasn't been uploaded yet
        if (!tradeAlreadyUploaded && finalizedTrades.isNotEmpty()) {
            // combine all users from all trades in one single list
            // to ensure one user that created the qrcode
            // one that scanned it
            val participants = finalizedTrades.flatMap {
                listOf(it.fromUser, it.toUser)
            }.distinct()

            // ensure trade between 2 users
            if (participants.size == 2) {

                val userAId = participants[0]
                val userBId = participants[1]

                // get all items sent by userA in this session
                val userAItems = finalizedTrades.filter {
                    it.fromUser == userAId
                }.map { it.itemId }

                // get all items sent by userB in this session
                val userBItems = finalizedTrades.filter {
                    it.fromUser == userBId
                }.map { it.itemId }

                // upload the trade
                manualTradeViewModel.uploadTrade(
                    userAId = userAId,
                    userBId = userBId,
                    userAItems = userAItems,
                    userBItems = userBItems,
                    photoUri = null
                )

                // mark session as uploaded
                sessionRef.update("tradeUploaded", true).await()
            }

            delay(1000)
            // delete session and clear UI state
            sessionRef.delete().await()
            _sessionState.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionListener?.remove()
    }
}


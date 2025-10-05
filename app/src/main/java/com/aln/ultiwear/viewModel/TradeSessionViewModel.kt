package com.aln.ultiwear.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aln.ultiwear.model.TradeSessionState
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class QuickTradeViewModel : ViewModel() {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val _sessionState = MutableStateFlow<TradeSessionState?>(null)
    val sessionState = _sessionState.asStateFlow()

    private var sessionListener: ListenerRegistration? = null


    suspend fun createTradeSession(): String {
        val sessionId = UUID.randomUUID().toString()
        val currentUser = auth.currentUser
            ?: throw Exception("User not logged in")

        val sessionData = mapOf(
            "createdBy" to currentUser.uid,
            "participants" to listOf(currentUser.uid),
            "createdAt" to FieldValue.serverTimestamp(),
            "isReady" to false
        )

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
        // remove any previous listener
        sessionListener?.remove()

        sessionListener = firestore.collection("trade_sessions")
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("QuickTrade", "Error listening to session", error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    // if the session is deleted, remove the state
                    _sessionState.value = null
                    return@addSnapshotListener
                }

                val data = snapshot.data ?: return@addSnapshotListener
                val participants =
                    (data["participants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val isReady = data["isReady"] as? Boolean ?: false

                _sessionState.value = TradeSessionState(
                    sessionId = snapshot.id,
                    participants = participants,
                    isReady = isReady
                )
            }
    }

    suspend fun sendItemToOtherUser(sessionId: String, itemId: String) {
        val currentUser = Firebase.auth.currentUser
            ?: throw Exception("User not logged in")
        val sessionRef = Firebase.firestore
            .collection("trade_sessions")
            .document(sessionId)
        val participants =
            sessionRef.get().await().get("participants") as? List<*> ?: emptyList<String>()
        val otherUser =
            participants.firstOrNull { it != currentUser.uid }
                ?: throw Exception("No recipient")

        val item = Firebase.firestore.collection("wardrobe")
            .document(itemId)
            .get()
            .await()
            .toObject(WardrobeItem::class.java)
            ?: throw Exception("Item not found")

        // add to pending trades
        val tradeData = mapOf(
            "fromUser" to currentUser.uid,
            "toUser" to otherUser,
            "itemId" to item.id,
            "frontImageUrl" to item.frontImageUrl,
            "confirmedBySender" to false,
            "confirmedByReceiver" to false,
            "timestamp" to FieldValue.serverTimestamp()
        )
        sessionRef.collection("pendingTrades").add(tradeData).await()
    }


    suspend fun finalizeTrade(sessionId: String) {
        val currentUser = Firebase.auth.currentUser
            ?: throw Exception("User not logged in")
        val sessionRef = Firebase.firestore
            .collection("trade_sessions")
            .document(sessionId)
        val pendingRef = sessionRef.collection("pendingTrades")

        val snapshot = pendingRef.get().await()
        snapshot.documents.forEach { doc ->
            val trade = doc.data ?: return@forEach
            val fromUser = trade["fromUser"] as? String ?: return@forEach
            val toUser = trade["toUser"] as? String ?: return@forEach
            val confirmedBySender = trade["confirmedBySender"] as? Boolean ?: false
            val confirmedByReceiver = trade["confirmedByReceiver"] as? Boolean ?: false
            val itemId = trade["itemId"] as? String ?: return@forEach

            val updates = mutableMapOf<String, Any>()
            if (currentUser.uid == fromUser) updates["confirmedBySender"] = true
            if (currentUser.uid == toUser) updates["confirmedByReceiver"] = true
            doc.reference.update(updates).await()

            // transfer ownership when both users have confirmed
            val finalConfirmed = (currentUser.uid == fromUser && confirmedByReceiver) ||
                    (currentUser.uid == toUser && confirmedBySender) ||
                    (confirmedBySender && confirmedByReceiver)

            if (finalConfirmed) {
                val itemSnapshot =
                    Firebase.firestore.collection("wardrobe")
                        .document(itemId)
                        .get()
                        .await()
                val item = itemSnapshot.toObject(WardrobeItem::class.java)
                    ?: return@forEach

                // transfer ownership
                Firebase.firestore.collection("wardrobe")
                    .document(item.id)
                    .set(item.copy(owner = toUser)).await()

                // remove pending trade
                doc.reference.delete().await()
            }
        }

        // delete the session after all trades have been processed
        val remainingTrades = pendingRef.get().await().documents
        if (remainingTrades.isEmpty()) {
            sessionRef.delete().await()
            _sessionState.value = null
        }
    }


    override fun onCleared() {
        super.onCleared()
        sessionListener?.remove()
    }
}


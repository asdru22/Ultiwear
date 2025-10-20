package com.aln.ultiwear.data

import android.net.Uri
import android.util.Log
import com.aln.ultiwear.model.PostedWardrobeItem
import com.aln.ultiwear.model.Trade
import com.aln.ultiwear.model.TradeMatch
import com.aln.ultiwear.model.tournament.TournamentUi
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TradeHandler(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    private val tag = "TradeHandler"

    suspend fun uploadTrade(
        userAId: String?,
        userBId: String,
        userAItems: List<String>,
        userBItems: List<String>,
        photoUri: Uri?,
        tournamentName: String?
    ) {

        var photoUrl: String? = null
        if (photoUri != null) {
            photoUrl = compressAndUpload(photoUri, "trade_photos/${UUID.randomUUID()}.webp")
        }

        val trade = Trade(
            id = firestore.collection("trades").document().id,
            userAId = userAId ?: "unknown",
            userBId = userBId,
            userAItems = userAItems,
            userBItems = userBItems,
            photoUrl = photoUrl,
            tournamentName = tournamentName
        )

        firestore.collection("trades").document(trade.id).set(trade).await()
    }

    suspend fun fetchTradeMatches(
        tradeableItems: List<PostedWardrobeItem>,
        attendingTournamentIds: Set<Int>,
        allEvents: List<TournamentUi>
    ): List<TradeMatch> {
        if (tradeableItems.isEmpty() || attendingTournamentIds.isEmpty()) return emptyList()

        val newMatches = mutableListOf<TradeMatch>()

        val tradeRef = firestore.collection("trade_interests")
        val allItemIds = tradeableItems.map { it.wardrobeItem.id }

        // Firestore "in" queries limited to 10 items
        val tradeSnapshots = allItemIds.chunked(10)
            .flatMap { chunk ->
                tradeRef.whereIn("itemId", chunk).get().await().documents
            }

        // Map: itemId -> list of interestedUserIds
        val tradeMap = tradeSnapshots.groupBy(
            keySelector = { it.getString("itemId") ?: "" },
            valueTransform = { it.getString("interestedUserId") ?: "" }
        )

        // load all attendances
        val interestedUserIds =
            tradeSnapshots.mapNotNull { it.getString("interestedUserId") }.distinct()

        val attendanceMap = mutableMapOf<Pair<String, Int>, Boolean>()

        interestedUserIds.forEach { userId ->
            val userTournaments = firestore
                .collection("user_attendance")
                .document(userId)
                .collection("tournaments")
                .get()
                .await()

            userTournaments.documents.forEach { doc ->
                val tournamentId = doc.getLong("tournamentId")?.toInt() ?: return@forEach
                val attending = doc.getBoolean("attending") ?: false
                attendanceMap[userId to tournamentId] = attending
            }
        }

        // check for matches
        tradeableItems.forEach { postedItem ->
            val itemId = postedItem.wardrobeItem.id
            val usersInterested = tradeMap[itemId] ?: emptyList()
            if (usersInterested.isEmpty()) return@forEach

            attendingTournamentIds.forEach { tournamentId ->
                // get the number of interested users
                val matchCount = usersInterested.count { userId ->
                    // create the pair (userID,tournamentId) so
                    // that it can be used as a key in the attendance map
                    attendanceMap[userId to tournamentId] == true
                }
                // if there's at least one
                if (matchCount >= 1) {
                    val tournament =
                        allEvents.firstOrNull { it.id == tournamentId } ?: return@forEach
                    // get and add tournament details
                    newMatches.add(
                        TradeMatch(
                            item = postedItem.wardrobeItem,
                            tournament = tournament,
                            matchCount = matchCount
                        )
                    )
                }
            }
        }

        return newMatches
    }


    suspend fun fetchIncomingMatches(
        browseItems: List<PostedWardrobeItem>,
        myAttendances: Map<Int, Boolean>,
        allEvents: List<TournamentUi>
    ): List<TradeMatch> {
        val currentUser = auth.currentUser ?: return emptyList()

        // get all trade interests of current user
        val interestSnapshot = firestore
            .collection("trade_interests")
            .whereEqualTo("interestedUserId", currentUser.uid)
            .get()
            .await()

        val interestedItemIds = interestSnapshot.documents
            .mapNotNull { it.getString("itemId") }
        if (interestedItemIds.isEmpty()) return emptyList()

        // check if the item the user is interested in is posted/still exists
        val allItems = browseItems.filter {
            interestedItemIds.contains(it.wardrobeItem.id)
        }
        if (allItems.isEmpty()) return emptyList()

        // fetch tournaments the current user is attending
        val myTournamentIds = myAttendances.filter { it.value }.keys.toSet()
        if (myTournamentIds.isEmpty()) return emptyList()

        val newMatches = mutableListOf<TradeMatch>()

        // get attendances for all owners
        val ownerIds = allItems.map { it.wardrobeItem.owner }.distinct()
        val attendanceMap =
            mutableMapOf<Pair<String, Int>, Boolean>() // (userId, tournamentId) -> attending

        ownerIds.forEach { ownerId ->
            val tournaments = firestore
                .collection("user_attendance")
                .document(ownerId)
                .collection("tournaments")
                .get()
                .await()

            tournaments.documents.forEach { doc ->
                val tournamentId = doc.getLong("tournamentId")?.toInt() ?: return@forEach
                val attending = doc.getBoolean("attending") ?: false
                attendanceMap[ownerId to tournamentId] = attending
            }
        }

        // check matches
        allItems.forEach { item ->
            val ownerId = item.wardrobeItem.owner
            myTournamentIds.forEach { tournamentId ->
                val ownerAttends = attendanceMap[ownerId to tournamentId] == true
                if (ownerAttends) {
                    val tournament =
                        allEvents.firstOrNull { it.id == tournamentId } ?: return@forEach
                    newMatches.add(
                        TradeMatch(
                            item = item.wardrobeItem,
                            tournament = tournament,
                            matchCount = 1 // only count the owner
                        )
                    )
                }
            }
        }

        return newMatches
    }

    suspend fun fetchUserTrades(): List<Trade> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()
        return try {
            // fetch trades where user is either userA or userB
            val snapshotA = firestore.collection("trades")
                .whereEqualTo("userAId", currentUserId)
                .get()
                .await()

            val snapshotB = firestore.collection("trades")
                .whereEqualTo("userBId", currentUserId)
                .get()
                .await()

            // combine snapshots
            val allDocuments = snapshotA.documents + snapshotB.documents

            // map to Trade objects
            allDocuments.mapNotNull { doc ->
                try {
                    Trade(
                        id = doc.id,
                        userAId = doc.getString("userAId") ?: "",
                        userBId = doc.getString("userBId") ?: "",
                        userAItems = (doc.get("userAItems") as? List<*>)
                            ?.mapNotNull { it as? String } ?: emptyList(),
                        userBItems = (doc.get("userBItems") as? List<*>)
                            ?.mapNotNull { it as? String } ?: emptyList(),
                        photoUrl = doc.getString("photoUrl"),
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        tournamentName = doc.getString("tournamentName") ?: "unknown"
                    )
                } catch (e: Exception) {
                    Log.e(tag, "Failed to map trade doc", e)
                    null
                }
            }.sortedBy { it.timestamp }.reversed()
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch trades", e)
            emptyList()
        }
    }
}
package com.aln.ultiwear.data

import android.util.Log
import com.aln.ultiwear.model.Post
import com.aln.ultiwear.model.PostedWardrobeItem
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await


class PostHandler(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    private val tag = "PostHandler"

    suspend fun fetchPosts(limit: Long = 20): List<PostedWardrobeItem> = coroutineScope {
        // fetch wardrobe items
        val wardrobeSnapshot = firestore.collection("wardrobe")
            .limit(limit)
            .get()
            .await()

        val wardrobeItems = wardrobeSnapshot.documents.mapNotNull { doc ->
            doc.toObject(WardrobeItem::class.java)?.copy(id = doc.id)
        }.filter { it.posted }

        if (wardrobeItems.isEmpty()) return@coroutineScope emptyList()

        // fetch posts
        val postsSnapshot = firestore.collection("posts")
            .whereIn("wardrobeUid", wardrobeItems.map { it.id })
            .get()
            .await()

        val postsByWardrobeUid = postsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(Post::class.java)?.let { it.wardrobeUid to it }
        }.toMap()

        wardrobeItems.map { item ->
            PostedWardrobeItem(
                wardrobeItem = item,
                post = postsByWardrobeUid[item.id],
                wardrobeUid = item.id
            )
        }
    }

    suspend fun toggleLike(wardrobeUid: String, userId: String): Int {
        val postQuery = firestore.collection("posts")
            .whereEqualTo("wardrobeUid", wardrobeUid)
            .limit(1)
            .get()
            .await()

        val postDoc = postQuery.documents.firstOrNull()
            ?: throw IllegalArgumentException("Post for wardrobeUid $wardrobeUid not found")

        val postRef = postDoc.reference
        val likeRef = postRef.collection("likes")
            .document(userId)

        return firestore.runTransaction { tx ->
            val postSnap = tx.get(postRef)
            var likeCount = postSnap.getLong("likes") ?: 0L

            val likeSnap = tx.get(likeRef)
            if (likeSnap.exists()) {
                tx.delete(likeRef)
                likeCount--
            } else {
                tx.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
                likeCount++
            }

            tx.set(
                postRef,
                mapOf("likes" to likeCount),
                SetOptions.merge()
            )
            likeCount.toInt()
        }.await()
    }

    suspend fun hasUserLiked(wardrobeUid: String, userId: String): Boolean {
        val postQuery = firestore
            .collection("posts")
            .whereEqualTo("wardrobeUid", wardrobeUid)
            .limit(1)
            .get()
            .await()

        val postDoc = postQuery.documents.firstOrNull() ?: return false
        val likeSnap = postDoc.reference
            .collection("likes")
            .document(userId)
            .get()
            .await()
        return likeSnap.exists()
    }

    suspend fun hasUserExpressedInterest(itemId: String): Boolean {
        val currentUser = auth.currentUser ?: return false

        return try {
            val snapshot = firestore
                .collection("trade_interests")
                .whereEqualTo("itemId", itemId)
                .whereEqualTo("interestedUserId", currentUser.uid)
                .get()
                .await()
            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            Log.e(tag, "Failed to check trade interest", e)
            false
        }
    }

    fun addTradeInterest(
        itemId: String,
        ownerId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val currentUser = auth.currentUser ?: return

        val tradeRef = firestore.collection("trade_interests")

        // prevent duplicate entries
        tradeRef
            .whereEqualTo("itemId", itemId)
            .whereEqualTo("interestedUserId", currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // user has not expressed interest yet, add document
                    val tradeDoc = hashMapOf(
                        "itemId" to itemId,
                        "ownerId" to ownerId,
                        "interestedUserId" to currentUser.uid,
                        "timestamp" to System.currentTimeMillis()
                    )
                    tradeRef.add(tradeDoc)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onFailure(e) }
                } else {
                    // already expressed interest
                    Log.d("TradeInterest", "User has already expressed interest")
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
}

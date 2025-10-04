package com.aln.ultiwear.data

import android.net.Uri
import android.util.Log
import com.aln.ultiwear.model.Condition
import com.aln.ultiwear.model.Post
import com.aln.ultiwear.model.Size
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

private const val tag = "HandleWardrobeItem"

suspend fun uploadWardrobeItem(
    frontUri: Uri,
    backUri: Uri? = null,
    condition: Condition,
    size: Size,
    post: Boolean,
    tradeable: Boolean
): WardrobeItem? = coroutineScope {
    val id = UUID.randomUUID().toString()
    val ownerId = Firebase.auth.currentUser?.uid ?: "unknown"

    try {
        // upload images on the dedicated IO threads
        val frontDeferred =
            async(Dispatchers.IO) {
                compressAndUpload(frontUri, "wardrobe/$id/front.webp")
            }
        val backDeferred = backUri?.let {
            async(Dispatchers.IO) {
                compressAndUpload(
                    it,
                    "wardrobe/$id/back.webp"
                )
            }
        }

        val frontUrl = frontDeferred.await() ?: run {
            Log.e(tag, "Front image upload failed")
            return@coroutineScope null // ensure the return is relative to the inner function
        }
        val backUrl = backDeferred?.await()

        val item = WardrobeItem(
            id = id,
            owner = ownerId,
            conditionStr = condition.name,
            sizeStr = size.name,
            frontImageUrl = frontUrl,
            backImageUrl = backUrl,
            tradeable = tradeable,
            posted = post
        )

        // save to Firestore
        withContext(Dispatchers.IO) {
            Firebase.firestore.collection("wardrobe").document(id)
                .set(item)
                .await()
        }

        Log.d(tag, "Wardrobe item uploaded")

        item
    } catch (e: Exception) {
        Log.e(tag, "Upload failed: ${e.message}")
        null
    }
}

// the listener updates the items when there are changes in the database
fun listenToWardrobeItems(
    userId: String,
    onItemsChanged: (List<WardrobeItem>) -> Unit
) {
    val firestore = Firebase.firestore
    firestore.collection("wardrobe")
        .whereEqualTo("owner", userId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error fetching items: ${error.message}")
                return@addSnapshotListener
            }
            val items = snapshot?.documents
                ?.mapNotNull { it.toObject(WardrobeItem::class.java) }
                ?: emptyList()
            onItemsChanged(items)
        }
}

suspend fun deleteWardrobeItem(id: String) {
    val firestore: FirebaseFirestore = Firebase.firestore
    try {

        val snapshot = firestore.collection("wardrobe")
            .document(id).get().await()
        val item = snapshot.toObject(WardrobeItem::class.java)

        // delete images
        item?.frontImageUrl?.let { url ->
            Firebase.storage.getReferenceFromUrl(url).delete().await()
            Log.d(tag, "Front image deleted")
        }
        item?.backImageUrl?.let { url ->
            Firebase.storage.getReferenceFromUrl(url).delete().await()
            Log.d(tag, "Back image deleted")
        }

        // delete wardrobe document
        firestore.collection("wardrobe")
            .document(id).delete().await()
        Log.d(tag, "Wardrobe item deleted")

        // delete associated post if present
        val postSnapshot = firestore.collection("posts")
            .whereEqualTo("wardrobeUid", id)
            .limit(1)
            .get()
            .await()

        if (postSnapshot.documents.isNotEmpty()) {
            postSnapshot.documents[0].reference.delete().await()
            Log.d(tag, "Post deleted")
        }

        if (item?.tradeable == true) {
            val tradeSnapshot = firestore.collection("trade_interests")
                .whereEqualTo("itemId", id)
                .get()
                .await()

            tradeSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
        }

    } catch (e: Exception) {
        Log.e(tag, "Failed to delete item, images, or post", e)
        throw e
    }
}

suspend fun makePost(item: WardrobeItem?) = try {
    val firestore: FirebaseFirestore = Firebase.firestore

    val postId = firestore.collection("posts").document().id
    val newPost = Post(
        wardrobeUid = item?.id ?: "none",
        likes = 0
    )

    firestore.collection("posts").document(postId)
        .set(newPost)
        .await()
    Log.d(tag, "Post created")
} catch (e: Exception) {
    Log.e(tag, "Failed to create post", e)
}
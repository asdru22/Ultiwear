package com.aln.ultiwear.data

import android.net.Uri
import android.util.Log
import com.aln.ultiwear.model.Condition
import com.aln.ultiwear.model.Size
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.UUID

const val tag = "HandleWardrobeItem"
suspend fun uploadImageAsync(uri: Uri, path: String): String? = suspendCancellableCoroutine { cont ->
    val storageRef = Firebase.storage.reference.child(path)
    storageRef.putFile(uri)
        .addOnSuccessListener {
            storageRef.downloadUrl
                .addOnSuccessListener { url -> cont.resume(url.toString(), null) }
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to get download URL: ${e.message}")
                    cont.resume(null, null)
                }
        }
        .addOnFailureListener { e ->
            Log.e(tag, "Upload failed: ${e.message}")
            cont.resume(null, null)
        }
}

suspend fun uploadWardrobeItem(
    frontUri: Uri,
    backUri: Uri?,
    condition: Condition,
    size: Size,
    post: Boolean,
    tradeable: Boolean
): WardrobeItem? = coroutineScope {
    val id = UUID.randomUUID().toString()
    val ownerId = Firebase.auth.currentUser?.uid ?: "unknown"

    try {
        // Upload front and back images in parallel
        val frontDeferred = async { uploadImageAsync(frontUri, "wardrobe/$id/front.jpg") }
        val backDeferred = backUri?.let { async { uploadImageAsync(it, "wardrobe/$id/back.jpg") } }

        val frontUrl = frontDeferred.await() ?: run {
            Log.e(tag, "Front image upload failed")
            return@coroutineScope null
        }
        val backUrl = backDeferred?.await()

        // Save item to Firestore
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

        Firebase.firestore.collection("wardrobe").document(id)
            .set(item)
            .await()
        Log.d(tag, "Wardrobe item uploaded")

        // Create post if needed
        if (post) {
            makePost(item) // can also be made suspendable if needed
        }

        item
    } catch (e: Exception) {
        Log.e(tag, "Upload failed: ${e.message}")
        null
    }
}


private fun uploadImage(
    uri: Uri,
    path: String,
    onComplete: (String?) -> Unit
) {
    val storageRef = Firebase.storage.reference.child(path)
    storageRef.putFile(uri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { url ->
                onComplete(url.toString())
            }.addOnFailureListener {
                Log.e(
                    tag, "Failed to get download URL: ${it.message}",
                )
                onComplete(null)
            }
        }
        .addOnFailureListener {
            Log.e(tag, "Upload failed: ${it.message}")
        }
}

fun saveWardrobeItemToFirestore(
    id: String,
    ownerId: String,
    condition: Condition,
    size: Size,
    frontUrl: String,
    backUrl: String?,
    post: Boolean,
    tradeable: Boolean,
    onUploaded: (WardrobeItem) -> Unit
): WardrobeItem {
    val firestore = Firebase.firestore

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

    firestore.collection("wardrobe").document(id)
        .set(item)
        .addOnSuccessListener { onUploaded(item) }
        .addOnFailureListener { e -> Log.e(tag, "Upload failed", e) }
    return item
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
                Log.e("WardrobeScreen", "Error fetching items: ${error.message}")
                return@addSnapshotListener
            }
            val items = snapshot?.documents
                ?.mapNotNull { it.toObject(WardrobeItem::class.java) }
                ?: emptyList()
            onItemsChanged(items)
        }
}

fun deleteWardrobeItemFromFirestore(
    id: String,
    onDeleted: () -> Unit
) {
    val firestore: FirebaseFirestore = Firebase.firestore

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // delete item
            firestore.collection("wardrobe").document(id).delete().await()
            Log.d(tag, "Wardrobe item deleted")

            // delete if post if present
            val snapshot = firestore.collection("posts")
                .whereEqualTo("wardrobeUid", id)
                .limit(1)
                .get()
                .await()

            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents[0].reference.delete().await()
                Log.d(tag, "Associated post deleted")
            }

            // update ui
            CoroutineScope(Dispatchers.Main).launch {
                onDeleted()
            }

        } catch (e: Exception) {
            Log.e(tag, "Failed to delete item or post", e)
            CoroutineScope(Dispatchers.Main).launch {
                onDeleted()
            }
        }
    }
}

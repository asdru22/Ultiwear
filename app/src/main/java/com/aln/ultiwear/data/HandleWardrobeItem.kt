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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

const val tag = "HandleWardrobeItem"
fun uploadWardrobeItem(
    frontUri: Uri,
    backUri: Uri?,
    condition: Condition,
    size: Size,
    post: Boolean,
    tradeable: Boolean,
    onUploaded: (WardrobeItem) -> Unit
): WardrobeItem? {
    val id = UUID.randomUUID().toString()
    var frontUrl: String? = null
    var backUrl: String? = null
    var uploadedItem: WardrobeItem? = null
    fun trySave() {
        if (frontUrl != null && (backUri == null || backUrl != null)) {
            uploadedItem = saveWardrobeItemToFirestore(
                id = id,
                ownerId = Firebase.auth.currentUser?.uid ?: "unknown",
                condition = condition,
                size = size,
                frontUrl = frontUrl!!,
                backUrl = backUrl,
                onUploaded = onUploaded,
                tradeable = tradeable,
                post = post
            )
        }
    }

    uploadImage(frontUri, "wardrobe/$id/front.jpg") { url ->
        if (url != null) {
            frontUrl = url
            trySave()
        } else {
            Log.e(tag, "Front image upload failed")
        }
    }

    backUri?.let {
        uploadImage(it, "wardrobe/$id/back.jpg") { url ->
            if (url != null) {
                backUrl = url
                trySave()
            } else {
                Log.e(tag, "Back image upload failed")
            }
        }
    }
    return uploadedItem
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

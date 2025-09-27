package com.aln.ultiwear.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.aln.ultiwear.model.Condition
import com.aln.ultiwear.model.Size
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.UUID

const val tag = "Firebase Items: "
fun uploadWardrobeItem(
    context: Context,
    frontUri: Uri,
    backUri: Uri?,
    condition: Condition,
    size: Size,
    onUploaded: (WardrobeItem) -> Unit
) {
    val id = UUID.randomUUID().toString()
    var frontUrl: String? = null
    var backUrl: String? = null

    fun trySave() {
        if (frontUrl != null && (backUri == null || backUrl != null)) {
            saveWardrobeItemToFirestore(
                id = id,
                ownerId = Firebase.auth.currentUser?.uid ?: "unknown",
                condition = condition,
                size = size,
                frontUrl = frontUrl!!,
                backUrl = backUrl,
                onUploaded = onUploaded
            )
        }
    }

    uploadImage(context, frontUri, "wardrobe/$id/front.jpg") { url ->
        if (url != null) {
            frontUrl = url
            trySave()
        } else {
            Toast.makeText(context, "Front image upload failed", Toast.LENGTH_LONG).show()
        }
    }

    backUri?.let {
        uploadImage(context, it, "wardrobe/$id/back.jpg") { url ->
            if (url != null) {
                backUrl = url
                trySave()
            } else {
                Toast.makeText(context, "Back image upload failed", Toast.LENGTH_LONG).show()
            }
        }
    }
}


private fun uploadImage(
    context: Context,
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
                Toast.makeText(
                    context,
                    "Failed to get download URL: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
                onComplete(null)
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Upload failed: ${it.message}", Toast.LENGTH_LONG).show()
            onComplete(null)
        }
}

fun saveWardrobeItemToFirestore(
    id: String,
    ownerId: String,
    condition: Condition,
    size: Size,
    frontUrl: String,
    backUrl: String?,
    onUploaded: (WardrobeItem) -> Unit
) {
    val firestore = Firebase.firestore

    val item = WardrobeItem(
        id = id,
        owner = ownerId,
        conditionStr = condition.name,
        sizeStr = size.name,
        frontImageUrl = frontUrl,
        backImageUrl = backUrl
    )

    firestore.collection("wardrobe").document(id)
        .set(item)
        .addOnSuccessListener { onUploaded(item) }
        .addOnFailureListener { e -> Log.e(tag, "Upload failed", e) }
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

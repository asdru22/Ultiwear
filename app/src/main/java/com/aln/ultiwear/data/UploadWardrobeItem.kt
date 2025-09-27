package com.aln.ultiwear.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.aln.ultiwear.model.Condition
import com.aln.ultiwear.model.Size
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.UUID

const val tag=  "AddWardrobeItemDialog: "
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
            saveWardrobeItemToFirestore(id, condition, size, frontUrl!!, backUrl, onUploaded)
        }
    }

    // Upload front image
    uploadImage(context, frontUri, "wardrobe/$id/front.jpg") { url ->
        if (url != null) {
            frontUrl = url
            trySave()
        } else {
            Toast.makeText(context, "Front image upload failed", Toast.LENGTH_LONG).show()
        }
    }

    // Upload back image if provided
    if (backUri != null) {
        uploadImage(context, backUri, "wardrobe/$id/back.jpg") { url ->
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

private fun saveWardrobeItemToFirestore(
    id: String,
    condition: Condition,
    size: Size,
    frontUrl: String,
    backUrl: String?,
    onUploaded: (WardrobeItem) -> Unit
) {
    val firestore = Firebase.firestore
    val item = WardrobeItem(
        id = id,
        owner = Firebase.auth.currentUser?.uid ?: "unknown",
        condition = condition,
        size = size,
        frontImageUrl = frontUrl,
        backImageUrl = backUrl
    )

    firestore.collection("wardrobe")
        .document(id)
        .set(item)
        .addOnSuccessListener { onUploaded(item) }
        .addOnFailureListener { e ->
            println(tag + "Failed to upload to firebase")
        }
}

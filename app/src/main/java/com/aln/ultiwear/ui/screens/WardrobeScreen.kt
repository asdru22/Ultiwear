package com.aln.ultiwear.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.aln.ultiwear.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

@Composable
fun WardrobeScreen() {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }

    // Launcher to select image from gallery
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { launcher.launch("image/*") }) {
            Text("Select profile picture")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected image preview
        selectedImageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Preview image",
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedImageUri != null) {
                    isUploading = true
                    uploadImageToFirebase(context, selectedImageUri!!) { downloadUrl ->
                        isUploading = false
                        uploadedImageUrl = downloadUrl
                        Toast.makeText(context, "Immagine caricata!", Toast.LENGTH_SHORT).show()

                        val user = User(
                            id = "12345",
                            email = "mario.rossi@email.com",
                        )
                        addUserToFirestore(user)
                    }
                } else {
                    Toast.makeText(context, "Select an image first", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text(if (isUploading) "Loading..." else "Save user")
        }
    }
}


fun addUserToFirestore(user: User) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users")
        .document(user.id)
        .set(user)
        .addOnSuccessListener {
            println("Utente aggiunto con ID: ${user.id}")
        }
        .addOnFailureListener { e ->
            println("Errore durante l'aggiunta dell'utente: ${e.message}")
        }
}

fun uploadImageToFirebase(context: android.content.Context, imageUri: Uri, onComplete: (String) -> Unit) {
    val storage: FirebaseStorage = FirebaseStorage.getInstance()
    val storageRef: StorageReference = storage.reference
    val imageRef: StorageReference = storageRef.child("profile_pictures/${imageUri.lastPathSegment}")

    val uploadTask = imageRef.putFile(imageUri)
    uploadTask.addOnSuccessListener {
        imageRef.downloadUrl.addOnSuccessListener { uri ->
            onComplete(uri.toString())
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Errore nel recuperare URL: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }.addOnFailureListener { e ->
        Toast.makeText(context, "Errore upload immagine: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
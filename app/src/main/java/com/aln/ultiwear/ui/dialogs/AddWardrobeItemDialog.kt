package com.aln.ultiwear.ui.dialogs

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aln.ultiwear.data.uploadWardrobeItem
import com.aln.ultiwear.model.Condition
import com.aln.ultiwear.model.Size
import com.aln.ultiwear.model.WardrobeItem

@Composable
fun AddWardrobeItemDialog(
    onDismiss: () -> Unit,
    onUpload: (WardrobeItem) -> Unit
) {
    var frontImageUri by remember { mutableStateOf<Uri?>(null) }
    var backImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCondition by remember { mutableStateOf<Condition?>(null) }
    var selectedSize by remember { mutableStateOf<Size?>(null) }
    val context = LocalContext.current

    // Launcher to pick image from gallery
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        frontImageUri = uri
    }

    // Launcher to take a picture
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) frontImageUri = cameraUri
    }

    // Optional back photo launcher
    var backCameraUri by remember { mutableStateOf<Uri?>(null) }
    val backCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) backImageUri = backCameraUri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (frontImageUri != null && selectedCondition != null && selectedSize != null) {
                        uploadWardrobeItem(
                            context,
                            frontImageUri!!,
                            backImageUri,
                            selectedCondition!!,
                            selectedSize!!,
                            onUpload
                        )
                        onDismiss()
                    }
                }
            ) { Text("Upload") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add Wardrobe Item") },
        text = {
            Column {
                // Front image selection
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text(if (frontImageUri == null) "Select Front Image from Gallery" else "Front Image Selected")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = {
                    cameraUri = createImageUri(context)
                    cameraLauncher.launch(cameraUri!!)
                }) {
                    Text(if (frontImageUri == null) "Take Front Photo" else "Front Image Selected")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Optional back image
                Button(onClick = {
                    backCameraUri = createImageUri(context)
                    backCameraLauncher.launch(backCameraUri!!)
                }) {
                    Text(if (backImageUri == null) "Take Back Photo (Optional)" else "Back Photo Selected")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Condition dropdown
                var conditionExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { conditionExpanded = true }) {
                        Text(selectedCondition?.name ?: "Select Condition")
                    }
                    DropdownMenu(
                        expanded = conditionExpanded,
                        onDismissRequest = { conditionExpanded = false }
                    ) {
                        Condition.entries.forEach { condition ->
                            DropdownMenuItem(
                                text = { Text(stringResource(condition.resId))
                                },
                                onClick = {
                                    selectedCondition = condition
                                    conditionExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Size dropdown
                var sizeExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { sizeExpanded = true }) {
                        Text(selectedSize?.name ?: "Select Size")
                    }
                    DropdownMenu(
                        expanded = sizeExpanded,
                        onDismissRequest = { sizeExpanded = false }
                    ) {
                        Size.entries.forEach { size ->
                            DropdownMenuItem(
                                text = { Text(size.name) },
                                onClick = {
                                    selectedSize = size
                                    sizeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

fun createImageUri(context: Context): Uri {
    val contentResolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.DISPLAY_NAME, "temp_${System.currentTimeMillis()}.jpg")
    }
    return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!
}

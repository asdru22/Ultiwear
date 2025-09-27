package com.aln.ultiwear.ui.dialogs

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import com.aln.ultiwear.R
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

    // Launchers for camera
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) frontImageUri = cameraUri
        }

    var backCameraUri by remember { mutableStateOf<Uri?>(null) }
    val backCameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
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
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) { Text(stringResource(R.string.upload)) }
        },
        dismissButton = {
            Button(
                onClick = onDismiss, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(stringResource(R.string.wardrobe_add_item)) },
        text = {
            UserInputs(
                frontImageUri = frontImageUri,
                backImageUri = backImageUri,
                selectedCondition = selectedCondition,
                selectedSize = selectedSize,
                onFrontImageClick = {
                    cameraUri = createImageUri(context)
                    cameraLauncher.launch(cameraUri!!)
                },
                onBackImageClick = {
                    backCameraUri = createImageUri(context)
                    backCameraLauncher.launch(backCameraUri!!)
                },
                onConditionSelected = { selectedCondition = it },
                onSizeSelected = { selectedSize = it }
            )
        }
    )
}

@Composable
fun UserInputs(
    frontImageUri: Uri?,
    backImageUri: Uri?,
    selectedCondition: Condition?,
    selectedSize: Size?,
    onFrontImageClick: () -> Unit,
    onBackImageClick: () -> Unit,
    onConditionSelected: (Condition) -> Unit,
    onSizeSelected: (Size) -> Unit
) {
    Column {
        // Front photo button
        Button(onClick = onFrontImageClick) {
            Text(
                if (frontImageUri == null) stringResource(R.string.wardrobe_front_picture)
                else stringResource(R.string.wardrobe_front_picture_selected)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Back photo button
        Button(onClick = onBackImageClick) {
            Text(
                if (backImageUri == null) stringResource(R.string.wardrobe_back_picture)
                else stringResource(R.string.wardrobe_back_picture_selected)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Condition dropdown
        var conditionExpanded by remember { mutableStateOf(false) }
        Box {
            Button(onClick = { conditionExpanded = true }) {
                Text(selectedCondition?.name ?: stringResource(R.string.wardrobe_select_condition))
            }
            DropdownMenu(
                expanded = conditionExpanded,
                onDismissRequest = { conditionExpanded = false }
            ) {
                Condition.entries.forEach { condition ->
                    DropdownMenuItem(
                        text = { Text(stringResource(condition.resId)) },
                        onClick = {
                            onConditionSelected(condition)
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
                Text(selectedSize?.name ?: stringResource(R.string.wardrobe_select_size))
            }
            DropdownMenu(
                expanded = sizeExpanded,
                onDismissRequest = { sizeExpanded = false }
            ) {
                Size.entries.forEach { size ->
                    DropdownMenuItem(
                        text = { Text(size.name) },
                        onClick = {
                            onSizeSelected(size)
                            sizeExpanded = false
                        }
                    )
                }
            }
        }
    }
}


fun createImageUri(context: Context): Uri {
    val contentResolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.DISPLAY_NAME, "temp_${System.currentTimeMillis()}.jpg")
    }
    return contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
    )!!
}

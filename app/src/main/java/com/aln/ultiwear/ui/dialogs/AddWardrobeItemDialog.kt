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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aln.ultiwear.R
import com.aln.ultiwear.data.makePost
import com.aln.ultiwear.data.uploadWardrobeItem
import com.aln.ultiwear.model.Condition
import com.aln.ultiwear.model.Size
import com.aln.ultiwear.model.WardrobeItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

@Composable
fun AddWardrobeItemDialog(
    onDismiss: () -> Unit,
    onUpload: (WardrobeItem) -> Unit
) {
    var frontImageUri by remember { mutableStateOf<Uri?>(null) }
    var backImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCondition by remember { mutableStateOf<Condition?>(null) }
    var selectedSize by remember { mutableStateOf<Size?>(null) }
    var post by remember { mutableStateOf(false) }
    var tradeable by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // create a CoroutineScope tied to the composable's lifecycle
    val coroutineScope = rememberCoroutineScope()
    // Launchers for camera
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) frontImageUri = cameraUri
        }

    var backCameraUri by remember { mutableStateOf<Uri?>(null) }
    val backCameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) backImageUri = backCameraUri
        }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (!isUploading && frontImageUri != null
                        && selectedCondition != null
                        && selectedSize != null
                    ) {
                        isUploading = true

                        coroutineScope.launch {
                            // momentarily suspend the coroutine to run others
                            // allowing recompositions triggered by isUplodaing==true
                            yield()

                            val uploadedItem = uploadWardrobeItem(
                                frontUri = frontImageUri!!,
                                backUri = backImageUri,
                                condition = selectedCondition!!,
                                size = selectedSize!!,
                                post = post,
                                tradeable = tradeable
                            )

                            uploadedItem?.let {
                                onUpload(it)
                                if (post) {
                                    launch { makePost(it) } // make the post concurrently
                                }
                            }

                            isUploading = false
                            onDismiss()
                        }
                    }
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                } else {
                    Text(stringResource(R.string.upload))
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) { Text(stringResource(R.string.cancel)) }
        },
        title = {
            Text(
                text = stringResource(R.string.wardrobe_add_item),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
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
                post = post,
                onPostChanged = { post = it },
                tradeable = tradeable,
                onTradeableChanged = { tradeable = it },
                onConditionSelected = { selectedCondition = it },
                onSizeSelected = { selectedSize = it }
            )
        }
    )
}


// The content parameter is a lambda that takes a RowScope composable
@Composable
fun InputRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        content = content
    )
}

val textSize = 13.sp


@Composable
fun UserInputs(
    frontImageUri: Uri?,
    backImageUri: Uri?,
    selectedCondition: Condition?,
    selectedSize: Size?,
    onFrontImageClick: () -> Unit,
    onBackImageClick: () -> Unit,
    onConditionSelected: (Condition) -> Unit,
    onSizeSelected: (Size) -> Unit,
    post: Boolean,
    onPostChanged: (Boolean) -> Unit,
    tradeable: Boolean,
    onTradeableChanged: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // front and optional back picture
        InputRow {
            Button(onClick = onFrontImageClick) {
                Text(
                    fontSize = textSize,
                    text = if (frontImageUri == null) stringResource(R.string.wardrobe_front_picture)
                    else stringResource(R.string.wardrobe_front_picture_selected)
                )
            }

            Button(onClick = onBackImageClick) {
                Text(
                    fontSize = textSize,
                    text = if (backImageUri == null) stringResource(R.string.wardrobe_back_picture)
                    else stringResource(R.string.wardrobe_back_picture_selected)
                )
            }
        }

        InputRow {
            // condition dropdown
            var conditionExpanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { conditionExpanded = true }) {
                    Text(
                        fontSize = textSize,
                        // run a lambda with select condition as the parameter
                        text = selectedCondition?.let { stringResource(it.resId) }
                            ?: stringResource(R.string.wardrobe_select_condition)
                    )
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

            // size dropdown
            var sizeExpanded by remember { mutableStateOf(false) }
            Box {
                Button(
                    onClick = { sizeExpanded = true }) {
                    // sizes don't need translation
                    Text(
                        fontSize = textSize,
                        text = selectedSize?.name ?: stringResource(R.string.wardrobe_select_size)
                    )
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

        // post and trade
        InputRow {
            Button(onClick = { onPostChanged(!post) }) {
                Text(
                    fontSize = textSize,
                    text = if (post) stringResource(R.string.wardrobe_published) else
                        stringResource(R.string.wardrobe_post)
                )
            }
            if (post) {
                Button(onClick = { onTradeableChanged(!tradeable) }) {
                    Text(
                        fontSize = textSize,
                        text = if (tradeable) stringResource(R.string.wardrobe_tradeable)
                        else stringResource(R.string.wardrobe_not_tradeable)
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

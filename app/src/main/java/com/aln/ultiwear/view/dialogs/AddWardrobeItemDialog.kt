package com.aln.ultiwear.view.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aln.ultiwear.R
import com.aln.ultiwear.data.createImageUri
import com.aln.ultiwear.model.Condition
import com.aln.ultiwear.model.Size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWardrobeItemDialog(
    onDismiss: () -> Unit,
    onUpload: (Uri, Uri?, Condition, Size, Boolean, Boolean) -> Unit,
    isUploading: Boolean,
    uploadSuccess: Boolean,
    onResetUploadState: () -> Unit
) {
    val context = LocalContext.current

    var frontImageUri by remember { mutableStateOf<Uri?>(null) }
    var backImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCondition by remember { mutableStateOf<Condition?>(null) }
    var selectedSize by remember { mutableStateOf<Size?>(null) }
    var post by remember { mutableStateOf(false) }
    var tradeable by remember { mutableStateOf(false) }

    var frontCameraUri by remember { mutableStateOf<Uri?>(null) }
    val frontCameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) frontImageUri = frontCameraUri
        }

    var backCameraUri by remember { mutableStateOf<Uri?>(null) }
    val backCameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) backImageUri = backCameraUri
        }

    // reset after successful upload
    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            onDismiss()
            onResetUploadState()
        }
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onUpload(
                        frontImageUri!!,
                        backImageUri,
                        selectedCondition!!,
                        selectedSize!!,
                        post,
                        tradeable
                    )
                },
                enabled = frontImageUri != null &&
                        selectedCondition != null &&
                        selectedSize != null &&
                        !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.upload))
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isUploading) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.wardrobe_add_item),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // photos
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ImageUploadBox(
                        uri = frontImageUri,
                        placeholderText = stringResource(R.string.wardrobe_front_picture),
                        onClick = {
                            frontCameraUri = createImageUri(context)
                            frontCameraLauncher.launch(frontCameraUri!!)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ImageUploadBox(
                        uri = backImageUri,
                        placeholderText = stringResource(R.string.wardrobe_back_picture),
                        onClick = {
                            backCameraUri = createImageUri(context)
                            backCameraLauncher.launch(backCameraUri!!)
                        }
                    )
                }

                // condition
                var conditionExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = conditionExpanded,
                    onExpandedChange = { conditionExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCondition?.let { stringResource(it.resId) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(stringResource(R.string.wardrobe_select_condition))
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = conditionExpanded,
                        onDismissRequest = { conditionExpanded = false }
                    ) {
                        Condition.entries.forEach { condition ->
                            DropdownMenuItem(
                                text = { Text(stringResource(condition.resId)) },
                                onClick = {
                                    selectedCondition = condition
                                    conditionExpanded = false
                                }
                            )
                        }
                    }
                }

                // size
                var sizeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = sizeExpanded,
                    onExpandedChange = { sizeExpanded = it }
                ) {

                    OutlinedTextField(
                        value = selectedSize?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(stringResource(R.string.wardrobe_select_size))
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
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

                // post
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.wardrobe_post))
                    Switch(checked = post, onCheckedChange = { post = it })
                }

                // tradeable
                if (post) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.wardrobe_tradeable))
                        Switch(checked = tradeable, onCheckedChange = { tradeable = it })
                    }
                }
            }
        }
    )
}

@Composable
fun ImageUploadBox(uri: Uri?, placeholderText: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.camera),
                    contentDescription = null
                )
                Text(placeholderText, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
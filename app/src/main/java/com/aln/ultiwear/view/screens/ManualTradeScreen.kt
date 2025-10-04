package com.aln.ultiwear.view.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.aln.ultiwear.R
import com.aln.ultiwear.data.createImageUri
import com.aln.ultiwear.model.WardrobeItem
import com.aln.ultiwear.view.dialogs.AddWardrobeItemDialog
import com.aln.ultiwear.viewModel.ManualTradeViewModel
import com.aln.ultiwear.viewModel.WardrobeViewModel

@Composable
fun ManualTradeScreen(
    wardrobeViewModel: WardrobeViewModel
) {
    // factory/initializer
    val manualTradeViewModel: ManualTradeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ManualTradeViewModel(
                    wardrobeViewModel = wardrobeViewModel,
                )
            }
        }
    )

    val wardrobeItems by wardrobeViewModel.wardrobeItems.collectAsState()
    val selectedGivenItems by manualTradeViewModel.selectedGivenItems.collectAsState()
    val receivedItems by manualTradeViewModel.receivedItems.collectAsState()
    val showAddReceivedDialog by manualTradeViewModel.showAddReceivedDialog.collectAsState()
    val isFinalizingTrade by manualTradeViewModel.isFinalizingTrade.collectAsState()

    val context = LocalContext.current
    var photoCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val photoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) manualTradeViewModel.setTradePhoto(photoCaptureUri)
        }

    if (isFinalizingTrade) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)), // semi-transparent overlay
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // select items to give away
            item {
                Text(
                    text = "Select items you're giving away:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 0.dp, max = 600.dp) // prevent infinite height
                ) {
                    items(wardrobeItems) { item ->
                        val isSelected = selectedGivenItems.contains(item)
                        FrontImageSelectableCard(
                            item = item,
                            isSelected = isSelected,
                            onClick = { manualTradeViewModel.toggleGivenItem(item) }
                        )
                    }
                }
            }

            // upload items to receive
            item {
                Text(
                    text = "Items you’re receiving:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    items(receivedItems) { item ->
                        ItemCard(item = item, onClick = {})
                    }
                    item {
                        IconButton(
                            onClick = { manualTradeViewModel.setShowAddReceivedDialog(true) },
                            modifier = Modifier
                                .size(100.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add received item")
                        }
                    }
                }
            }

            // take picture
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val tradePhotoUri = manualTradeViewModel.tradePhotoUri.collectAsState().value
                    if (tradePhotoUri != null) {
                        AsyncImage(
                            model = tradePhotoUri,
                            contentDescription = "Trade photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Button(onClick = {
                            photoCaptureUri = createImageUri(context)
                            photoLauncher.launch(photoCaptureUri!!)
                        }) {
                            Icon(painter = painterResource(R.drawable.camera), contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Take Photo")
                        }
                    }
                }

            }

            // finalize trade
            item {
                Button(
                    onClick = { manualTradeViewModel.finalizeTrade(context, userBId = "unknown") },
                    enabled = selectedGivenItems.isNotEmpty() || receivedItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Complete Trade")
                }
            }
        }
    }


    if (showAddReceivedDialog) {
        AddWardrobeItemDialog(
            onDismiss = { manualTradeViewModel.setShowAddReceivedDialog(false) },
            onUpload = { front,
                         back,
                         cond,
                         size,
                         post,
                         tradeable ->
                manualTradeViewModel.addReceivedItem(
                    front = front,
                    back = back,
                    condition = cond,
                    size = size,
                    post = post,
                    tradeable = tradeable
                )
            },
            isUploading = false,
            uploadSuccess = false,
            onResetUploadState = {}
        )
    }
}


@Composable
fun FrontImageSelectableCard(item: WardrobeItem, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        AsyncImage(
            model = item.frontImageUrl,
            contentDescription = "Front Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ItemCard(item: WardrobeItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(100.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        AsyncImage(
            model = item.frontImageUrl,
            contentDescription = "Received Item",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}



package com.aln.ultiwear.view.screens

import android.content.Context
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aln.ultiwear.R
import com.aln.ultiwear.data.createImageUri
import com.aln.ultiwear.model.WardrobeItem
import com.aln.ultiwear.view.dialogs.AddWardrobeItemDialog
import com.aln.ultiwear.view.shared.FrontImageSelectableCard
import com.aln.ultiwear.viewModel.EventViewModel
import com.aln.ultiwear.viewModel.ManualTradeViewModel
import com.aln.ultiwear.viewModel.WardrobeViewModel
import com.google.android.gms.location.LocationServices

@Composable
fun ManualTradeScreen(
    manualTradeViewModel: ManualTradeViewModel,
    wardrobeViewModel: WardrobeViewModel,
    eventViewModel: EventViewModel
) {


    val isFinalizingTrade by manualTradeViewModel.isFinalizingTrade.collectAsState()
    val showAddReceivedDialog by manualTradeViewModel.showAddReceivedDialog.collectAsState()

    val context = LocalContext.current

    if (isFinalizingTrade) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    } else {
        ManualTradeContent(
            wardrobeViewModel = wardrobeViewModel,
            manualTradeViewModel = manualTradeViewModel,
            eventViewModel = eventViewModel,
            context = context
        )
    }

    if (showAddReceivedDialog) {
        AddWardrobeItemDialog(
            onDismiss = { manualTradeViewModel.setShowAddReceivedDialog(false) },
            onUpload = { front, back, cond, size, post, tradeable ->
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
fun ManualTradeContent(
    wardrobeViewModel: WardrobeViewModel,
    manualTradeViewModel: ManualTradeViewModel,
    eventViewModel: EventViewModel,
    context: Context
) {
    val wardrobeItems by wardrobeViewModel
        .tradeableWardrobeItems.collectAsState()
    val selectedGivenItems by manualTradeViewModel
        .selectedGivenItems.collectAsState()
    val receivedItems by manualTradeViewModel
        .receivedItems.collectAsState()
    var photoCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    var currentLocation by remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                }
            }
        } catch (e: SecurityException) {
            Log.e("ManualTradeScreen", "Location permission missing: ${e.message}")
        }
    }

    val nearestTournaments = remember(
        currentLocation,
        eventViewModel.events
    ) {
        // if current location is not null (loc permission was granted)
        currentLocation?.let { loc ->
            eventViewModel.events
                // remove any events that don't have latitude or longitue
                .filter { it.lat != null && it.lng != null }
                .map { t ->
                    // because the android API uses a FloatArray to store the distance
                    val distance = FloatArray(1)
                    Location.distanceBetween(
                        // current location
                        loc.latitude, loc.longitude,
                        // tournament location
                        t.lat!!.toDouble(), t.lng!!.toDouble(),
                        // save it in distance
                        distance
                    )
                    // create a Pair<Tournament, Float>
                    t to distance[0] // meters
                }
                // sort by distance (second value in Pair)
                .sortedBy { it.second }
                // show top 3
                .take(3)
        } ?: emptyList()
    }

    val photoLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) manualTradeViewModel.setTradePhoto(photoCaptureUri)
        }

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
                    .heightIn(min = 0.dp, max = 600.dp)
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
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add received item"
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Optional picture:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // take picture
        item {
            TradePhotoBox(
                tradePhotoUri = manualTradeViewModel.tradePhotoUri.collectAsState().value,
                onTakePhotoClick = {
                    photoCaptureUri = createImageUri(context)
                    photoLauncher.launch(photoCaptureUri!!)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

        }

        // select nearest tournament
        if (nearestTournaments.isNotEmpty()) {
            item {
                Text(
                    text = "Select Tournament:",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(nearestTournaments) { (tournament, distance) ->
                val isSelected =
                    manualTradeViewModel.selectedTournamentName
                        .collectAsState().value == tournament.name

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            manualTradeViewModel.setSelectedTournament(tournament.name)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                    ) {
                        Text(
                            tournament.name,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${"%.1f".format(distance / 1000)} km away",
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

@Composable
fun TradePhotoBox(
    tradePhotoUri: Uri?,
    onTakePhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        if (tradePhotoUri != null) {
            AsyncImage(
                model = tradePhotoUri,
                contentDescription = "Trade photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            IconButton(
                onClick = onTakePhotoClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    painter = painterResource(R.drawable.camera),
                    contentDescription = "Take photo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
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



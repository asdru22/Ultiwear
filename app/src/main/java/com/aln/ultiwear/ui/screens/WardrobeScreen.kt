package com.aln.ultiwear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aln.ultiwear.R
import com.aln.ultiwear.data.listenToWardrobeItems
import com.aln.ultiwear.model.WardrobeItem
import com.aln.ultiwear.ui.dialogs.AddWardrobeItemDialog
import com.aln.ultiwear.ui.dialogs.WardrobeItemDetailsDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.auth


@Composable
fun WardrobeScreen() {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar(
            title = stringResource(R.string.wardrobe),
            action = {
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Item",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            WardrobeScreenContent(showDialog, onDialogDismiss = { showDialog = false })
        }
    }
}


@Composable
fun WardrobeScreenContent(showDialog: Boolean, onDialogDismiss: () -> Unit) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: return
    var wardrobeItems by remember {
        mutableStateOf<List<WardrobeItem>>(emptyList())
    }
    var selectedItem by remember { mutableStateOf<WardrobeItem?>(null) }

    // Load items from Firestore
    LaunchedEffect(currentUserId) {
        listenToWardrobeItems(currentUserId) { items ->
            wardrobeItems = items
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        if (showDialog) {
            AddWardrobeItemDialog(
                onDismiss = onDialogDismiss,
                onUpload = {
                    // getting items is handled by the listener
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(wardrobeItems) { item ->
                FrontImageCard(item) {
                    selectedItem = item
                }
            }
        }
    }

    if (selectedItem != null) {
        WardrobeItemDetailsDialog(item = selectedItem!!) {
            selectedItem = null
        }
    }
}


@Composable
fun FrontImageCard(item: WardrobeItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square card
            .clickable { onClick() },
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


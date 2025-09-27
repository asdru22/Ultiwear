package com.aln.ultiwear.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aln.ultiwear.R
import com.aln.ultiwear.data.listenToWardrobeItems
import com.aln.ultiwear.model.WardrobeItem
import com.aln.ultiwear.ui.dialogs.AddWardrobeItemDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore


@Composable
fun WardrobeScreen() {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        TitleBar(statusBarPadding) {
            showDialog = true
        }

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
fun TitleBar(statusBarPadding: Dp, onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarPadding + 6.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.wardrobe),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )

            IconButton(onClick = onAddClick) {
                Icon(
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ),
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Item",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun WardrobeScreenContent(showDialog: Boolean, onDialogDismiss: () -> Unit) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: return
    var wardrobeItems by remember { mutableStateOf<List<WardrobeItem>>(emptyList()) }
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
                onUpload = { item ->
                    wardrobeItems = wardrobeItems + item
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
        WardrobeItemDetailDialog(item = selectedItem!!) {
            selectedItem = null
        }
    }
}


@Composable
fun FrontImageCard(item: WardrobeItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        AsyncImage(
            model = item.frontImageUrl,
            contentDescription = "Front Image",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun WardrobeItemDetailDialog(item: WardrobeItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wardrobe Item") },
        text = {
            Column {
                AsyncImage(
                    model = item.frontImageUrl,
                    contentDescription = "Front Image",
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                item.backImageUrl?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = it,
                        contentDescription = "Back Image",
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Condition: ${stringResource(item.condition.resId)}")
                Text("Size: ${item.size.name}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

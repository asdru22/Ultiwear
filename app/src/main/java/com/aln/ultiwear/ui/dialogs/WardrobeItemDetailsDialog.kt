package com.aln.ultiwear.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aln.ultiwear.data.deleteWardrobeItemFromFirestore
import com.aln.ultiwear.model.WardrobeItem

@Composable
fun WardrobeItemDetailsDialog(
    item: WardrobeItem,
    onDismiss: () -> Unit
) {
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
        },
        dismissButton = {
            Button(
                onClick = {
                    deleteWardrobeItemFromFirestore(item.id) {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        }
    )
}

package com.aln.ultiwear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aln.ultiwear.R
import com.aln.ultiwear.model.Post
import com.aln.ultiwear.model.PostedWardrobeItem
import com.aln.ultiwear.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

@Composable
fun BrowseScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        BrowseTitleBar(statusBarPadding)

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            BrowseScreenContent()
        }
    }
}

@Composable
fun BrowseTitleBar(statusBarPadding: Dp) {
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
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = stringResource(R.string.browse),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}


@Composable
fun BrowseScreenContent() {
    var items by remember { mutableStateOf<List<PostedWardrobeItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val firestore = Firebase.firestore
        val snapshot = firestore.collection("wardrobe")
            .limit(20)
            .get()
            .await()

        val wardrobeItems = snapshot.documents.mapNotNull { doc ->
            doc.toObject(WardrobeItem::class.java)?.copy(id = doc.id)
        }

        val posts = wardrobeItems.map { item ->
            val postDoc = firestore.collection("posts")
                .document(item.id).get().await()
            val post = postDoc.toObject(Post::class.java)
            PostedWardrobeItem(item, post)
        }

        items = posts
    }

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { combined ->
            Box(
                Modifier
                    .fillParentMaxHeight()
                    .wrapContentHeight()
            ) {
                WardrobeItemCard(
                    combined.wardrobeItem,
                    combined.post?.likes ?: 0,
                    {},
                    {})
            }
        }
    }
}


@Composable
fun WardrobeItemCard(
    item: WardrobeItem,
    likes: Int,
    onLikeClicked: () -> Unit,
    onTradeClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column {
            val imageUrls = listOfNotNull(item.frontImageUrl, item.backImageUrl)
            val pagerState = rememberPagerState(pageCount = { imageUrls.size })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
            ) { page ->
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = if (page == 0) "Front Image" else "Back Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
            }

            WardrobeItemInfoBar(
                item = item,
                likes = likes,
                onLikeClicked = onLikeClicked,
                onTradeClicked = onTradeClicked,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@Composable
fun WardrobeItemInfoBar(
    item: WardrobeItem,
    likes: Int,
    onLikeClicked: () -> Unit,
    onTradeClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Fit
        OutlinedIconLabel(
            icon = Icons.Default.Star,
            text = stringResource(item.condition.resId),
            outlineColor = Color(0xFF4CAF50)
        )

        // Condition
        OutlinedIconLabel(
            icon = Icons.Default.Star,
            text = item.conditionStr,
            outlineColor = Color(0xFFFFC107)
        )

        // Likes
        OutlinedIconLabel(
            icon = Icons.Default.Star,
            text = likes.toString(),
            outlineColor = Color(0xFFE91E63),
            onClick = onLikeClicked
        )

        // Tradeable
        if (item.tradeable) {
            OutlinedIconLabel(
                icon = Icons.Default.Star,
                text = "",
                outlineColor = Color(0xFF2196F3),
                onClick = onTradeClicked
            )
        }
    }
}


@Composable
fun OutlinedIconLabel(
    icon: ImageVector,
    text: String,
    outlineColor: Color,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = outlineColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = outlineColor,
                modifier = Modifier.size(16.dp)
            )
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    color = outlineColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

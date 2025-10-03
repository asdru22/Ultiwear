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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aln.ultiwear.R
import com.aln.ultiwear.model.PostedWardrobeItem
import com.aln.ultiwear.model.WardrobeItem
import com.aln.ultiwear.viewModel.BrowseViewModel
import com.aln.ultiwear.viewModel.WardrobeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun BrowseScreen(browseViewModel: BrowseViewModel = viewModel(),
                 wardrobeViewModel: WardrobeViewModel
) {
    val items by browseViewModel.items
    val isLoading by browseViewModel.isLoading
    // convert the flow into a compose State object
    val postsChanged by wardrobeViewModel.itemsChanged.collectAsState()

    // launch a coroutine tied to the composable's lifecycle
    LaunchedEffect(postsChanged) {
        browseViewModel.fetchPosts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar(title = stringResource(R.string.browse))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                items.isEmpty() -> {
                    Text("No posts found", modifier = Modifier.align(Alignment.Center))
                }

                // isn't loading and there are items
                else -> {
                    BrowseScreenContent(items = items, viewModel = browseViewModel)
                }
            }
        }
    }
}


@Composable
fun BrowseScreenContent(
    items: List<PostedWardrobeItem>,
    viewModel: BrowseViewModel
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val currentUser = Firebase.auth.currentUser

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { combined ->
            var userLiked by remember { mutableStateOf(false) }

            LaunchedEffect(combined.wardrobeUid) {
                if (currentUser != null && combined.post != null) {
                    userLiked =
                        viewModel.handler.hasUserLiked(
                            wardrobeUid =  combined.wardrobeUid,
                            userId = currentUser.uid
                        )
                }
            }

            val isOwner = currentUser?.uid == combined.wardrobeItem.owner

            WardrobeItemCard(
                item = combined.wardrobeItem,
                likes = combined.post?.likes ?: 0,
                userLiked = userLiked,
                isOwner = isOwner,
                onLikeClicked = {
                    viewModel.toggleLike(combined)
                    userLiked = !userLiked
                },
                onTradeClicked = {}
            )
        }
    }
}


@Composable
fun WardrobeItemCard(
    item: WardrobeItem,
    likes: Int,
    userLiked: Boolean,
    onLikeClicked: () -> Unit,
    onTradeClicked: () -> Unit,
    isOwner: Boolean
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
            val imageUrls = listOfNotNull(
                item.frontImageUrl,
                item.backImageUrl
            )
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
                userLiked = userLiked,
                modifier = Modifier.fillMaxWidth(),
                isOwner = isOwner
            )
        }
    }
}


@Composable
fun WardrobeItemInfoBar(
    item: WardrobeItem,
    likes: Int,
    userLiked: Boolean,
    onLikeClicked: () -> Unit,
    onTradeClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isOwner: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // size
        OutlinedIconLabel(
            icon = R.drawable.size,
            text = item.sizeStr,
            color = Color(0xFF4CAF50)
        )

        // condition
        OutlinedIconLabel(
            icon = R.drawable.condition,
            text = stringResource(item.condition.resId),
            color = Color(0xFFFFC107)
        )

        val likeColor =
            if (isOwner) Color(0xFFE91E63).copy(alpha = 0.4f)
            else Color(0xFFE91E63)
        // likes
        OutlinedIconLabel(
            icon = R.drawable.like,
            text = likes.toString(),
            color = likeColor,
            selected = userLiked,
            onClick = if (isOwner) null else onLikeClicked
        )

        // tradeable
        if (item.tradeable) {
            val tradeColor =
                if (isOwner) Color(0xFF2196F3).copy(alpha = 0.4f)
                else Color(0xFF2196F3)
            OutlinedIconLabel(
                icon = R.drawable.trade_small,
                text = "",
                color = tradeColor,
                selected = false,
                onClick = if (isOwner) null else onTradeClicked
            )
        }
    }
}


@Composable
fun OutlinedIconLabel(
    icon: Int,
    text: String,
    color: Color,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val backgroundColor = if (selected) color else MaterialTheme.colorScheme.background
    val contentColor = if (selected) MaterialTheme.colorScheme.background else color
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = color,
                shape = shape
            )
            .background(color = backgroundColor, shape = shape)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    color = contentColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
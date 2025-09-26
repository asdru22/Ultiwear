package com.aln.ultiwear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aln.ultiwear.data.GoogleAuthClient
import com.aln.ultiwear.model.TabItem
import com.aln.ultiwear.ui.screens.LoginScreen
import com.aln.ultiwear.ui.screens.SettingsScreen
import com.aln.ultiwear.ui.theme.LocalBottomBarBackground
import com.aln.ultiwear.ui.theme.UltiwearTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val googleAuthClient = GoogleAuthClient(this)

        setContent {
            UltiwearTheme {
                var isSignedIn by rememberSaveable {
                    mutableStateOf(googleAuthClient.isSingedIn())
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    if (isSignedIn) {
                        AppWithBottomBar(onSignOut = { isSignedIn = false })
                    } else {
                        LoginScreen(
                            onSignIn = { googleAuthClient.signIn() },
                            onSignedIn = { isSignedIn = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppWithBottomBar(
    onSignOut: () -> Unit
) {
    val tabs = listOf(
        TabItem("Wardrobe", R.drawable.wardrobe) { WardrobeScreen() },
        TabItem("Social", R.drawable.social) { SocialScreen() },
        TabItem("Trade", R.drawable.trade) { TradeScreen() },
        TabItem("Settings", R.drawable.settings) { SettingsScreen(onSignOut) }
    )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Content of the selected tab
        tabs[selectedIndex].content()

        // Bottom bar
        Footer(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
                .background(
                    color = LocalBottomBarBackground.current,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(vertical = 8.dp),
            tabs,
            selectedIndex,
            onTabSelected = { selectedIndex = it }
        )
    }
}


@Composable
fun Footer(
    modifier: Modifier,
    tabs: List<TabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = modifier
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            FooterButton(
                isSelected,
                tab,
                Modifier
                    .weight(1f)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else Color.Transparent,
                        CircleShape
                    )
                    .clickable(
                        indication = null, // remove ripple rectangle
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTabSelected(index) })
        }
    }
}

@Composable
fun FooterButton(
    isSelected: Boolean,
    tab: TabItem,
    modifier: Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = tab.icon),
            contentDescription = tab.title,
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(28.dp)
        )
    }
}


@Composable
fun WardrobeScreen() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) { Text("Wardrobe") }
}

@Composable
fun SocialScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Social") }
}

@Composable
fun TradeScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Trade") }
}
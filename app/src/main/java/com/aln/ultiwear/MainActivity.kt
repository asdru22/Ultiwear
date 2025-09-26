package com.aln.ultiwear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.aln.ultiwear.ui.theme.LocalBottomBarBackground
import com.aln.ultiwear.ui.theme.UltiwearTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val googleAuthClient = GoogleAuthClient(this)

        setContent {
            UltiwearTheme {
                var isSignedIn by rememberSaveable { mutableStateOf(googleAuthClient.isSingedIn()) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSignedIn) {
                        AppWithBottomBar()
                    } else {
                        SignInButton {
                            lifecycleScope.launch {
                                isSignedIn = googleAuthClient.signIn()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppWithBottomBar() {
    val tabs = listOf(
        TabItem("Wardrobe", R.drawable.wardrobe) { WardrobeScreen() },
        TabItem("Social", R.drawable.social) { SocialScreen() },
        TabItem("Trade", R.drawable.trade) { TradeScreen() },
        TabItem("Settings", R.drawable.settings) { SettingsScreen() }
    )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Content of the selected tab
        tabs[selectedIndex].content()

        // Bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                .background(
                    color = LocalBottomBarBackground.current,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                Icon(
                    painter = painterResource(id = tab.icon),
                    contentDescription = tab.title,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f) //
                        .size(32.dp)
                        .clickable { selectedIndex = index }
                )
            }
        }
    }
}

data class TabItem(
    val title: String,
    val icon: Int,
    val content: @Composable () -> Unit
)

@Composable
fun SignInButton(onSignIn: () -> Unit) {
    OutlinedButton(onClick = onSignIn) {
        Text(
            text = "Sign In With Google",
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
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

@Composable
fun SettingsScreen() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) { Text("Settings") }
}

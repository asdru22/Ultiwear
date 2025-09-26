package com.aln.ultiwear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aln.ultiwear.data.GoogleAuthClient
import com.aln.ultiwear.model.TabItem
import com.aln.ultiwear.ui.screens.Footer
import com.aln.ultiwear.ui.screens.LoginScreen
import com.aln.ultiwear.ui.screens.SettingsScreen
import com.aln.ultiwear.ui.screens.WardrobeScreen
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
fun SocialScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Social") }
}

@Composable
fun TradeScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Trade") }
}
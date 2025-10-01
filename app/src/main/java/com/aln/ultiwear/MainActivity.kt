package com.aln.ultiwear

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aln.ultiwear.data.GoogleAuthClient
import com.aln.ultiwear.model.TabItem
import com.aln.ultiwear.ui.screens.BrowseScreen
import com.aln.ultiwear.ui.screens.EventScreen
import com.aln.ultiwear.ui.screens.Footer
import com.aln.ultiwear.ui.screens.LoginScreen
import com.aln.ultiwear.ui.screens.SettingsScreen
import com.aln.ultiwear.ui.screens.WardrobeScreen
import com.aln.ultiwear.ui.theme.LocalBottomBarBackground
import com.aln.ultiwear.ui.theme.UltiwearTheme
import com.aln.ultiwear.viewModel.EventViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val googleAuthClient = GoogleAuthClient(this)

        setContent {
            val vm = EventViewModel()

            UltiwearTheme {
                var isSignedIn by rememberSaveable {
                    mutableStateOf(googleAuthClient.isSingedIn())
                }
                var hasInternet by rememberSaveable {
                    mutableStateOf(isConnectedToInternet(this))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    when {
                        !hasInternet -> {
                            NoInternetScreen(
                                onRetry = { hasInternet = isConnectedToInternet(this) }
                            )
                        }

                        isSignedIn -> {
                            AppWithBottomBar(onSignOut = { isSignedIn = false }, viewModel = vm)
                        }

                        else -> {
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
}

private fun isConnectedToInternet(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun NoInternetScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.no_internet))
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.try_again))
        }
    }
}


@Composable
fun AppWithBottomBar(
    viewModel: EventViewModel,
    onSignOut: () -> Unit
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        TabItem("Wardrobe", R.drawable.wardrobe) { WardrobeScreen() },
        TabItem("Social", R.drawable.social) { BrowseScreen() },
        TabItem("Trade", R.drawable.trade) { EventScreen(viewModel) },
        TabItem("Settings", R.drawable.settings) { SettingsScreen(onSignOut) }
    )

    Scaffold(
        bottomBar = {
            Footer(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            tabs[selectedIndex].content()
        }
    }
}
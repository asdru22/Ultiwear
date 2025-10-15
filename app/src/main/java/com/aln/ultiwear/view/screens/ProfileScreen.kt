package com.aln.ultiwear.view.screens


import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.aln.ultiwear.R
import com.aln.ultiwear.notifications.TournamentReminderWorker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentUser = Firebase.auth.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar(
            title = stringResource(R.string.profile),
            content = {
                Button(
                    onClick = {
                        lifecycleOwner.lifecycleScope.launch {
                            onSignOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                    )
                }
            }
        )


        currentUser?.let { user ->
            UserInfo(user)
        }

        Spacer(Modifier.height(200.dp))

        val context = LocalContext.current
        Button(
            onClick = { testNotificationNow(context) },
            content = { Text("Test Notification") })

    }
}

@Composable
fun UserInfo(user: FirebaseUser) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // profile picture
        AsyncImage(
            model = user.photoUrl,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(12.dp))

        // email
        Text(
            text = user.email ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun testNotificationNow(context: Context) {
    val workRequest = OneTimeWorkRequestBuilder<TournamentReminderWorker>().build()
    WorkManager.getInstance(context).enqueue(workRequest)
}
package com.aln.ultiwear.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.aln.ultiwear.BuildConfig
import com.aln.ultiwear.model.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class GoogleAuthClient(
    private val context: Context,
) {
    private val tag = "GoogleAuthClient: "
    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun isSingedIn(): Boolean {
        if (firebaseAuth.currentUser != null) {
            println(tag + "already signed in")
            return true
        }
        return false
    }

    suspend fun signIn(): Boolean {
        if (isSingedIn()) {
            return true
        }
        try {
            val result = buildCredentialRequest()
            return handleSignIn(result)

        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e

            println(tag + "sinIn error: ${e.message}")
            return false
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse): Boolean {
        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            println(tag + "credential is not GoogleIdTokenCredential")
            return false
        }

        return try {
            val authCredential = getAuthCredential(credential) ?: return false
            val user = signInWithFirebase(authCredential) ?: return false
            checkAndRegisterUser(user)
            true
        } catch (e: GoogleIdTokenParsingException) {
            println(tag + "GoogleIdTokenParsingException: ${e.message}")
            false
        }
    }

    private fun getAuthCredential(credential: CustomCredential): AuthCredential? {
        return try {
            val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleAuthProvider.getCredential(tokenCredential.idToken, null)
        } catch (e: Exception) {
            println(tag + "Failed to parse GoogleIdToken: ${e.message}")
            null
        }
    }

    private suspend fun signInWithFirebase(authCredential: AuthCredential): FirebaseUser? {
        val authResult = firebaseAuth.signInWithCredential(authCredential).await()
        return authResult.user
    }

    private suspend fun checkAndRegisterUser(user: FirebaseUser) {
        val userRef = firestore.collection("users")
            .whereEqualTo("email", user.email)
            .get()
            .await()

        if (userRef.isEmpty) {
            registerNewUser(user)
        } else {
            println(tag + "User already exists in Firestore")
        }
    }

    private suspend fun registerNewUser(user: FirebaseUser) {
        val newUserId = UUID.randomUUID().toString()
        val newUser = User(id = newUserId, email = user.email ?: "")
        firestore.collection("users")
            .document(newUserId)
            .set(newUser.toMap())
            .await()

        println(tag + "New user registered with ID: $newUserId")
    }

    private suspend fun buildCredentialRequest(): GetCredentialResponse {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        return credentialManager.getCredential(
            request = request, context = context
        )
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(
            ClearCredentialStateRequest()
        )
        firebaseAuth.signOut()
    }
}

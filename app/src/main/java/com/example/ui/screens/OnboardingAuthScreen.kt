package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.i18n.getMbdStrings
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdCrimsonDark
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutMbd(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun OnboardingAuthScreen(
    language: String,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit,
    onEnterGuestMode: () -> Unit = {}
) {
    val strings = getMbdStrings(language)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE31B23),
                        MbdCrimson,
                        MbdCrimsonDark
                    )
                )
            )
    ) {
        // Subtle organic wave curves matching the reference splash/onboarding screen
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val topWave = Path().apply {
                moveTo(w * 0.25f, 0f)
                cubicTo(w * 0.55f, h * 0.14f, w * 0.85f, h * 0.08f, w, h * 0.28f)
                lineTo(w, 0f)
                close()
            }
            drawPath(topWave, color = Color.White.copy(alpha = 0.06f))

            val bottomWave1 = Path().apply {
                moveTo(0f, h * 0.62f)
                cubicTo(w * 0.35f, h * 0.68f, w * 0.55f, h * 0.84f, w, h * 0.78f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(bottomWave1, color = Color.White.copy(alpha = 0.06f))

            val bottomWave2 = Path().apply {
                moveTo(0f, h * 0.75f)
                cubicTo(w * 0.28f, h * 0.88f, w * 0.68f, h * 0.72f, w, h * 0.89f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(bottomWave2, color = Color.Black.copy(alpha = 0.08f))
        }

        val scrollState = androidx.compose.foundation.rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Language Switcher Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MBD 🩸",
                    color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onToggleLanguage() }
                        .testTag("onboarding_language_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Switch Language",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (strings.isBangla) "বাংলা | EN" else "EN | বাংলা",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.9f))

            // Large Blood Drop + Heart + Cupping Hands Illustration matching Reference Screen 1
            OnboardingBloodDropHeroLogo(modifier = Modifier.size(180.dp))

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = strings.onboardingTitle,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.onboardingSubtitle,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Subtle white horizontal divider line
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(1.5.dp)
                    .background(Color.White.copy(alpha = 0.6f))
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = strings.onboardingQuote,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp)
            )

            Spacer(modifier = Modifier.weight(1.1f))

            if (errorMessage != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Primary White Pill Button ("Get Started ->" / Google Sign-In) matching Reference Screen 1
            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MbdCrimson,
                    disabledContainerColor = Color.White.copy(alpha = 0.8f),
                    disabledContentColor = MbdCrimson
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .widthIn(max = 320.dp)
                    .height(54.dp)
                    .testTag("get_started_google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MbdCrimson,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.signingIn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Text(
                        text = "${strings.getStarted} (${strings.signInWithGoogle})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Guest Mode Button so user can explore and test all screens immediately
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .widthIn(max = 320.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onEnterGuestMode() }
                    .testTag("enter_guest_mode_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.continueAsGuest,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Mirpur Blood Donation • মিরপুর, ঢাকা",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun OnboardingBloodDropHeroLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. White Blood Drop Silhouette
        val dropPath = Path().apply {
            moveTo(w * 0.5f, h * 0.06f)
            cubicTo(
                w * 0.5f, h * 0.06f,
                w * 0.19f, h * 0.42f,
                w * 0.19f, h * 0.60f
            )
            cubicTo(
                w * 0.19f, h * 0.77f,
                w * 0.33f, h * 0.89f,
                w * 0.5f, h * 0.89f
            )
            cubicTo(
                w * 0.67f, h * 0.89f,
                w * 0.81f, h * 0.77f,
                w * 0.81f, h * 0.60f
            )
            cubicTo(
                w * 0.81f, h * 0.42f,
                w * 0.5f, h * 0.06f,
                w * 0.5f, h * 0.06f
            )
            close()
        }
        drawPath(path = dropPath, color = Color.White)

        // 2. Crimson Heart Inside Drop
        val heartPath = Path().apply {
            moveTo(w * 0.5f, h * 0.73f)
            cubicTo(
                w * 0.5f, h * 0.73f,
                w * 0.33f, h * 0.62f,
                w * 0.33f, h * 0.51f
            )
            cubicTo(
                w * 0.33f, h * 0.44f,
                w * 0.39f, h * 0.40f,
                w * 0.45f, h * 0.40f
            )
            cubicTo(
                w * 0.48f, h * 0.40f,
                w * 0.50f, h * 0.42f,
                w * 0.5f, h * 0.45f
            )
            cubicTo(
                w * 0.50f, h * 0.42f,
                w * 0.52f, h * 0.40f,
                w * 0.55f, h * 0.40f
            )
            cubicTo(
                w * 0.61f, h * 0.40f,
                w * 0.67f, h * 0.44f,
                w * 0.67f, h * 0.51f
            )
            cubicTo(
                w * 0.67f, h * 0.62f,
                w * 0.5f, h * 0.73f,
                w * 0.5f, h * 0.73f
            )
            close()
        }
        drawPath(path = heartPath, color = MbdCrimson)

        // 3. Left Cupping Hand
        val leftHand = Path().apply {
            moveTo(w * 0.10f, h * 0.62f)
            cubicTo(
                w * 0.14f, h * 0.62f,
                w * 0.17f, h * 0.75f,
                w * 0.32f, h * 0.87f
            )
            lineTo(w * 0.45f, h * 0.94f)
            lineTo(w * 0.42f, h * 0.99f)
            cubicTo(
                w * 0.24f, h * 0.93f,
                w * 0.11f, h * 0.80f,
                w * 0.07f, h * 0.65f
            )
            close()
        }
        drawPath(path = leftHand, color = Color.White)

        // 4. Right Cupping Hand
        val rightHand = Path().apply {
            moveTo(w * 0.90f, h * 0.62f)
            cubicTo(
                w * 0.86f, h * 0.62f,
                w * 0.83f, h * 0.75f,
                w * 0.68f, h * 0.87f
            )
            lineTo(w * 0.55f, h * 0.94f)
            lineTo(w * 0.58f, h * 0.99f)
            cubicTo(
                w * 0.76f, h * 0.93f,
                w * 0.89f, h * 0.80f,
                w * 0.93f, h * 0.65f
            )
            close()
        }
        drawPath(path = rightHand, color = Color.White)
    }
}

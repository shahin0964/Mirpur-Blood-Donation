package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdCrimsonDark
import com.example.util.DownloadState
import com.example.util.UpdateInfo
import com.example.util.UpdateManager
import kotlinx.coroutines.launch

@Composable
fun MandatoryUpdateScreen(
    updateInfo: UpdateInfo,
    isBangla: Boolean,
    updateManager: UpdateManager,
    onSkipOptionalUpdate: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloadState by updateManager.downloadState.collectAsState()

    var canInstall by remember { mutableStateOf(updateManager.canInstallPackages()) }

    // Disable Back Button if update is mandatory
    if (updateInfo.isMandatory) {
        BackHandler(enabled = true) {
            // Intercept and prevent dismissing mandatory update screen
        }
    }

    LaunchedEffect(Unit) {
        canInstall = updateManager.canInstallPackages()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MbdCrimson
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MbdCrimson.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "App Update",
                            tint = MbdCrimson,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = if (updateInfo.isMandatory) {
                            if (isBangla) "জরুরি অ্যাপ আপডেট প্রয়োজন" else "Mandatory App Update Required"
                        } else {
                            if (isBangla) "নতুন সংস্করণ সহজলভ্য" else "New Version Available"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Version Tags
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Text(
                                text = if (isBangla) "বর্তমান: v${updateInfo.currentVersionName}" else "Current: v${updateInfo.currentVersionName}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(text = "➔", color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MbdCrimson
                        ) {
                            Text(
                                text = "v${updateInfo.latestVersionName}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Guidance Description
                    Text(
                        text = if (updateInfo.isMandatory) {
                            if (isBangla) {
                                "মিরপুর ব্লাড ডোনেশন অ্যাপটির নিরবচ্ছিন্ন সেবা, সর্বোচ্চ নিরাপত্তা এবং গুরুত্বপূর্ণ ফিচার পেতে নতুন সংস্করণে আপডেট করা বাধ্যতামূলক।"
                            } else {
                                "To continue using Mirpur Blood Donation safely and access critical features, you must update to the latest version."
                            }
                        } else {
                            if (isBangla) {
                                "অ্যাপটির পারফরম্যান্স উন্নত করতে এবং নতুন সুবিধা উপভোগ করতে এখনই আপডেট করুন।"
                            } else {
                                "Update now to experience performance improvements and new features."
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    // Release Notes Card if available
                    val notes = if (isBangla) updateInfo.releaseNotesBn else updateInfo.releaseNotesEn
                    val displayNotes = notes.ifBlank { updateInfo.releaseNotesBn.ifBlank { updateInfo.releaseNotesEn } }

                    if (displayNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MbdCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBangla) "নতুন পরিবর্তনসমূহ:" else "What's New:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = displayNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Install Unknown Apps Permission Warning (Android 8.0+)
                    if (!canInstall) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3E0)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBangla) "ইন্সটল পারমিশন প্রয়োজন" else "Installation Permission Required",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isBangla) {
                                        "অ্যাপ সরাসরি ইন্সটল করতে 'Unknown Apps' ইন্সটল করার অনুমতি দিন।"
                                    } else {
                                        "Please allow 'Install Unknown Apps' permission in settings to install updates."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF5D4037)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        updateManager.openInstallPermissionSettings()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100))
                                ) {
                                    Text(if (isBangla) "সেটিংস খুলুন" else "Open Settings")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Download State Section
                    when (val state = downloadState) {
                        is DownloadState.Idle -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        canInstall = updateManager.canInstallPackages()
                                        coroutineScope.launch {
                                            updateManager.downloadApk(updateInfo.downloadUrl)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBangla) "ডাউনলোড ও আপডেট করুন" else "Download & Update Now",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        updateManager.openDownloadUrl(updateInfo.downloadUrl)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "প্লে স্টোর বা ব্রাউজারে খুলুন" else "Open in Play Store / Browser",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        is DownloadState.Downloading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    progress = { state.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MbdCrimson,
                                    trackColor = MbdCrimson.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isBangla) "ডাউনলোড হচ্ছে... ${state.progressPercent}%" else "Downloading... ${state.progressPercent}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MbdCrimson
                                )
                            }
                        }

                        is DownloadState.Completed -> {
                            Button(
                                onClick = {
                                    updateManager.installApk(state.fileUri)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MbdCrimsonDark),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "এখনই ইন্সটল করুন" else "Install Package Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }

                        is DownloadState.Error -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isBangla) "ত্রুটি: ${state.message}" else "Error: ${state.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        updateManager.resetDownloadState()
                                        coroutineScope.launch {
                                            updateManager.downloadApk(updateInfo.downloadUrl)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBangla) "পুনরায় চেষ্টা করুন" else "Retry Download",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        updateManager.openDownloadUrl(updateInfo.downloadUrl)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "প্লে স্টোর বা ব্রাউজারে খুলুন" else "Open in Play Store / Browser",
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Optional skip button for non-mandatory updates
                    if (!updateInfo.isMandatory && onSkipOptionalUpdate != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = onSkipOptionalUpdate,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBangla) "পরে করব" else "Skip for Now",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

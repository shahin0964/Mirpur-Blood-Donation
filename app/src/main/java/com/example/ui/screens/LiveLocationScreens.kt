package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BloodRequest
import com.example.data.model.DonorProfile
import com.example.data.model.LiveLocationSession
import com.example.ui.i18n.MbdStrings
import com.example.ui.theme.MbdAvailableGreen
import com.example.ui.theme.MbdBorderGray
import com.example.ui.theme.MbdCallGreen
import com.example.ui.theme.MbdChipUnselected
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdCrimsonDark
import com.example.ui.theme.MbdCrimsonLight
import com.example.ui.theme.MbdSoftPinkAlert
import com.example.ui.theme.MbdSurface
import com.example.ui.theme.MbdTextPrimary
import com.example.ui.theme.MbdTextSecondary
import com.example.ui.util.LiveLocationTracker
import com.example.ui.util.MbdNotificationHelper
import com.example.ui.util.dialPhoneNumber
import com.example.ui.util.formatRelativeTime
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1. Blood Request Details Screen
 * Includes:
 * - Patient, blood group, hospital, units, emergency contact, needed date/time, notes
 * - "Share Live Location" (“লাইভ লোকেশন শেয়ার করুন”) for the requester
 * - "View Live Location" (“লাইভ লোকেশন দেখুন”) for the authorized donor / active session
 * - "Stop Sharing Location" (“লোকেশন শেয়ার বন্ধ করুন”) at all times when active
 */
@Composable
fun BloodRequestDetailsScreen(
    request: BloodRequest,
    strings: MbdStrings,
    currentUserId: String,
    isDonorAvailable: Boolean,
    liveSessionState: UiState<LiveLocationSession?>,
    isAdminUser: Boolean = false,
    onBack: () -> Unit,
    onOpenShareConfirmation: () -> Unit,
    onOpenSharingStatus: () -> Unit,
    onOpenDonorLiveMap: () -> Unit,
    onStopSharingLocation: () -> Unit,
    onUpdateStatus: (String) -> Unit = {},
    onAcceptRequest: () -> Unit = {},
    onMarkFulfilled: () -> Unit,
    onDeleteRequest: () -> Unit,
    onReportRequest: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val isRequester = request.userId == currentUserId
    val session = (liveSessionState as? UiState.Success)?.data
    val nowMs = System.currentTimeMillis()
    val isSessionActive = (session != null && session.isActive && session.expiresAtEpochMs > nowMs) ||
        (request.liveSessionActive && request.liveSessionExpiresAt > nowMs)
    val isAuthorizedParticipant = isRequester ||
        request.authorizedDonorId == currentUserId ||
        session?.authorizedDonorId == currentUserId

    var showReportDialog by remember { mutableStateOf(false) }
    var reportReasonText by remember { mutableStateOf("") }
    var showFulfillConfirmationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Red Top App Bar
        Surface(
            color = MbdCrimson,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("request_details_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = strings.requestDetailsTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Under Admin Review Warning Banner if reported
            if (request.isReported) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MbdCrimson.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MbdCrimson.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MbdCrimson,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.reportedUnderReviewNotice,
                                color = MbdCrimson,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (request.reportReason.isNotBlank()) {
                                Text(
                                    text = request.reportReason,
                                    color = MbdTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Active Live Location Banner if currently sharing
            if (isSessionActive) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MbdAvailableGreen.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MbdAvailableGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingLiveDot(color = MbdAvailableGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.liveLocationActiveBanner,
                                color = MbdAvailableGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        val donorName = session?.authorizedDonorName?.ifBlank { request.authorizedDonorName }
                            ?: request.authorizedDonorName
                        if (donorName.isNotBlank()) {
                            Text(
                                text = if (strings.isBangla) {
                                    "অনুমোদিত রক্তদাতা: $donorName"
                                } else {
                                    "Shared with Authorized Donor: $donorName"
                                },
                                fontSize = 13.sp,
                                color = MbdTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isAuthorizedParticipant) {
                                Button(
                                    onClick = onOpenDonorLiveMap,
                                    colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("details_banner_view_live_map_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.viewLiveLocationBtn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (isRequester) {
                                OutlinedButton(
                                    onClick = onStopSharingLocation,
                                    border = BorderStroke(1.5.dp, MbdCrimson),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("details_banner_stop_sharing_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.StopCircle,
                                        contentDescription = null,
                                        tint = MbdCrimson,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.stopSharingLocationBtn,
                                        color = MbdCrimson,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Patient & Blood Request Details Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdBorderGray),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BloodDropBadgeIcon(modifier = Modifier.size(52.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${request.bloodGroup} (${request.bagsNeeded} ${if (strings.isBangla) "ব্যাগ" else "Bag(s)"})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = MbdTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MbdCrimsonLight
                                ) {
                                    Text(
                                        text = request.urgency,
                                        color = MbdCrimson,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                val resolvedStatus = request.resolvedStatus()
                                val statusLabel = when (resolvedStatus) {
                                    "donor_contacted" -> strings.statusDonorContacted
                                    "accepted" -> strings.statusAccepted
                                    "completed" -> strings.statusCompleted
                                    "cancelled" -> strings.statusCancelled
                                    else -> strings.statusPending
                                }
                                val statusColor = when (resolvedStatus) {
                                    "completed" -> MbdAvailableGreen
                                    "cancelled" -> Color.Gray
                                    "accepted" -> Color(0xFF2E7D32)
                                    "donor_contacted" -> Color(0xFF0277BD)
                                    else -> MbdCrimson
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = statusColor.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = statusLabel,
                                        color = statusColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MbdBorderGray)

                    RequestInfoItem(label = strings.requiredDonationTypeLabel, value = request.donationTypeLabel(strings.isBangla))
                    RequestInfoItem(label = strings.patientNameLabel, value = request.patientName)
                    RequestInfoItem(label = strings.hospitalNameLabel, value = request.hospitalName)
                    RequestInfoItem(label = strings.locationLabel, value = request.location)
                    if (request.neededDateTime.isNotBlank()) {
                        RequestInfoItem(label = strings.neededDateTimeLabel, value = request.neededDateTime)
                    }
                    RequestInfoItem(label = strings.contactNumberLabel, value = request.contactNumber)
                    if (request.notes.isNotBlank()) {
                        RequestInfoItem(label = strings.notesLabel, value = request.notes)
                    }

                    // Call Emergency Contact Button
                    Button(
                        onClick = { dialPhoneNumber(context, request.contactNumber) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MbdCallGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("details_call_requester_button")
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${strings.callBtn} (${request.contactNumber})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Live Location Sharing Action Card (Core Feature)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdCrimson.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MbdCrimsonLight,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShareLocation,
                                    contentDescription = null,
                                    tint = MbdCrimson,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isBangla) {
                                    "রিয়েল-টাইম লাইভ লোকেশন শেয়ারিং"
                                } else {
                                    "Real-Time Live Location Sharing"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MbdTextPrimary
                            )
                            Text(
                                text = if (strings.isBangla) {
                                    "রক্তদাতাকে বারবার লোকেশন জিজ্ঞেস না করে সরাসরি ম্যাপে আপনার অবস্থান দেখান।"
                                } else {
                                    "Help the selected blood donor reach the hospital or patient without repeated calls."
                                },
                                fontSize = 12.sp,
                                color = MbdTextSecondary
                            )
                        }
                    }

                    if (request.status == "open") {
                        if (isRequester) {
                            if (!isSessionActive) {
                                Button(
                                    onClick = onOpenShareConfirmation,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MbdCrimson,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("share_live_location_button")
                                ) {
                                    Icon(imageVector = Icons.Default.ShareLocation, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.shareLiveLocationBtn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onOpenSharingStatus,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MbdAvailableGreen,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("open_sharing_status_button")
                                ) {
                                    Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.liveLocationActiveBanner,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = onStopSharingLocation,
                                    border = BorderStroke(1.5.dp, MbdCrimson),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("stop_sharing_location_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.StopCircle,
                                        contentDescription = null,
                                        tint = MbdCrimson
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.stopSharingLocationBtn,
                                        color = MbdCrimson,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        // "View Live Location" button for Authorized Donor (or Requester previewing donor map)
                        if (isAuthorizedParticipant && isSessionActive) {
                            Button(
                                onClick = onOpenDonorLiveMap,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MbdCrimsonDark,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("view_live_location_button")
                            ) {
                                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.viewLiveLocationBtn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        } else if (!isRequester && !isSessionActive) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MbdSoftPinkAlert,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (strings.isBangla) {
                                        "আবেদনকারী এখনো এই আবেদনের জন্য লাইভ লোকেশন শেয়ার চালু করেননি।"
                                    } else {
                                        "The requester has not started sharing live location for this blood request yet."
                                    },
                                    color = MbdTextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Owner / Admin / Authorized Donor Request Management Card
            val isAuthorizedDonor = request.authorizedDonorId == currentUserId
            val canVolunteer = !isRequester && !isAdminUser && request.status == "open" && request.authorizedDonorId.isBlank()

            if (isRequester || isAdminUser || isAuthorizedDonor || canVolunteer) {
                val resolvedStatus = request.resolvedStatus()
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MbdSurface),
                    border = BorderStroke(1.dp, MbdBorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (strings.isBangla) "আবেদনের অবস্থা ও ব্যবস্থাপনা" else "Request Status & Management",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MbdTextPrimary
                        )

                        if (canVolunteer) {
                            if (!isDonorAvailable) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MbdSoftPinkAlert.copy(alpha = 0.8f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MbdCrimson,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (strings.isBangla) "আপনি বর্তমানে কোল্ডডাউন পিরিয়ডে আছেন। নতুন আবেদন গ্রহণ করতে পারবেন না।" else "You are in cooldown. Cannot accept new requests.",
                                            fontSize = 11.sp,
                                            color = MbdCrimson,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Primary Action for Potential Donors
                            Button(
                                onClick = onAcceptRequest,
                                enabled = isDonorAvailable,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MbdCrimson,
                                    disabledContainerColor = MbdBorderGray.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("details_accept_request_button")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (strings.isBangla) "রক্তদান করতে আগ্রহী (Accept Request)" else "Accept Request & Volunteer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        if (!request.isTerminal() && (isRequester || isAdminUser || isAuthorizedDonor)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isRequester || isAdminUser) {
                                    if (resolvedStatus == "pending") {
                                        OutlinedButton(
                                            onClick = { onUpdateStatus("donor_contacted") },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("details_mark_contacted_button")
                                        ) {
                                            Text(
                                                text = strings.markDonorContactedBtn,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (isAuthorizedDonor && resolvedStatus == "accepted") {
                                    // Donor specific actions
                                    OutlinedButton(
                                        onClick = { onUpdateStatus("pending") },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("details_cancel_commitment_button")
                                    ) {
                                        Text(
                                            text = if (strings.isBangla) "অঙ্গীকার বাতিল করুন" else "Cancel My Commitment",
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // "Mark as Fulfilled" - Now visible to Donor too!
                                if (isRequester || isAdminUser || isAuthorizedDonor) {
                                    Button(
                                        onClick = { showFulfillConfirmationDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MbdAvailableGreen),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("details_mark_fulfilled_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAuthorizedDonor) {
                                                if (strings.isBangla) "আমি রক্ত দিয়েছি (Fulfilled)" else "I have Donated (Fulfilled)"
                                            } else {
                                                strings.markCompletedBtn
                                            },
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isRequester || isAdminUser) {
                                    OutlinedButton(
                                        onClick = { onUpdateStatus("cancelled") },
                                        border = BorderStroke(1.dp, Color.Gray),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("details_mark_cancelled_button")
                                    ) {
                                        Text(
                                            text = strings.markCancelledBtn,
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (isRequester || isAdminUser) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = onDeleteRequest,
                                    border = BorderStroke(1.dp, MbdCrimson),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("details_delete_request_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = MbdCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (strings.isBangla) "আবেদন মুছুন" else "Delete",
                                        color = MbdCrimson,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Report Suspicious / Inappropriate Request Action
            TextButton(
                onClick = { showReportDialog = true },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("details_report_request_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MbdTextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.reportRequestTitle,
                    color = MbdTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showReportDialog) {
        val predefinedReasons = if (strings.isBangla) {
            listOf("ভুল বা বন্ধ নম্বর", "ইতিমধ্যে রক্ত পাওয়া গেছে / পুরনো আবেদন", "অবাঞ্ছিত তথ্য / স্প্যাম", "অন্যান্য সমস্যা")
        } else {
            listOf("Incorrect or unreachable phone number", "Already received blood / Outdated", "Spam / False emergency", "Other issue")
        }

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text(
                    text = strings.reportRequestTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MbdCrimson
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (strings.isBangla) "রিপোর্টের কারণ নির্বাচন বা লিখুন:" else "Select or enter report reason:",
                        fontSize = 13.sp,
                        color = MbdTextSecondary
                    )
                    predefinedReasons.forEach { reasonOption ->
                        val isChosen = reportReasonText == reasonOption
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChosen) MbdCrimsonLight else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isChosen) MbdCrimson else MbdBorderGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReasonText = reasonOption }
                        ) {
                            Text(
                                text = reasonOption,
                                fontSize = 12.sp,
                                color = if (isChosen) MbdCrimson else MbdTextPrimary,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reportReasonText,
                        onValueChange = { reportReasonText = it },
                        placeholder = { Text(strings.reportReasonHint, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MbdCrimson,
                            unfocusedBorderColor = MbdBorderGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReasonText.isNotBlank()) {
                            onReportRequest(reportReasonText)
                            showReportDialog = false
                        }
                    },
                    enabled = reportReasonText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson)
                ) {
                    Text(strings.reportSubmitBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(if (strings.isBangla) "বাতিল" else "Cancel", color = MbdTextSecondary)
                }
            }
        )
    }

    if (showFulfillConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showFulfillConfirmationDialog = false },
            title = {
                Text(
                    text = if (strings.isBangla) "রক্তদান সম্পন্ন নিশ্চিতকরণ" else "Confirm Blood Donation Completion",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MbdTextPrimary
                )
            },
            text = {
                Text(
                    text = if (strings.isBangla) {
                        "আপনি কি এই আবেদনটির জন্য সফলভাবে রক্তদান সম্পন্ন করেছেন?\n\n'হ্যাঁ' সিলেক্ট করলে হাসপাতাল (${request.hospitalName}) ও গ্রহীতার তথ্য স্বয়ংক্রিয়ভাবে আপনার রক্তদানের ইতিহাসে যুক্ত হবে এবং পরবর্তী সময়ের জন্য কোল্ডডাউন কাউন্টডাউন শুরু হবে।"
                    } else {
                        "Did you successfully complete blood donation for this request?\n\nSelecting 'Yes' will automatically log the hospital (${request.hospitalName}) and recipient details to your donation history and start your cooldown countdown."
                    },
                    fontSize = 13.sp,
                    color = MbdTextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFulfillConfirmationDialog = false
                        onMarkFulfilled()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbdAvailableGreen)
                ) {
                    Text(if (strings.isBangla) "হ্যাঁ, রক্তদান সম্পন্ন করেছি" else "Yes, Blood Donated")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFulfillConfirmationDialog = false }) {
                    Text(if (strings.isBangla) "না (ফিরে যান)" else "No, Cancel")
                }
            }
        )
    }
}

/**
 * 2. Share Live Location Confirmation Screen
 * Asks for explicit consent, allows selecting the authorized donor participant and session expiry duration,
 * and requests runtime ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION permissions before starting.
 */
@Composable
fun ShareLiveLocationConfirmationScreen(
    request: BloodRequest,
    strings: MbdStrings,
    donorsState: UiState<List<DonorProfile>>,
    onBack: () -> Unit,
    onConfirmStartSharing: (
        authorizedDonorId: String,
        authorizedDonorName: String,
        durationMinutes: Int,
        initialLat: Double,
        initialLng: Double,
        initialAcc: Double
    ) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val availableDonors = remember(donorsState, request.bloodGroup) {
        val list = (donorsState as? UiState.Success)?.data.orEmpty()
        val matching = list.filter { it.bloodGroup.equals(request.bloodGroup, ignoreCase = true) }
        if (matching.isNotEmpty()) matching + list.filterNot { it in matching } else list
    }

    var selectedDonor by remember(availableDonors) {
        mutableStateOf(availableDonors.firstOrNull())
    }
    var selectedDurationMins by remember { mutableStateOf(30) }
    var userAgreedConsent by remember { mutableStateOf(false) }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }
    var gpsDisabledWarning by remember {
        mutableStateOf(!LiveLocationTracker.isLocationServicesEnabled(context))
    }

    val startSharingWithPermittedLocation = {
        val donorId = selectedDonor?.userId?.ifBlank { selectedDonor?.id } ?: "authorized_donor_participant"
        val donorName = selectedDonor?.fullName?.ifBlank { "নির্বাচিত রক্তদাতা (Authorized Donor)" }
            ?: if (strings.isBangla) "নির্বাচিত রক্তদাতা" else "Authorized Blood Donor"

        // Send notification to the authorized donor
        MbdNotificationHelper.showLiveLocationSharedNotification(
            context = context,
            requesterName = request.requesterName.ifBlank { request.patientName },
            bloodGroup = request.bloodGroup,
            hospitalName = request.hospitalName,
            authorizedDonorName = donorName,
            isBangla = strings.isBangla
        )

        onConfirmStartSharing(
            donorId,
            donorName,
            selectedDurationMins,
            23.8069, // Mirpur-10 default initial anchor until first GPS fix arrives
            90.3687,
            12.0
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            permissionDeniedMessage = null
            startSharingWithPermittedLocation()
        } else {
            permissionDeniedMessage = if (strings.isBangla) {
                "লোকেশন অনুমতি প্রদান করা হয়নি। লাইভ লোকেশন শেয়ার করতে অনুগ্রহ করে লোকেশন পারমিশন (Location Permission) অনুমতি দিন।"
            } else {
                "Location permission was denied. Please grant location permission to share your live location with the donor."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MbdCrimson,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("confirm_share_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = strings.shareLiveLocationBtn,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Privacy & Security Notice Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSoftPinkAlert),
                border = BorderStroke(1.dp, MbdCrimson.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MbdCrimson,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (strings.isBangla) {
                                "গোপনীয়তা ও নিরাপত্তা সুরক্ষা"
                            } else {
                                "Privacy & Security Protection"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MbdCrimson
                        )
                        Text(
                            text = if (strings.isBangla) {
                                "• শুধুমাত্র আপনার নির্বাচিত রক্তদাতা এই লাইভ লোকেশন দেখতে পাবেন।\n" +
                                    "• পাবলিক ডিরেক্টরিতে কখনোই আপনার সুনির্দিষ্ট জিপিএস লোকেশন দেখানো হবে না।\n" +
                                    "• আপনি যেকোনো মুহূর্তে 'লোকেশন শেয়ার বন্ধ করুন' বাটনে চাপ দিয়ে তাৎক্ষণিক শেয়ার বন্ধ ও ডেটা মুছে ফেলতে পারবেন।"
                            } else {
                                "• Only the authorized blood donor you select below can view this live location.\n" +
                                    "• Your precise GPS coordinates are never exposed publicly.\n" +
                                    "• Location sharing stops automatically when the timer expires or when you tap Stop Sharing."
                            },
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MbdTextPrimary
                        )
                    }
                }
            }

            if (gpsDisabledWarning) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
                    border = BorderStroke(1.dp, Color(0xFFFF9800)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GpsOff,
                                contentDescription = null,
                                tint = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (strings.isBangla) {
                                    "ডিভাইসের GPS / Location সার্ভিস বন্ধ আছে"
                                } else {
                                    "Device GPS / Location Services Disabled"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                        Text(
                            text = if (strings.isBangla) {
                                "সঠিক লাইভ লোকেশন আপডেট পেতে অনুগ্রহ করে ফোনের লোকেশন (GPS) চালু করুন।"
                            } else {
                                "Please enable device Location / GPS so accurate coordinates can be shared with the donor."
                            },
                            fontSize = 12.sp,
                            color = MbdTextPrimary
                        )
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                } catch (ignored: Exception) {
                                }
                                gpsDisabledWarning = !LiveLocationTracker.isLocationServicesEnabled(context)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (strings.isBangla) "জিপিএস সেটিংস খুলুন" else "Open Location Settings",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (permissionDeniedMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MbdCrimson.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = permissionDeniedMessage!!,
                        color = MbdCrimson,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Step 1: Select Authorized Donor
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdBorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.selectAuthorizedDonorLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MbdTextPrimary
                    )

                    if (availableDonors.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MbdChipUnselected,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (strings.isBangla) {
                                    "নির্বাচিত রক্তদাতা: অনুমোদিত রক্তদাতা (${request.bloodGroup})"
                                } else {
                                    "Selected Participant: Authorized ${request.bloodGroup} Donor"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableDonors.take(5).forEach { donor ->
                                val isSelected = selectedDonor?.id == donor.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MbdCrimsonLight else Color(0xFFF8F9FA),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MbdCrimson else MbdBorderGray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedDonor = donor }
                                        .testTag("select_donor_participant_${donor.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isSelected) MbdCrimson else MbdTextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = donor.fullName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MbdTextPrimary
                                            )
                                            Text(
                                                text = "${donor.bloodGroup} • ${donor.area}",
                                                fontSize = 12.sp,
                                                color = MbdTextSecondary
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MbdCrimson
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Step 2: Configurable Expiry Period
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdBorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.sessionDurationLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MbdTextPrimary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(15, 30, 60, 120)) { mins ->
                            val selected = selectedDurationMins == mins
                            val label = if (strings.isBangla) "$mins মিনিট" else "$mins Mins"
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) MbdCrimson else MbdChipUnselected,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { selectedDurationMins = mins }
                                    .testTag("duration_chip_$mins")
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) Color.White else MbdTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Step 3: Explicit User Agreement Checkbox
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdBorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { userAgreedConsent = !userAgreedConsent }
                        .padding(14.dp)
                        .testTag("live_location_consent_checkbox"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = userAgreedConsent,
                        onCheckedChange = { userAgreedConsent = it },
                        colors = CheckboxDefaults.colors(checkedColor = MbdCrimson)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.liveSharingConsentText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MbdTextPrimary
                    )
                }
            }

            // Confirm & Start Live Location Sharing Button
            Button(
                onClick = {
                    if (!userAgreedConsent) {
                        permissionDeniedMessage = if (strings.isBangla) {
                            "লাইভ লোকেশন শেয়ার শুরু করার আগে অনুগ্রহ করে সম্মতি বক্সে টিক দিন।"
                        } else {
                            "Please check the consent box to explicitly agree before sharing your location."
                        }
                        return@Button
                    }

                    if (LiveLocationTracker.hasLocationPermission(context)) {
                        startSharingWithPermittedLocation()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MbdCrimson,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_start_live_location_button")
            ) {
                Icon(imageVector = Icons.Default.ShareLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.startSharingConfirmBtn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * 3. Live Location Sharing Status Screen (Requester's Active Sharing View)
 * Streams real Android GPS location updates to Firestore while active and provides an
 * always-visible "Stop Sharing Location" (“লোকেশন শেয়ার বন্ধ করুন”) button.
 */
@Composable
fun LiveLocationSharingStatusScreen(
    request: BloodRequest,
    strings: MbdStrings,
    liveSessionState: UiState<LiveLocationSession?>,
    onBack: () -> Unit,
    onUpdateRealCoordinates: (latitude: Double, longitude: Double, accuracyMeters: Double) -> Unit,
    onStopSharingLocation: () -> Unit,
    onOpenInteractiveMap: () -> Unit,
    onSessionExpired: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val session = (liveSessionState as? UiState.Success)?.data

    var hasPerm by remember { mutableStateOf(LiveLocationTracker.hasLocationPermission(context)) }
    var isGpsEnabled by remember { mutableStateOf(LiveLocationTracker.isLocationServicesEnabled(context)) }
    var isOnline by remember { mutableStateOf(LiveLocationTracker.isInternetAvailable(context)) }
    var latestAccuracy by remember { mutableFloatStateOf(session?.accuracyMeters?.toFloat() ?: 12f) }
    var lastUpdatedEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var remainingSeconds by remember { mutableLongStateOf(1800L) }

    // Subscribe to real Android device GPS updates while this screen or session is active
    LaunchedEffect(hasPerm, request.id) {
        if (hasPerm) {
            LiveLocationTracker.observeRealDeviceLocation(context, intervalMs = 5000L)
                .catch { /* Handled via permission/GPS state UI */ }
                .collect { realLoc ->
                    latestAccuracy = realLoc.accuracyMeters.toFloat()
                    lastUpdatedEpochMs = realLoc.timestampMs
                    onUpdateRealCoordinates(
                        realLoc.latitude,
                        realLoc.longitude,
                        realLoc.accuracyMeters
                    )
                }
        }
    }

    // Periodic health & expiry countdown ticker
    LaunchedEffect(session?.expiresAtEpochMs, request.liveSessionExpiresAt) {
        while (true) {
            isGpsEnabled = LiveLocationTracker.isLocationServicesEnabled(context)
            isOnline = LiveLocationTracker.isInternetAvailable(context)
            hasPerm = LiveLocationTracker.hasLocationPermission(context)

            val expiryMs = session?.expiresAtEpochMs?.takeIf { it > 0L }
                ?: request.liveSessionExpiresAt.takeIf { it > 0L }
                ?: (System.currentTimeMillis() + 1800_000L)
            val diffSec = (expiryMs - System.currentTimeMillis()) / 1000L
            remainingSeconds = diffSec.coerceAtLeast(0L)
            if (diffSec <= 0L) {
                onStopSharingLocation()
                onSessionExpired()
                break
            }
            delay(1000L)
        }
    }

    val formattedLastUpdate = remember(lastUpdatedEpochMs, session?.updatedAt) {
        val ts = session?.updatedAt?.toDate()?.time ?: lastUpdatedEpochMs
        SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(ts))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MbdCrimson,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("sharing_status_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = strings.liveLocationActiveBanner,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Active Live Sharing Hero Status Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.5.dp, MbdAvailableGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PulsingLiveDot(color = MbdAvailableGreen, sizeDp = 22)
                    Text(
                        text = strings.liveLocationActiveBanner,
                        color = MbdAvailableGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    val donorName = session?.authorizedDonorName?.ifBlank { request.authorizedDonorName }
                        ?: request.authorizedDonorName.ifBlank { "Authorized Donor" }
                    Text(
                        text = if (strings.isBangla) {
                            "রক্তদাতা $donorName আপনার রিয়েল-টাইম লোকেশন দেখতে পাচ্ছেন"
                        } else {
                            "Authorized donor $donorName can view your live location"
                        },
                        fontSize = 14.sp,
                        color = MbdTextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    HorizontalDivider(color = MbdBorderGray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.lastUpdatedLabel,
                                fontSize = 11.sp,
                                color = MbdTextSecondary
                            )
                            Text(
                                text = formattedLastUpdate,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MbdTextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.accuracyLabel,
                                fontSize = 11.sp,
                                color = MbdTextSecondary
                            )
                            Text(
                                text = "±${latestAccuracy.toInt()} m",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MbdAvailableGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (strings.isBangla) "বাকি সময়" else "Expires In",
                                fontSize = 11.sp,
                                color = MbdTextSecondary
                            )
                            val mins = remainingSeconds / 60
                            val secs = remainingSeconds % 60
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", mins, secs),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MbdCrimson
                            )
                        }
                    }
                }
            }

            // Connectivity / GPS Diagnostics Warnings
            if (!isOnline) {
                DiagnosticWarningBanner(
                    icon = Icons.Default.WifiOff,
                    title = if (strings.isBangla) "ইন্টারনেট সংযোগ বিচ্ছিন্ন" else "Internet Disconnected",
                    subtitle = if (strings.isBangla) {
                        "ইন্টারনেট সংযোগ ফিরে এলে স্বয়ংক্রিয়ভাবে পুনরায় লাইভ লোকেশন সিঙ্ক হবে।"
                    } else {
                        "Live location updates will automatically sync as soon as internet reconnects."
                    }
                )
            }
            if (!isGpsEnabled) {
                DiagnosticWarningBanner(
                    icon = Icons.Default.GpsOff,
                    title = if (strings.isBangla) "GPS বন্ধ রয়েছে" else "GPS is Disabled",
                    subtitle = if (strings.isBangla) {
                        "নির্ভুল লাইভ লোকেশন পাঠাতে ফোনের GPS চালু করুন।"
                    } else {
                        "Please enable GPS for real-time coordinate updates."
                    }
                )
            }

            // Button to Preview Interactive Live Map
            OutlinedButton(
                onClick = onOpenInteractiveMap,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MbdCrimson),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("status_preview_map_button")
            ) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = MbdCrimson)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.viewLiveLocationBtn,
                    color = MbdCrimson,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Always-Visible "Stop Sharing Location" (“লোকেশন শেয়ার বন্ধ করুন”) Button
            Button(
                onClick = onStopSharingLocation,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MbdCrimson,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("status_stop_sharing_location_button")
            ) {
                Icon(imageVector = Icons.Default.StopCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.stopSharingLocationBtn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * 4. Donor's Live Map View Screen (“লাইভ লোকেশন দেখুন”)
 * Interactive map displaying the requester's live-updating location marker, accuracy radius,
 * last location update time (“সর্বশেষ আপডেট”), and "Open Directions" (“দিকনির্দেশনা দেখুন”) button.
 */
@Composable
fun DonorLiveMapViewScreen(
    request: BloodRequest,
    strings: MbdStrings,
    currentUserId: String,
    liveSessionState: UiState<LiveLocationSession?>,
    onBack: () -> Unit,
    onStopSharingLocation: () -> Unit,
    onSessionEndedOrExpired: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val session = (liveSessionState as? UiState.Success)?.data
    val isRequester = request.userId == currentUserId

    // Check if session has ended or expired
    LaunchedEffect(liveSessionState, request.liveSessionActive, request.status) {
        if (liveSessionState is UiState.Success) {
            val s = liveSessionState.data
            val expired = s != null && s.expiresAtEpochMs > 0 && System.currentTimeMillis() > s.expiresAtEpochMs
            if (request.status != "open" || expired || (s != null && !s.isActive)) {
                onSessionEndedOrExpired()
            }
        }
    }

    val lat = session?.latitude ?: 23.8069
    val lng = session?.longitude ?: 90.3687
    val accuracy = session?.accuracyMeters ?: 12.0
    val updatedTimeText = remember(session?.updatedAt) {
        val date = session?.updatedAt?.toDate() ?: Date()
        SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(date)
    }

    var mapZoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Red Top App Bar
        Surface(
            color = MbdCrimson,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("donor_live_map_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.viewLiveLocationBtn,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${request.bloodGroup} • ${request.hospitalName}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Interactive Map Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            InteractiveMirpurLiveMapCanvas(
                latitude = lat,
                longitude = lng,
                accuracyMeters = accuracy,
                bloodGroup = request.bloodGroup,
                requesterName = request.requesterName.ifBlank { request.patientName },
                hospitalName = request.hospitalName,
                zoom = mapZoom,
                panOffset = panOffset,
                onPanZoom = { pan, zoomChange ->
                    mapZoom = (mapZoom * zoomChange).coerceIn(0.75f, 3.0f)
                    panOffset += pan
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("interactive_live_location_map")
            )

            // Top Floating Live Status Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulsingLiveDot(color = MbdAvailableGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${strings.lastUpdatedLabel}: $updatedTimeText • ±${accuracy.toInt()}m",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MbdTextPrimary
                    )
                }
            }

            // Right Map Controls (Zoom In, Zoom Out, Re-Center)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = { mapZoom = (mapZoom * 1.25f).coerceAtMost(3.0f) }) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = MbdTextPrimary)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = { mapZoom = (mapZoom / 1.25f).coerceAtLeast(0.75f) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = MbdTextPrimary)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = {
                            mapZoom = 1f
                            panOffset = Offset.Zero
                        }
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Re-center", tint = MbdCrimson)
                    }
                }
            }
        }

        // Bottom Live Location Details & Directions Sheet
        Surface(
            color = Color.White,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BloodDropBadgeIcon(modifier = Modifier.size(46.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${request.patientName} (${request.bloodGroup})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MbdTextPrimary
                        )
                        Text(
                            text = "${request.hospitalName}, ${request.location}",
                            fontSize = 13.sp,
                            color = MbdTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${strings.lastUpdatedLabel}: $updatedTimeText (${String.format(Locale.US, "%.4f, %.4f", lat, lng)})",
                            fontSize = 11.sp,
                            color = MbdAvailableGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = { dialPhoneNumber(context, request.contactNumber) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MbdCallGreen.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = strings.callBtn,
                            tint = MbdCallGreen
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            LiveLocationTracker.openDirectionsToRequester(
                                context = context,
                                latitude = lat,
                                longitude = lng,
                                label = "${request.hospitalName} (${request.patientName})"
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MbdCrimson,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_directions_button")
                    ) {
                        Icon(imageVector = Icons.Default.Directions, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.openDirectionsBtn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (isRequester) {
                        OutlinedButton(
                            onClick = onStopSharingLocation,
                            border = BorderStroke(1.5.dp, MbdCrimson),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("map_stop_sharing_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.StopCircle,
                                contentDescription = null,
                                tint = MbdCrimson
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.stopSharingLocationBtn,
                                color = MbdCrimson,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Location Sharing Ended or Expired Screen
 */
@Composable
fun LocationSharingEndedScreen(
    strings: MbdStrings,
    onBackToRequestDetails: () -> Unit,
    onBackToHome: () -> Unit
) {
    BackHandler { onBackToRequestDetails() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MbdCrimson,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToRequestDetails) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = strings.locationEndedTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdBorderGray),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MbdCrimsonLight,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = MbdCrimson,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Text(
                        text = strings.locationEndedTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = MbdTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = strings.locationEndedSub,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MbdTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = onBackToRequestDetails,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ended_back_to_details_button")
                    ) {
                        Text(
                            text = strings.requestDetailsTitle,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onBackToHome,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("ended_back_to_home_button")
                    ) {
                        Text(text = strings.navHome, color = MbdTextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveMirpurLiveMapCanvas(
    latitude: Double,
    longitude: Double,
    accuracyMeters: Double,
    bloodGroup: String,
    requesterName: String,
    hospitalName: String,
    zoom: Float,
    panOffset: Offset,
    onPanZoom: (pan: Offset, zoomChange: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liveMapPulse")
    val pulseRadiusMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    Canvas(
        modifier = modifier
            .background(Color(0xFFEEF2F6))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    onPanZoom(pan, zoomChange)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f + panOffset.x
        val centerY = h * 0.5f + panOffset.y

        // 1. Subtle park & sector blocks
        drawRect(
            color = Color(0xFFDFF0DF),
            topLeft = Offset(centerX - 180.dp.toPx() * zoom, centerY - 140.dp.toPx() * zoom),
            size = androidx.compose.ui.geometry.Size(110.dp.toPx() * zoom, 85.dp.toPx() * zoom)
        )
        drawRect(
            color = Color(0xFFE3ECFA),
            topLeft = Offset(centerX + 50.dp.toPx() * zoom, centerY + 60.dp.toPx() * zoom),
            size = androidx.compose.ui.geometry.Size(130.dp.toPx() * zoom, 90.dp.toPx() * zoom)
        )

        // 2. Road Grid Lines representing Mirpur Begum Rokeya Sarani & Mirpur Road
        val gridSpacing = 48.dp.toPx() * zoom
        for (i in -8..8) {
            val x = centerX + i * gridSpacing
            drawLine(
                color = Color.White,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = if (i == 0) 8.dp.toPx() * zoom else 2.5.dp.toPx()
            )
            val y = centerY + i * gridSpacing
            drawLine(
                color = Color.White,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = if (i == 0) 8.dp.toPx() * zoom else 2.5.dp.toPx()
            )
        }

        // Major arterial highway (Begum Rokeya Sarani)
        val highwayPath = Path().apply {
            moveTo(centerX - 160.dp.toPx() * zoom, 0f)
            lineTo(centerX, centerY)
            lineTo(centerX + 120.dp.toPx() * zoom, h)
        }
        drawPath(
            path = highwayPath,
            color = Color(0xFFFFE082),
            style = Stroke(width = 7.dp.toPx() * zoom, cap = StrokeCap.Round)
        )

        // 3. Sector Labels around Mirpur
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#5F6672")
            textSize = 11.dp.toPx()
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        drawContext.canvas.nativeCanvas.apply {
            drawText("Mirpur-10 Goalchattar", centerX + 14.dp.toPx(), centerY - 14.dp.toPx(), paint)
            drawText("Mirpur-2 (Heart Foundation)", centerX - 155.dp.toPx() * zoom, centerY - 35.dp.toPx() * zoom, paint)
            drawText("Mirpur-11 / Pallabi", centerX - 60.dp.toPx() * zoom, centerY - 115.dp.toPx() * zoom, paint)
            drawText("Kazipara / Shewrapara", centerX + 35.dp.toPx() * zoom, centerY + 95.dp.toPx() * zoom, paint)
        }

        // 4. Accuracy Radius Circle + Pulsing Ring at Requester's Live Position
        val baseAccuracyPx = (accuracyMeters.toFloat().coerceIn(8f, 120f) * 1.4f).dp.toPx() * zoom
        drawCircle(
            color = MbdCrimson.copy(alpha = 0.14f),
            radius = baseAccuracyPx,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = MbdCrimson.copy(alpha = 0.35f),
            radius = baseAccuracyPx,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = MbdCrimson.copy(alpha = 0.25f * (1.5f - pulseRadiusMultiplier).coerceIn(0f, 1f)),
            radius = 34.dp.toPx() * pulseRadiusMultiplier * zoom,
            center = Offset(centerX, centerY)
        )

        // 5. Live Requester Pin Marker
        drawCircle(
            color = Color.White,
            radius = 18.dp.toPx(),
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = MbdCrimson,
            radius = 14.dp.toPx(),
            center = Offset(centerX, centerY)
        )

        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 11.dp.toPx()
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        drawContext.canvas.nativeCanvas.drawText(
            bloodGroup,
            centerX,
            centerY + 4.dp.toPx(),
            labelPaint
        )

        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#C61A20")
            textSize = 12.dp.toPx()
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        drawContext.canvas.nativeCanvas.drawText(
            "$requesterName • $hospitalName",
            centerX,
            centerY - 26.dp.toPx(),
            titlePaint
        )
    }
}

@Composable
fun PulsingLiveDot(color: Color, sizeDp: Int = 12) {
    val transition = rememberInfiniteTransition(label = "liveDot")
    val scale by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    Box(
        modifier = Modifier.size((sizeDp + 6).dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((sizeDp * scale).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f))
        )
        Box(
            modifier = Modifier
                .size((sizeDp * 0.65f).dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun RequestInfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MbdTextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MbdTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DiagnosticWarningBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
        border = BorderStroke(1.dp, Color(0xFFFF9800)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFE65100))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MbdTextPrimary
                )
            }
        }
    }
}

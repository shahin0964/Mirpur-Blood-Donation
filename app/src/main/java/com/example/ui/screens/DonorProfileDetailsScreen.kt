package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BloodRequest
import com.example.data.model.DEFAULT_ELIGIBILITY_RULES
import com.example.data.model.DonationRecord
import com.example.data.model.DonorAchievementBadge
import com.example.data.model.DonorProfile
import com.example.data.model.EligibilityRule
import com.example.data.model.computeDonorAchievementBadges
import com.example.ui.components.AdminCorrectDonorDatesDialog
import com.example.ui.components.AdminEligibilityRulesDialog
import com.example.ui.components.AvailabilityStatusBadge
import com.example.ui.components.DonationCountdownEngine
import com.example.ui.components.DonationHistoryTimelineSection
import com.example.ui.components.LiveDonationCountdownCard
import com.example.ui.components.RecordBloodDonationDialog
import com.example.ui.i18n.MbdStrings
import com.example.ui.theme.MbdAvailableGreen
import com.example.ui.theme.MbdBorderGray
import com.example.ui.theme.MbdCallGreen
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdCrimsonDark
import com.example.ui.theme.MbdCrimsonLight
import com.example.ui.theme.MbdSoftPinkAlert
import com.example.ui.theme.MbdSurface
import com.example.ui.theme.MbdTextPrimary
import com.example.ui.theme.MbdTextSecondary
import com.example.ui.util.LiveLocationTracker
import com.example.ui.util.dialPhoneNumber
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DonorProfileDetailsScreen(
    donorState: UiState<DonorProfile?>,
    donorDonationsState: UiState<List<DonationRecord>> = UiState.Success(emptyList()),
    eligibilityRules: Map<String, EligibilityRule> = DEFAULT_ELIGIBILITY_RULES,
    strings: MbdStrings,
    currentUserId: String,
    isAdminUser: Boolean = false,
    isSaving: Boolean = false,
    authorizedLiveRequest: BloodRequest?,
    onBack: () -> Unit,
    onRefreshDonor: () -> Unit,
    onSendBloodRequestToDonor: (DonorProfile) -> Unit,
    onViewAuthorizedLiveLocation: (BloodRequest) -> Unit,
    onRecordBloodDonation: (
        donationDate: String,
        donationTime: String,
        donationType: String,
        customDonationTypeName: String,
        procedureConfirmation: String,
        eligibilityIntervalDays: Long,
        ruleVersion: String,
        hospitalName: String,
        location: String,
        recipientName: String,
        bloodGroup: String,
        notes: String,
        verificationStatus: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onAdminCorrectDonorDates: (
        donor: DonorProfile,
        correctedLastDate: String,
        correctedNextDate: String,
        correctedIntervalDays: Long,
        correctedTotalDonations: Long
    ) -> Unit = { _, _, _, _, _ -> },
    onSaveEligibilityRule: (EligibilityRule) -> Unit = {},
    onApproveOrUpdateDonationRecord: (DonationRecord, String) -> Unit = { _, _ -> },
    onDeleteOwnDonationRecord: (String) -> Unit = {},
    onReportDonor: (donorId: String, reason: String) -> Unit = { _, _ -> }
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val isOnline = remember { LiveLocationTracker.isInternetAvailable(context) }
    var showExpandedAreaMap by remember { mutableStateOf(false) }
    var showRecordDonationDialog by remember { mutableStateOf(false) }
    var showAdminCorrectDatesDialog by remember { mutableStateOf(false) }
    var showAdminRulesDialog by remember { mutableStateOf(false) }
    var showReportDonorDialog by remember { mutableStateOf(false) }
    var reportDonorReasonText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar with Back & Refresh buttons
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
                    modifier = Modifier.testTag("donor_profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = strings.donorProfileDetailsTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                )
                IconButton(
                    onClick = onRefreshDonor,
                    modifier = Modifier.testTag("donor_profile_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = strings.refreshProfileTooltip,
                        tint = Color.White
                    )
                }
            }
        }

        // Offline diagnostic banner if device has no network
        if (!isOnline) {
            Surface(
                color = Color(0xFFFFF8E1),
                border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (strings.isBangla) {
                            "ইন্টারনেট সংযোগ নেই • ক্যাশকৃত রক্তদাতার তথ্য দেখানো হচ্ছে"
                        } else {
                            "Offline mode • Showing cached donor profile information"
                        },
                        fontSize = 12.sp,
                        color = MbdTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        when (donorState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MbdCrimson)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (strings.isBangla) "রক্তদাতার প্রোফাইল লোড হচ্ছে..." else "Loading donor profile...",
                            color = MbdTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MbdSurface),
                        border = BorderStroke(1.dp, MbdBorderGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BloodDropBadgeIcon(modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = donorState.message,
                                color = MbdCrimson,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRefreshDonor,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson)
                            ) {
                                Text(text = strings.refreshProfileTooltip)
                            }
                        }
                    }
                }
            }

            is UiState.Success -> {
                val donor = donorState.data
                if (donor == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MbdSurface),
                            border = BorderStroke(1.dp, MbdBorderGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                BloodDropBadgeIcon(modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (strings.isBangla) {
                                        "রক্তদাতার প্রোফাইল খুঁজে পাওয়া যায়নি বা গোপন রাখা হয়েছে।"
                                    } else {
                                        "Donor profile not found or set to private."
                                    },
                                    color = MbdTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onBack,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson)
                                ) {
                                    Text(if (strings.isBangla) "রক্তদাতা তালিকায় ফিরে যান" else "Back to Donor List")
                                }
                            }
                        }
                    }
                } else {
                    val isSelf = donor.userId == currentUserId || donor.id == currentUserId
                    val canSeePhone = donor.showPhonePublicly || isSelf
                    val canSeeLocationArea = donor.shareLocationArea || isSelf || authorizedLiveRequest != null
                    val rawDonorId = donor.id.ifBlank { donor.userId }
                    val formattedDonorId = remember(rawDonorId) {
                        if (rawDonorId.isNotBlank()) {
                            "#MBD-${rawDonorId.takeLast(6).uppercase()}"
                        } else {
                            ""
                        }
                    }

                    val registrationDateText = remember(donor.createdAt, strings.isBangla) {
                        val date = donor.createdAt?.toDate()
                        if (date != null) {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                        } else {
                            if (strings.isBangla) "যাচাইকৃত সদস্য (মিরপুর)" else "Verified Community Donor"
                        }
                    }

                    val eligibilityInfo = remember(
                        donor.lastDonationDate,
                        donor.eligibilityIntervalDays,
                        donor.donationType,
                        strings.isBangla
                    ) {
                        calculateNextEligibleDate(
                            donor.lastDonationDate,
                            donor.eligibilityIntervalDays,
                            donor.donationType,
                            strings.isBangla
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Under Admin Review Warning Banner if reported
                        if (donor.isReported) {
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
                                        if (donor.reportReason.isNotBlank()) {
                                            Text(
                                                text = donor.reportReason,
                                                color = MbdTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================================
                        // A. PROFILE HEADER CARD
                        // =========================================================================
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MbdSurface),
                            border = BorderStroke(1.dp, MbdBorderGray),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("donor_profile_header_card")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Subtle Crimson Gradient Banner + Overlapping Avatar & Blood Group Badge
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    MbdCrimsonLight.copy(alpha = 0.85f),
                                                    Color.White
                                                )
                                            )
                                        )
                                        .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                                ) {
                                    // Prominent Red Blood Group Badge at Top-End
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MbdCrimson,
                                        shadowElevation = 3.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .testTag("donor_profile_blood_group_badge")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WaterDrop,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = donor.bloodGroup,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 17.sp
                                            )
                                        }
                                    }

                                    // Donor ID Pill at Top-Start if available
                                    if (formattedDonorId.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color.White.copy(alpha = 0.9f),
                                            border = BorderStroke(1.dp, MbdCrimson.copy(alpha = 0.25f)),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Badge,
                                                    contentDescription = null,
                                                    tint = MbdCrimson,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = formattedDonorId,
                                                    color = MbdTextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    // Circular Donor Avatar with Green Availability Dot
                                    Box(
                                        modifier = Modifier
                                            .size(88.dp)
                                            .align(Alignment.Center)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            border = BorderStroke(3.dp, MbdCrimson),
                                            shadowElevation = 4.dp,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MbdCrimsonLight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val initials = donor.fullName
                                                    .trim()
                                                    .split(" ")
                                                    .filter { it.isNotBlank() }
                                                    .take(2)
                                                    .joinToString("") { it.first().uppercase() }
                                                    .ifEmpty { "D" }
                                                Text(
                                                    text = initials,
                                                    color = MbdCrimson,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 28.sp
                                                )
                                            }
                                        }

                                        // Availability indicator dot on bottom-right of avatar
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .align(Alignment.BottomEnd)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(3.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (donor.isAvailable && (eligibilityInfo == null || eligibilityInfo.isEligibleNow)) MbdAvailableGreen else Color.Gray
                                                    )
                                            )
                                        }
                                    }
                                }

                                // Full Name + Verified Indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = donor.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = MbdTextPrimary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .testTag("donor_profile_full_name")
                                    )
                                    if (donor.isPhoneVerified && donor.isAdminReviewed) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified Donor",
                                            tint = MbdAvailableGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // 3-State Availability Status Pill
                                AvailabilityStatusBadge(
                                    availabilityStatus = donor.resolvedAvailabilityStatus(),
                                    isAvailableFallback = donor.isAvailable,
                                    strings = strings,
                                    compact = false,
                                    forceTemporarilyUnavailable = eligibilityInfo != null && !eligibilityInfo.isEligibleNow
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Summary Metrics Row (Blood Group, Total Donations, Area)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileQuickStat(
                                        value = donor.bloodGroup,
                                        label = strings.bloodGroupLabel,
                                        valueColor = MbdCrimson,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(28.dp)
                                            .background(MbdBorderGray.copy(alpha = 0.4f))
                                    )
                                    ProfileQuickStat(
                                        value = if (strings.isBangla) "${donor.totalDonations} বার" else "${donor.totalDonations}",
                                        label = strings.totalDonationsLabel,
                                        valueColor = MbdTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(28.dp)
                                            .background(MbdBorderGray.copy(alpha = 0.4f))
                                    )
                                    ProfileQuickStat(
                                        value = donor.area.substringBefore(",").ifBlank { "Mirpur" },
                                        label = strings.locationLabel,
                                        valueColor = MbdAvailableGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // =========================================================================
                        // E. PRIMARY ACTION BUTTONS (Call, Contact SMS, Send Blood Request, View Shared Location)
                        // =========================================================================
                        if (!isSelf) {
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
                                    val isDonorReady = donor.isAvailable && (eligibilityInfo == null || eligibilityInfo.isEligibleNow)

                                    if (!isDonorReady) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFFFF3E0),
                                            border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = Color(0xFFE65100),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (strings.isBangla) {
                                                        "এই রক্তদাতা বর্তমানে রক্তদানের জন্য প্রস্তুত নন। প্রস্তুত হওয়ার পর যোগাযোগ ও আবেদন সক্রিয় হবে।"
                                                    } else {
                                                        "This donor is currently unavailable for donation. Contact and request buttons will be enabled when ready."
                                                    },
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF5D4037),
                                                    fontWeight = FontWeight.Medium,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }

                                    if (!canSeePhone) {
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
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = MbdCrimson,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = strings.phoneHiddenNotice,
                                                    fontSize = 12.sp,
                                                    color = MbdCrimson,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        // Direct in-app blood request action protecting phone privacy
                                        Button(
                                            onClick = { onSendBloodRequestToDonor(donor) },
                                            enabled = isDonorReady,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("profile_action_send_request_private_donor")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = strings.sendBloodRequestBtn,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // 1. Call Donor Button (Green)
                                            Button(
                                                onClick = {
                                                    if (donor.phoneNumber.isNotBlank()) {
                                                        dialPhoneNumber(context, donor.phoneNumber)
                                                    }
                                                },
                                                enabled = isDonorReady && donor.phoneNumber.isNotBlank(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MbdCallGreen,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(50.dp)
                                                    .testTag("profile_action_call_donor")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = strings.callBtn,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            // 2. Direct Message / SMS Button
                                            OutlinedButton(
                                                onClick = { openSmsToDonor(context, donor, strings.isBangla) },
                                                enabled = isDonorReady && donor.phoneNumber.isNotBlank(),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, MbdCallGreen),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MbdCallGreen),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(50.dp)
                                                    .testTag("profile_action_sms_donor")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Message,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = strings.contactDonorBtn,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // 3. Send Blood Request Button (Primary Crimson Action — associates request with selected donor)
                                        Button(
                                            onClick = { onSendBloodRequestToDonor(donor) },
                                            enabled = isDonorReady,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MbdCrimson,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("profile_action_send_blood_request")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${strings.sendBloodRequestBtn} (${donor.bloodGroup})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    // 4. View Shared Location Button (when authorized or area shared)
                                    if (authorizedLiveRequest != null) {
                                        Button(
                                            onClick = { onViewAuthorizedLiveLocation(authorizedLiveRequest) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MbdAvailableGreen,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("profile_action_view_shared_location")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = strings.viewSharedLocationBtn,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else if (canSeeLocationArea) {
                                        OutlinedButton(
                                            onClick = { showExpandedAreaMap = !showExpandedAreaMap },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MbdAvailableGreen),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MbdAvailableGreen),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                                .testTag("profile_action_view_shared_location")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Map,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = strings.viewSharedLocationBtn,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Info bar for own profile
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MbdCrimsonLight.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MbdCrimson.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MbdCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (strings.isBangla) "এটি আপনার নিজস্ব প্রোফাইল। নিজের জন্য আবেদন সক্রিয় নয়।" else "This is your own profile. Self-requests are disabled.",
                                        fontSize = 13.sp,
                                        color = MbdCrimson,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }


                        // =========================================================================
                        // B. PERSONAL INFORMATION SECTION
                        // =========================================================================
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MbdSurface),
                            border = BorderStroke(1.dp, MbdBorderGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("donor_personal_info_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SectionHeaderRow(
                                    icon = Icons.Default.Person,
                                    title = strings.personalInfoSection
                                )

                                HorizontalDivider(color = MbdBorderGray.copy(alpha = 0.6f))

                                ProfileInfoRow(
                                    icon = Icons.Default.Person,
                                    label = strings.fullNameLabel,
                                    value = donor.fullName
                                )

                                ProfileInfoRow(
                                    icon = Icons.Default.Bloodtype,
                                    label = strings.bloodGroupLabel,
                                    value = donor.bloodGroup,
                                    valueColor = MbdCrimson,
                                    isValueBold = true
                                )

                                ProfileInfoRow(
                                    icon = Icons.Default.LocationOn,
                                    label = strings.areaLabel,
                                    value = donor.area.ifBlank { "Mirpur, Dhaka" }
                                )

                                if (donor.address.isNotBlank() && donor.address != donor.area) {
                                    ProfileInfoRow(
                                        icon = Icons.Default.Map,
                                        label = strings.addressLabel,
                                        value = donor.address
                                    )
                                }

                                // Phone Number visible ONLY according to donor's privacy settings
                                ProfileInfoRow(
                                    icon = if (canSeePhone) Icons.Default.Phone else Icons.Default.Lock,
                                    label = strings.contactNumberLabel,
                                    value = if (canSeePhone && donor.phoneNumber.isNotBlank()) {
                                        donor.phoneNumber
                                    } else {
                                        if (strings.isBangla) {
                                            "গোপনীয় (রক্তদাতার প্রাইভেসি সেটিংস দ্বারা সুরক্ষিত)"
                                        } else {
                                            "Hidden by donor's privacy settings"
                                        }
                                    },
                                    valueColor = if (canSeePhone) MbdTextPrimary else MbdTextSecondary
                                )

                                // Age — displayed ONLY if provided and appropriate
                                if (donor.age.isNotBlank()) {
                                    ProfileInfoRow(
                                        icon = Icons.Default.CalendarMonth,
                                        label = strings.ageLabel,
                                        value = if (strings.isBangla) "${donor.age} বছর" else "${donor.age} years"
                                    )
                                }

                                // Gender — displayed ONLY if donor has chosen to share it
                                if (donor.gender.isNotBlank()) {
                                    ProfileInfoRow(
                                        icon = Icons.Default.Verified,
                                        label = strings.genderLabel,
                                        value = donor.gender
                                    )
                                }

                                if (formattedDonorId.isNotBlank()) {
                                    ProfileInfoRow(
                                        icon = Icons.Default.Badge,
                                        label = strings.donorIdLabel,
                                        value = formattedDonorId
                                    )
                                }
                            }
                        }

                        // =========================================================================
                        // C1. LIVE DONATION COUNTDOWN TIMER WIDGET (1-Second Real-Time Ticker)
                        // =========================================================================
                        val donorRecords = (donorDonationsState as? UiState.Success)?.data.orEmpty()
                        val pendingRecordsCount = donorRecords.count { it.verificationStatus == "pending" }
                        val canSeeDonationHistory = donor.showDonationHistoryPublicly || isSelf
                        val multiEval = remember(donorRecords, eligibilityRules) {
                            DonationCountdownEngine.evaluateMultiRecordEligibility(donorRecords, eligibilityRules)
                        }
                        val countsByType = remember(donorRecords, donor) {
                            DonationCountdownEngine.computeCountsByType(donorRecords, donor)
                        }

                        LiveDonationCountdownCard(
                            strings = strings,
                            lastDonationDate = donor.lastDonationDate,
                            lastDonationTime = donor.lastDonationTime.ifBlank { multiEval.lastDonationTime },
                            nextEligibleDate = donor.nextEligibleDate,
                            eligibilityIntervalDays = donor.eligibilityIntervalDays,
                            donationType = donor.donationType,
                            customDonationTypeName = donor.customDonationTypeName.ifBlank { multiEval.customDonationTypeName },
                            ruleVersion = donor.ruleVersion.ifBlank { multiEval.governingRuleVersion },
                            totalConfirmedDonations = donor.totalDonations,
                            pendingDonationsCount = pendingRecordsCount,
                            countsByType = countsByType,
                            governingRuleExplanation = if (strings.isBangla) {
                                multiEval.governingExplanationBn
                            } else {
                                multiEval.governingExplanationEn
                            },
                            isPublicProfileHiddenByPrivacy = !canSeeDonationHistory,
                            onRecordDonationClick = if (isSelf) {
                                { showRecordDonationDialog = true }
                            } else {
                                null
                            },
                            onAdminCorrectDatesClick = if (isAdminUser || isSelf) {
                                { showAdminCorrectDatesDialog = true }
                            } else {
                                null
                            },
                            onAdminConfigureRulesClick = if (isAdminUser) {
                                { showAdminRulesDialog = true }
                            } else {
                                null
                            }
                        )

                        // =========================================================================
                        // C2. DONATION INFORMATION SECTION
                        // =========================================================================
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MbdSurface),
                            border = BorderStroke(1.dp, MbdBorderGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("donor_donation_info_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SectionHeaderRow(
                                    icon = Icons.Default.WaterDrop,
                                    title = strings.donationInfoSection
                                )

                                HorizontalDivider(color = MbdBorderGray.copy(alpha = 0.6f))

                                ProfileInfoRow(
                                    icon = Icons.Default.History,
                                    label = strings.totalDonationsLabel,
                                    value = if (strings.isBangla) {
                                        "${donor.totalDonations} বার রক্তদান সম্পন্ন"
                                    } else {
                                        "${donor.totalDonations} time(s)"
                                    },
                                    isValueBold = true
                                )

                                ProfileInfoRow(
                                    icon = Icons.Default.CalendarMonth,
                                    label = strings.lastDonationDateLabel.substringBefore(" ("),
                                    value = if (canSeeDonationHistory) {
                                        donor.lastDonationDate.ifBlank {
                                            strings.noDonationHistoryText
                                        }
                                    } else {
                                        if (strings.isBangla) "গোপনীয় (রক্তদাতার প্রাইভেসি সেটিংস)" else "Hidden by privacy settings"
                                    }
                                )

                                ProfileInfoRow(
                                    icon = Icons.Default.Verified,
                                    label = strings.registrationDateLabel,
                                    value = registrationDateText
                                )

                                val resolvedAvailLabel = when (donor.resolvedAvailabilityStatus()) {
                                    "available" -> strings.availabilityAvailableToContact
                                    "not_accepting" -> strings.availabilityNotAcceptingRequests
                                    else -> strings.availabilityTemporarilyUnavailable
                                }

                                ProfileInfoRow(
                                    icon = Icons.Default.CheckCircle,
                                    label = strings.currentAvailabilityLabel,
                                    value = resolvedAvailLabel,
                                    valueColor = if (donor.resolvedAvailabilityStatus() == "available") {
                                        MbdAvailableGreen
                                    } else if (donor.resolvedAvailabilityStatus() == "not_accepting") {
                                        MbdCrimsonDark
                                    } else {
                                        Color(0xFFE65100)
                                    },
                                    isValueBold = true
                                )

                                // Next Eligible Donation Date — shown when supported by reliable YYYY-MM-DD donation history
                                if (canSeeDonationHistory && eligibilityInfo != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (eligibilityInfo.isEligibleNow) {
                                            MbdAvailableGreen.copy(alpha = 0.1f)
                                        } else {
                                            MbdCrimsonLight.copy(alpha = 0.65f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (eligibilityInfo.isEligibleNow) {
                                                MbdAvailableGreen.copy(alpha = 0.35f)
                                            } else {
                                                MbdCrimson.copy(alpha = 0.25f)
                                            }
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EventAvailable,
                                                contentDescription = null,
                                                tint = if (eligibilityInfo.isEligibleNow) MbdAvailableGreen else MbdCrimson,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = strings.nextEligibleDateLabel,
                                                    fontSize = 11.sp,
                                                    color = MbdTextSecondary
                                                )
                                                Text(
                                                    text = donor.nextEligibleDate.ifBlank { eligibilityInfo.formattedDate },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MbdTextPrimary
                                                )
                                                Text(
                                                    text = eligibilityInfo.statusNote,
                                                    fontSize = 11.sp,
                                                    color = if (eligibilityInfo.isEligibleNow) MbdAvailableGreen else MbdCrimson,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================================
                        // C2. DONOR ACHIEVEMENT BADGES SECTION
                        // =========================================================================
                        val verifiedCount = remember(donorRecords, donor.totalDonations) {
                            val verifiedFromRecords = donorRecords.count {
                                it.verificationStatus == "approved" || it.verificationStatus == "verified"
                            }.toLong()
                            maxOf(verifiedFromRecords, donor.totalDonations)
                        }
                        val achievementBadges = remember(donor, verifiedCount) {
                            computeDonorAchievementBadges(donor, verifiedCount)
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MbdSurface),
                            border = BorderStroke(1.dp, MbdBorderGray),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("donor_achievements_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MbdCrimsonLight,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = MbdCrimson,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.donorAchievementsTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MbdTextPrimary
                                        )
                                        Text(
                                            text = if (strings.isBangla) "যাচাইকৃত রক্তদান: $verifiedCount বার" else "Verified Donations: $verifiedCount",
                                            fontSize = 12.sp,
                                            color = MbdAvailableGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = strings.achievementsNotice,
                                        fontSize = 11.sp,
                                        color = MbdTextSecondary,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    achievementBadges.forEach { badge ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (badge.isUnlocked) {
                                                MbdAvailableGreen.copy(alpha = 0.08f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (badge.isUnlocked) MbdAvailableGreen.copy(alpha = 0.4f) else MbdBorderGray
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = badge.emoji,
                                                    fontSize = 24.sp,
                                                    modifier = Modifier.padding(end = 12.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (strings.isBangla) badge.titleBn else badge.titleEn,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (badge.isUnlocked) MbdTextPrimary else MbdTextSecondary
                                                    )
                                                    Text(
                                                        text = if (strings.isBangla) badge.descBn else badge.descEn,
                                                        fontSize = 11.sp,
                                                        color = MbdTextSecondary,
                                                        lineHeight = 14.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = if (badge.isUnlocked) MbdAvailableGreen else Color.LightGray.copy(alpha = 0.4f)
                                                ) {
                                                    Text(
                                                        text = if (badge.isUnlocked) {
                                                            if (strings.isBangla) "অর্জিত" else "Unlocked"
                                                        } else {
                                                            if (strings.isBangla) "লকড" else "Locked"
                                                        },
                                                        color = if (badge.isUnlocked) Color.White else Color.DarkGray,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================================
                        // C3. DONATION HISTORY TIMELINE SECTION
                        // =========================================================================
                        if (canSeeDonationHistory) {
                            val timelineItems = remember(donorRecords, donor) {
                                if (donorRecords.isNotEmpty()) {
                                    donorRecords
                                } else if (donor.lastDonationDate.isNotBlank()) {
                                    listOf(
                                        DonationRecord(
                                            id = "profile_summary_${donor.id}",
                                            userId = donor.userId.ifBlank { donor.id },
                                            donorName = donor.fullName,
                                            hospitalName = "",
                                            location = donor.area,
                                            bloodGroup = donor.bloodGroup,
                                            donationDate = donor.lastDonationDate,
                                            donationType = donor.donationType,
                                            eligibilityIntervalDays = donor.eligibilityIntervalDays,
                                            nextEligibleDate = donor.nextEligibleDate,
                                            verificationStatus = "verified",
                                            visibility = "public"
                                        )
                                    )
                                } else {
                                    emptyList()
                                }
                            }

                            DonationHistoryTimelineSection(
                                records = timelineItems,
                                strings = strings,
                                canEditOrDeleteOwn = isSelf,
                                canAdminVerifyOrCorrect = isAdminUser,
                                onRecordDonationClick = if (isSelf) {
                                    { showRecordDonationDialog = true }
                                } else {
                                    null
                                },
                                onApproveRecord = { rec ->
                                    onApproveOrUpdateDonationRecord(rec, "verified")
                                },
                                onMarkPendingRecord = { rec ->
                                    onApproveOrUpdateDonationRecord(rec, "pending")
                                },
                                onDeleteRecord = { id ->
                                    if (!id.startsWith("profile_summary_")) {
                                        onDeleteOwnDonationRecord(id)
                                    }
                                }
                            )
                        }

                        // =========================================================================
                        // D. LOCATION SECTION (Shared Area, Map Preview when authorized, Directions, Privacy)
                        // =========================================================================
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MbdSurface),
                            border = BorderStroke(1.dp, MbdBorderGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("donor_location_section_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SectionHeaderRow(
                                    icon = Icons.Default.LocationOn,
                                    title = strings.donorLocationSection
                                )

                                HorizontalDivider(color = MbdBorderGray.copy(alpha = 0.6f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MbdCrimsonLight,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = MbdCrimson,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = donor.area.ifBlank { "Mirpur, Dhaka" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MbdTextPrimary
                                        )
                                        Text(
                                            text = if (canSeeLocationArea) {
                                                donor.address.ifBlank { donor.area }
                                            } else {
                                                if (strings.isBangla) {
                                                    "শুধুমাত্র সাধারণ এলাকা প্রদর্শিত হচ্ছে"
                                                } else {
                                                    "Approximate community area only"
                                                }
                                            },
                                            fontSize = 12.sp,
                                            color = MbdTextSecondary
                                        )
                                    }
                                }

                                // Approximate Area Map Preview when donor shares location area
                                AnimatedVisibility(visible = canSeeLocationArea || showExpandedAreaMap) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        DonorApproximateAreaMapPreview(
                                            areaName = donor.area.ifBlank { "Mirpur-10, Dhaka" },
                                            donorName = donor.fullName,
                                            bloodGroup = donor.bloodGroup,
                                            isBangla = strings.isBangla,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(if (showExpandedAreaMap) 210.dp else 155.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { showExpandedAreaMap = !showExpandedAreaMap }
                                        )

                                        // Directions Button to Donor's shared area
                                        OutlinedButton(
                                            onClick = {
                                                openDirectionsToDonorArea(
                                                    context = context,
                                                    areaQuery = donor.address.ifBlank { donor.area.ifBlank { "Mirpur, Dhaka" } }
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MbdCrimson),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MbdCrimson),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("donor_directions_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Directions,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${strings.openDirectionsBtn} (${donor.area.substringBefore(",")})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }

                                // Privacy Notice — never expose precise live GPS location without explicit permission
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MbdSoftPinkAlert.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = MbdCrimson,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (strings.isBangla) {
                                                "গোপনীয়তা সুরক্ষা: রক্তদাতার সুস্পষ্ট অনুমতি ছাড়া কখনোই সুনির্দিষ্ট লাইভ জিপিএস লোকেশন প্রকাশ করা হয় না। এখানে শুধুমাত্র অনুমোদিত এলাকা দেখানো হচ্ছে।"
                                            } else {
                                                "Privacy Protected: Precise live GPS coordinates are never shown without explicit permission. Only the donor's shared area is displayed."
                                            },
                                            fontSize = 11.sp,
                                            color = MbdTextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Report Inaccurate Profile / Abuse Action
                        TextButton(
                            onClick = { showReportDonorDialog = true },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .testTag("donor_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MbdTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.reportDonorTitle,
                                color = MbdTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (showRecordDonationDialog && isSelf) {
                        RecordBloodDonationDialog(
                            strings = strings,
                            defaultBloodGroup = donor.bloodGroup,
                            defaultLocation = donor.area,
                            isAdminUser = isAdminUser,
                            eligibilityRules = eligibilityRules,
                            existingRecords = (donorDonationsState as? UiState.Success)?.data.orEmpty(),
                            isSaving = isSaving,
                            onDismiss = { showRecordDonationDialog = false },
                            onConfirmRecord = { date, time, type, customTypeName, procConfirm, interval, ruleVer, hosp, loc, rec, bg, notes, verStatus ->
                                onRecordBloodDonation(
                                    date,
                                    time,
                                    type,
                                    customTypeName,
                                    procConfirm,
                                    interval,
                                    ruleVer,
                                    hosp,
                                    loc,
                                    rec,
                                    bg,
                                    notes,
                                    verStatus
                                )
                                showRecordDonationDialog = false
                            }
                        )
                    }

                    if (showAdminRulesDialog && isAdminUser) {
                        AdminEligibilityRulesDialog(
                            strings = strings,
                            currentRules = eligibilityRules,
                            onDismiss = { showAdminRulesDialog = false },
                            onSaveRuleAndRecalculate = { updatedRule ->
                                onSaveEligibilityRule(updatedRule)
                                showAdminRulesDialog = false
                            }
                        )
                    }

                    if (showAdminCorrectDatesDialog && (isAdminUser || isSelf)) {
                        AdminCorrectDonorDatesDialog(
                            strings = strings,
                            donorName = donor.fullName,
                            initialLastDonationDate = donor.lastDonationDate,
                            initialNextEligibleDate = donor.nextEligibleDate,
                            initialIntervalDays = donor.eligibilityIntervalDays,
                            initialTotalDonations = donor.totalDonations,
                            onDismiss = { showAdminCorrectDatesDialog = false },
                            onConfirmCorrection = { lastD, nextD, intervalD, totalD ->
                                onAdminCorrectDonorDates(donor, lastD, nextD, intervalD, totalD)
                                showAdminCorrectDatesDialog = false
                            }
                        )
                    }

                    if (showReportDonorDialog) {
                        val donorReportReasons = if (strings.isBangla) {
                            listOf(
                                "ভুল রক্তের গ্রুপ বা মিথ্যা তথ্য",
                                "ফোন নম্বর ভুল, বন্ধ বা ভুয়া",
                                "রক্তদানের জন্য অনৈতিক অর্থ দাবি",
                                "রক্তদাতা রক্তদানে অস্বীকৃতি জানিয়েছেন",
                                "অন্যান্য অভিযোগ"
                            )
                        } else {
                            listOf(
                                "Inaccurate blood group or false details",
                                "Inactive, wrong, or fake phone number",
                                "Demanded unethical payment for donation",
                                "Refused to donate blood despite active status",
                                "Other complaint"
                            )
                        }

                        AlertDialog(
                            onDismissRequest = { showReportDonorDialog = false },
                            title = {
                                Text(
                                    text = strings.reportDonorTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MbdCrimson
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (strings.isBangla) "রিপোর্টের সুনির্দিষ্ট কারণ নির্বাচন করুন:" else "Select specific reason for report:",
                                        fontSize = 13.sp,
                                        color = MbdTextSecondary
                                    )
                                    donorReportReasons.forEach { reasonOpt ->
                                        val isChosen = reportDonorReasonText == reasonOpt
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isChosen) MbdCrimsonLight else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, if (isChosen) MbdCrimson else MbdBorderGray),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { reportDonorReasonText = reasonOpt }
                                        ) {
                                            Text(
                                                text = reasonOpt,
                                                fontSize = 12.sp,
                                                color = if (isChosen) MbdCrimson else MbdTextPrimary,
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = reportDonorReasonText,
                                        onValueChange = { reportDonorReasonText = it },
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
                                        if (reportDonorReasonText.isNotBlank()) {
                                            onReportDonor(donor.id.ifBlank { donor.userId }, reportDonorReasonText)
                                            showReportDonorDialog = false
                                        }
                                    },
                                    enabled = reportDonorReasonText.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson)
                                ) {
                                    Text(strings.reportSubmitBtn)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showReportDonorDialog = false }) {
                                    Text(if (strings.isBangla) "বাতিল" else "Cancel", color = MbdTextSecondary)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileQuickStat(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = valueColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MbdTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeaderRow(
    icon: ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MbdCrimsonLight,
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MbdCrimson,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MbdTextPrimary
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MbdTextPrimary,
    isValueBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MbdTextSecondary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MbdTextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                color = valueColor,
                fontWeight = if (isValueBold) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DonorApproximateAreaMapPreview(
    areaName: String,
    donorName: String,
    bloodGroup: String,
    isBangla: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MbdBorderGray),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Soft map background
                drawRect(color = Color(0xFFF3F6F9))

                // Park & Green zone accents
                drawCircle(
                    color = Color(0xFFDFF0E2),
                    radius = w * 0.22f,
                    center = Offset(w * 0.2f, h * 0.25f)
                )
                drawCircle(
                    color = Color(0xFFDFF0E2),
                    radius = w * 0.18f,
                    center = Offset(w * 0.82f, h * 0.78f)
                )

                // Grid streets
                val gridStep = 36.dp.toPx()
                var x = gridStep
                while (x < w) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    x += gridStep
                }
                var y = gridStep
                while (y < h) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    y += gridStep
                }

                // Major Mirpur arterial roads (Rokeya Sarani & Mirpur Road)
                val roadPath = Path().apply {
                    moveTo(0f, h * 0.65f)
                    cubicTo(w * 0.35f, h * 0.58f, w * 0.65f, h * 0.45f, w, h * 0.32f)
                }
                drawPath(
                    path = roadPath,
                    color = Color.White,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = roadPath,
                    color = Color(0xFFCBD5E1),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )

                val center = Offset(w * 0.5f, h * 0.5f)
                // Approximate Sector Highlight Circle (Privacy-Safe Area Zone, not exact house pin)
                drawCircle(
                    color = MbdCrimson.copy(alpha = 0.14f),
                    radius = 38.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = MbdCrimson.copy(alpha = 0.45f),
                    radius = 38.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // Center Area Marker
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = MbdCrimson,
                    radius = 8.5.dp.toPx(),
                    center = center
                )

                // Label
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#1E293B")
                    textSize = 11.dp.toPx()
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                    )
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "$areaName ($bloodGroup)",
                    14.dp.toPx(),
                    h - 12.dp.toPx(),
                    paint
                )
            }

            // Top-Left Badge
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MbdCrimson,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBangla) "অনুমোদিত এলাকার প্রিভিউ: $areaName" else "Shared Area: $areaName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MbdTextPrimary
                    )
                }
            }
        }
    }
}

private data class EligibilityCalculationResult(
    val formattedDate: String,
    val isEligibleNow: Boolean,
    val statusNote: String
)

/**
 * Calculates the next eligible donation date based on the donor's recorded donationType and eligibilityIntervalDays.
 * Returns null if lastDonationDate is empty, unconfigured ("other" with 0 interval), or not in a reliable YYYY-MM-DD format.
 */
private fun calculateNextEligibleDate(
    lastDonationDate: String,
    eligibilityIntervalDays: Long,
    donationType: String,
    isBangla: Boolean
): EligibilityCalculationResult? {
    val trimmed = lastDonationDate.trim()
    if (!trimmed.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) return null
    if (donationType == "other" && eligibilityIntervalDays <= 0L) return null
    val interval = if (eligibilityIntervalDays > 0L) eligibilityIntervalDays.toInt() else 90
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        val parsed = parser.parse(trimmed) ?: return null
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, interval)
        }
        val nextDate = cal.time
        val nextFormatted = parser.format(nextDate)
        val now = System.currentTimeMillis()
        val isEligible = nextDate.time <= now
        val note = if (isEligible) {
            if (isBangla) {
                "সম্ভাব্য পরবর্তী রক্তদানের সময় হয়েছে ($interval দিনের নিয়ম)"
            } else {
                "$interval-day interval completed • Estimated time reached"
            }
        } else {
            val daysLeft = ((nextDate.time - now) / (24L * 3600_000L)).coerceAtLeast(1L)
            if (isBangla) {
                "প্রযোজ্য নিয়ম অনুযায়ী নির্ধারিত ($interval দিনের বিরতি • আরও প্রায় $daysLeft দিন বাকি)"
            } else {
                "Approx. $daysLeft day(s) remaining ($interval-day rule)"
            }
        }
        EligibilityCalculationResult(
            formattedDate = nextFormatted,
            isEligibleNow = isEligible,
            statusNote = note
        )
    } catch (e: Exception) {
        null
    }
}

private fun openSmsToDonor(
    context: Context,
    donor: DonorProfile,
    isBangla: Boolean
) {
    val sanitized = donor.phoneNumber.filter { it.isDigit() || it == '+' }
    if (sanitized.isBlank()) return
    val body = if (isBangla) {
        "আসসালামু আলাইকুম ${donor.fullName}, Mirpur Blood Donation (MBD 🩸) অ্যাপ থেকে জরুরি ${donor.bloodGroup} রক্তের প্রয়োজনে যোগাযোগ করছি।"
    } else {
        "Hello ${donor.fullName}, contacting you via Mirpur Blood Donation (MBD 🩸) regarding an urgent ${donor.bloodGroup} blood request."
    }
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$sanitized")).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        dialPhoneNumber(context, sanitized)
    }
}

private fun openDirectionsToDonorArea(
    context: Context,
    areaQuery: String
) {
    val encoded = Uri.encode(areaQuery)
    try {
        val gmmUri = Uri.parse("geo:0,0?q=$encoded")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
            return
        }
    } catch (ignored: Exception) {
    }
    try {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
        val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    } catch (ignored: Exception) {
    }
}

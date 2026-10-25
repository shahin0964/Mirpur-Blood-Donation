package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BLOOD_GROUPS
import com.example.data.model.BloodRequest
import com.example.data.model.DEFAULT_ELIGIBILITY_RULES
import com.example.data.model.DonationRecord
import com.example.data.model.DonorProfile
import com.example.data.model.EligibilityRule
import com.example.data.model.MIRPUR_LOCATIONS
import com.example.data.model.UserProfile
import com.example.data.model.computeDonorAchievementBadges
import com.example.ui.components.AdminCorrectDonorDatesDialog
import com.example.ui.components.AdminEligibilityRulesDialog
import com.example.ui.components.AvailabilityStatusBadge
import com.example.ui.components.DonationCountdownEngine
import com.example.ui.components.DonationHistoryTimelineSection
import com.example.ui.components.DonorAvailabilityStatusSelector
import com.example.ui.components.LiveDonationCountdownCard
import com.example.ui.components.RecordBloodDonationDialog
import com.example.ui.i18n.MbdStrings
import com.example.ui.theme.MbdAvailableGreen
import com.example.ui.theme.MbdBorderGray
import com.example.ui.theme.MbdCallGreen
import com.example.ui.theme.MbdChipUnselected
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdCrimsonLight
import com.example.ui.theme.MbdSurface
import com.example.ui.theme.MbdTextPrimary
import com.example.ui.theme.MbdTextSecondary
import com.example.ui.util.MbdNotificationHelper
import com.example.ui.util.dialPhoneNumber
import com.example.ui.viewmodel.MbdScreen
import com.example.ui.viewmodel.UiState

/**
 * Navigation Drawer Sheet matching Reference Screen 5 (Bottom-Right in reference image)
 */
@Composable
fun MbdDrawerSheetContent(
    strings: MbdStrings,
    userName: String,
    userEmail: String,
    userBloodGroup: String?,
    openRequestsCount: Int,
    currentScreen: MbdScreen,
    isAdmin: Boolean = false,
    onSelectScreen: (MbdScreen) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MbdSurface,
        drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
        modifier = Modifier
            .width(305.dp)
            .fillMaxHeight()
    ) {
        // Red Header matching Reference Screen 5
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MbdCrimson)
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.8f)),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val initials = userName
                            .trim()
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercase() }
                            .ifEmpty { "M" }
                        Text(
                            text = initials,
                            color = MbdCrimson,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName.ifBlank { "MBD Member" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (userEmail.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail,
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!userBloodGroup.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "🩸 $userBloodGroup",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Drawer Menu Items matching Reference Screen 5
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp)
        ) {
            DrawerMenuItem(
                icon = Icons.Default.Home,
                label = strings.navHome,
                selected = currentScreen == MbdScreen.HOME,
                iconTint = MbdCrimson,
                onClick = { onSelectScreen(MbdScreen.HOME) },
                testTag = "drawer_item_home"
            )
            DrawerMenuItem(
                icon = Icons.Default.WaterDrop,
                label = strings.myDonations,
                selected = currentScreen == MbdScreen.MY_DONATIONS,
                onClick = { onSelectScreen(MbdScreen.MY_DONATIONS) },
                testTag = "drawer_item_my_donations"
            )
            DrawerMenuItem(
                icon = Icons.Default.CalendarToday,
                label = strings.requestBlood,
                selected = currentScreen == MbdScreen.REQUEST_BLOOD,
                onClick = { onSelectScreen(MbdScreen.REQUEST_BLOOD) },
                testTag = "drawer_item_request_blood"
            )
            DrawerMenuItem(
                icon = Icons.Default.Groups,
                label = strings.findDonors,
                selected = currentScreen == MbdScreen.FIND_DONORS,
                onClick = { onSelectScreen(MbdScreen.FIND_DONORS) },
                testTag = "drawer_item_find_donors"
            )
            DrawerMenuItem(
                icon = Icons.Default.Notifications,
                label = strings.notifications,
                selected = currentScreen == MbdScreen.NOTIFICATIONS,
                badgeCount = openRequestsCount,
                onClick = { onSelectScreen(MbdScreen.NOTIFICATIONS) },
                testTag = "drawer_item_notifications"
            )
            DrawerMenuItem(
                icon = Icons.Default.HealthAndSafety,
                label = strings.awarenessEligibility,
                selected = currentScreen == MbdScreen.AWARENESS,
                onClick = { onSelectScreen(MbdScreen.AWARENESS) },
                testTag = "drawer_item_awareness"
            )
            DrawerMenuItem(
                icon = Icons.Default.Settings,
                label = strings.settings,
                selected = currentScreen == MbdScreen.SETTINGS,
                onClick = { onSelectScreen(MbdScreen.SETTINGS) },
                testTag = "drawer_item_settings"
            )
            DrawerMenuItem(
                icon = Icons.Default.Info,
                label = strings.aboutUs,
                selected = currentScreen == MbdScreen.ABOUT_CONTACT_PRIVACY,
                onClick = { onSelectScreen(MbdScreen.ABOUT_CONTACT_PRIVACY) },
                testTag = "drawer_item_about_us"
            )

            if (isAdmin) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                    color = MbdCrimson.copy(alpha = 0.35f)
                )
                DrawerMenuItem(
                    icon = Icons.Default.Security,
                    label = strings.adminPanelTitle,
                    selected = currentScreen == MbdScreen.ADMIN_PANEL,
                    iconTint = MbdCrimson,
                    onClick = { onSelectScreen(MbdScreen.ADMIN_PANEL) },
                    testTag = "drawer_item_admin_panel"
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                color = MbdBorderGray
            )

            DrawerMenuItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = strings.logout,
                selected = false,
                onClick = onLogout,
                testTag = "drawer_item_logout"
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    iconTint: Color = if (selected) MbdCrimson else MbdTextSecondary,
    badgeCount: Int = 0
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MbdCrimsonLight.copy(alpha = 0.65f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = label,
                color = if (selected) MbdCrimson else MbdTextPrimary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (badgeCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = MbdCrimson,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badgeCount.coerceAtMost(99).toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Profile & Donor Registration Screen (Features #2, #7, #8 + Live Countdown & Donation History)
 */
@Composable
fun ProfileAndDonorRegistrationScreen(
    strings: MbdStrings,
    defaultName: String,
    defaultEmail: String,
    userProfileState: UiState<UserProfile?>,
    myDonorState: UiState<DonorProfile?>,
    myDonationsState: UiState<List<DonationRecord>> = UiState.Success(emptyList()),
    eligibilityRules: Map<String, EligibilityRule> = DEFAULT_ELIGIBILITY_RULES,
    isAdminUser: Boolean = false,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSaveProfile: (
        fullName: String,
        phoneNumber: String,
        bloodGroup: String,
        area: String,
        address: String,
        lastDonationDate: String,
        availabilityStatus: String,
        isPublicDonor: Boolean,
        notificationsEnabled: Boolean,
        totalDonations: Long,
        donationType: String,
        eligibilityIntervalDays: Long,
        showPhonePublicly: Boolean,
        shareLocationArea: Boolean,
        showDonationHistoryPublicly: Boolean,
        age: String,
        gender: String
    ) -> Unit,
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
    onDeleteDonationRecord: (String) -> Unit = {},
    onLogout: () -> Unit
) {
    BackHandler { onBack() }

    val existingUser = (userProfileState as? UiState.Success)?.data
    val existingDonor = (myDonorState as? UiState.Success)?.data
    val myRecords = (myDonationsState as? UiState.Success)?.data.orEmpty()
    val pendingRecordsCount = myRecords.count { it.verificationStatus == "pending" }
    val multiEval = remember(myRecords, eligibilityRules) {
        DonationCountdownEngine.evaluateMultiRecordEligibility(myRecords, eligibilityRules)
    }
    val countsByType = remember(myRecords) {
        DonationCountdownEngine.computeCountsByType(myRecords)
    }

    var fullName by remember { mutableStateOf(existingUser?.fullName ?: defaultName) }
    var phoneNumber by remember { mutableStateOf(existingUser?.phoneNumber ?: existingDonor?.phoneNumber ?: "") }
    var bloodGroup by remember { mutableStateOf(existingUser?.bloodGroup ?: existingDonor?.bloodGroup ?: "A+") }
    var area by remember { mutableStateOf(existingUser?.area ?: existingDonor?.area ?: "Mirpur-10, Dhaka") }
    var address by remember { mutableStateOf(existingUser?.address ?: existingDonor?.address ?: "Mirpur-10, Dhaka") }
    var lastDonationDate by remember { mutableStateOf(existingDonor?.lastDonationDate ?: existingUser?.lastDonationDate ?: "") }
    var nextEligibleDate by remember { mutableStateOf(existingDonor?.nextEligibleDate ?: existingUser?.nextEligibleDate ?: "") }
    var donationType by remember { mutableStateOf(existingDonor?.donationType ?: existingUser?.donationType ?: "whole_blood") }
    var eligibilityIntervalDays by remember { mutableLongStateOf(existingDonor?.eligibilityIntervalDays ?: existingUser?.eligibilityIntervalDays ?: 90L) }
    var availabilityStatus by remember {
        mutableStateOf(
            existingDonor?.resolvedAvailabilityStatus()
                ?: existingUser?.availabilityStatus?.takeIf { it.isNotBlank() }
                ?: "available"
        )
    }
    var isPublicDonor by remember { mutableStateOf((existingDonor?.visibility ?: "public") == "public") }
    var showPhonePublicly by remember { mutableStateOf(existingDonor?.showPhonePublicly ?: true) }
    var shareLocationArea by remember { mutableStateOf(existingDonor?.shareLocationArea ?: true) }
    var showDonationHistoryPublicly by remember { mutableStateOf(existingDonor?.showDonationHistoryPublicly ?: true) }
    var notificationsEnabled by remember { mutableStateOf(existingUser?.notificationsEnabled ?: true) }
    var age by remember { mutableStateOf(existingDonor?.age ?: "") }
    var gender by remember { mutableStateOf(existingDonor?.gender ?: "") }
    var totalDonations by remember { mutableLongStateOf(existingDonor?.totalDonations ?: 0L) }

    var showRecordDonationDialog by remember { mutableStateOf(false) }
    var showAdminCorrectDatesDialog by remember { mutableStateOf(false) }
    var showAdminRulesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(existingUser, existingDonor) {
        if (existingUser != null) {
            fullName = existingUser.fullName.ifBlank { defaultName }
            phoneNumber = existingUser.phoneNumber
            bloodGroup = existingUser.bloodGroup
            area = existingUser.area
            address = existingUser.address
            lastDonationDate = existingUser.lastDonationDate
            nextEligibleDate = existingUser.nextEligibleDate
            donationType = existingUser.donationType.ifBlank { "whole_blood" }
            eligibilityIntervalDays = existingUser.eligibilityIntervalDays.coerceIn(14L, 365L)
            availabilityStatus = existingUser.availabilityStatus.ifBlank {
                if (existingUser.isAvailable) "available" else "temporarily_unavailable"
            }
            notificationsEnabled = existingUser.notificationsEnabled
        }
        if (existingDonor != null) {
            isPublicDonor = existingDonor.visibility == "public"
            showPhonePublicly = existingDonor.showPhonePublicly
            shareLocationArea = existingDonor.shareLocationArea
            showDonationHistoryPublicly = existingDonor.showDonationHistoryPublicly
            totalDonations = existingDonor.totalDonations
            availabilityStatus = existingDonor.resolvedAvailabilityStatus()
            age = existingDonor.age
            gender = existingDonor.gender
            if (existingDonor.lastDonationDate.isNotBlank()) {
                lastDonationDate = existingDonor.lastDonationDate
                nextEligibleDate = existingDonor.nextEligibleDate
                donationType = existingDonor.donationType.ifBlank { "whole_blood" }
                eligibilityIntervalDays = existingDonor.eligibilityIntervalDays.coerceIn(14L, 365L)
            }
            if (phoneNumber.isBlank()) phoneNumber = existingDonor.phoneNumber
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
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = strings.donorRegistrationTitle,
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
            // Top Profile Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdBorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MbdCrimsonLight,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = bloodGroup,
                                color = MbdCrimson,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fullName.ifBlank { defaultName.ifBlank { "MBD Donor" } },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MbdTextPrimary
                        )
                        if (defaultEmail.isNotBlank()) {
                            Text(
                                text = defaultEmail,
                                fontSize = 13.sp,
                                color = MbdTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        AvailabilityStatusBadge(
                            availabilityStatus = availabilityStatus,
                            isAvailableFallback = availabilityStatus == "available",
                            strings = strings,
                            compact = false,
                            forceTemporarilyUnavailable = multiEval.nextEligibleDate.isNotBlank() && multiEval.nextEligibleDate != "UNDECIDED" && !DonationCountdownEngine.computeCountdown(multiEval.lastDonationDate, multiEval.nextEligibleDate, multiEval.governingIntervalDays, multiEval.lastDonationType, multiEval.lastDonationTime).isZeroReached
                        )
                    }
                }
            }

            // =========================================================================
            // LIVE DONATION COUNTDOWN TIMER ON OWN PROFILE (1-Second Real-Time Ticker)
            // =========================================================================
            LiveDonationCountdownCard(
                strings = strings,
                lastDonationDate = lastDonationDate,
                lastDonationTime = existingDonor?.lastDonationTime?.ifBlank { existingUser?.lastDonationTime.orEmpty() }
                    ?: multiEval.lastDonationTime,
                nextEligibleDate = nextEligibleDate,
                eligibilityIntervalDays = eligibilityIntervalDays,
                donationType = donationType,
                customDonationTypeName = existingDonor?.customDonationTypeName?.ifBlank { existingUser?.customDonationTypeName.orEmpty() }
                    ?: multiEval.customDonationTypeName,
                ruleVersion = existingDonor?.ruleVersion?.ifBlank { multiEval.governingRuleVersion }
                    ?: multiEval.governingRuleVersion,
                totalConfirmedDonations = totalDonations,
                pendingDonationsCount = pendingRecordsCount,
                countsByType = countsByType,
                governingRuleExplanation = if (strings.isBangla) {
                    multiEval.governingExplanationBn
                } else {
                    multiEval.governingExplanationEn
                },
                isPublicProfileHiddenByPrivacy = false,
                onRecordDonationClick = { showRecordDonationDialog = true },
                onAdminCorrectDatesClick = { showAdminCorrectDatesDialog = true },
                onAdminConfigureRulesClick = if (isAdminUser) {
                    { showAdminRulesDialog = true }
                } else {
                    null
                }
            )

            // =========================================================================
            // DONOR ACHIEVEMENT BADGES (Recognized Milestones)
            // =========================================================================
            val selfDonor = existingDonor ?: DonorProfile(
                fullName = defaultName,
                phoneNumber = existingUser?.phoneNumber.orEmpty(),
                bloodGroup = bloodGroup,
                area = area,
                totalDonations = totalDonations
            )
            val selfBadges = remember(selfDonor, totalDonations) {
                computeDonorAchievementBadges(selfDonor, totalDonations)
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdBorderGray),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("self_achievements_card")
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
                            modifier = Modifier.size(36.dp)
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
                                text = if (strings.isBangla) "যাচাইকৃত রক্তদান: $totalDonations বার" else "Verified Donations: $totalDonations",
                                fontSize = 12.sp,
                                color = MbdAvailableGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = strings.achievementsNotice,
                        fontSize = 11.sp,
                        color = MbdTextSecondary,
                        lineHeight = 15.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        selfBadges.forEach { badge ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (badge.isUnlocked) MbdAvailableGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (badge.isUnlocked) MbdAvailableGreen.copy(alpha = 0.35f) else MbdBorderGray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = badge.emoji,
                                        fontSize = 22.sp,
                                        modifier = Modifier.padding(end = 10.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (strings.isBangla) badge.titleBn else badge.titleEn,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (badge.isUnlocked) MbdTextPrimary else MbdTextSecondary
                                        )
                                        Text(
                                            text = if (strings.isBangla) badge.descBn else badge.descEn,
                                            fontSize = 10.sp,
                                            color = MbdTextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
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
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3-STATE DONOR AVAILABILITY STATUS & PRIVACY CONTROLS (Section 5 of Prompt)
            // =========================================================================
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
                        text = strings.availabilityToggleLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MbdTextPrimary
                    )
                    Text(
                        text = if (strings.isBangla) {
                            "কাউন্টডাউন শেষ হলেও আপনার প্রাপ্যতা স্বয়ংক্রিয়ভাবে পরিবর্তন হবে না—আপনি নিজে আপনার বর্তমান অবস্থা নির্বাচন করুন:"
                        } else {
                            "Your availability is never changed automatically by the countdown—choose your current contact preference:"
                        },
                        fontSize = 12.sp,
                        color = MbdTextSecondary
                    )

                    DonorAvailabilityStatusSelector(
                        selectedStatus = availabilityStatus,
                        onSelectStatus = { availabilityStatus = it },
                        strings = strings
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MbdBorderGray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.publicDirectoryToggleLabel,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MbdTextPrimary
                            )
                            Text(
                                text = if (strings.isBangla) {
                                    "মিরপুর ব্লাড ডোনেশন ডিরেক্টরিতে যুক্ত থাকুন"
                                } else {
                                    "List my donor card in Mirpur Blood Donation search"
                                },
                                fontSize = 11.sp,
                                color = MbdTextSecondary
                            )
                        }
                        Switch(
                            checked = isPublicDonor,
                            onCheckedChange = { isPublicDonor = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MbdCrimson
                            ),
                            modifier = Modifier.testTag("public_directory_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isBangla) "পাবলিকভাবে ফোন নম্বর দেখান" else "Show Phone Number Publicly",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MbdTextPrimary
                            )
                            Text(
                                text = if (strings.isBangla) "রক্তপ্রার্থীদের সরাসরি ফোন করার অনুমতি দিন" else "Allow blood requesters to see & call your number",
                                fontSize = 11.sp,
                                color = MbdTextSecondary
                            )
                        }
                        Switch(
                            checked = showPhonePublicly,
                            onCheckedChange = { showPhonePublicly = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MbdCrimson
                            ),
                            modifier = Modifier.testTag("show_phone_publicly_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.showDonationHistoryPubliclyLabel,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MbdTextPrimary
                            )
                            Text(
                                text = if (strings.isBangla) {
                                    "অন্য ব্যবহারকারীরা আপনার প্রোফাইলে কাউন্টডাউন দেখতে পারবেন"
                                } else {
                                    "Allow other MBD users to see your countdown & history"
                                },
                                fontSize = 11.sp,
                                color = MbdTextSecondary
                            )
                        }
                        Switch(
                            checked = showDonationHistoryPublicly,
                            onCheckedChange = { showDonationHistoryPublicly = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MbdAvailableGreen
                            ),
                            modifier = Modifier.testTag("show_history_publicly_switch")
                        )
                    }
                }
            }

            // Personal & Donor Information Form Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdBorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.bloodGroupLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BLOOD_GROUPS) { bg ->
                            val selected = bloodGroup == bg
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) MbdCrimson else MbdChipUnselected,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { bloodGroup = bg }
                                    .testTag("profile_blood_group_$bg")
                            ) {
                                Text(
                                    text = bg,
                                    color = if (selected) Color.White else MbdTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(strings.fullNameLabel) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text(strings.contactNumberLabel) },
                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_phone_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text(if (strings.isBangla) "বয়স" else "Age") },
                            leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_age_input")
                        )

                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = if (strings.isBangla) "লিঙ্গ" else "Gender",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MbdTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val genderOptions = listOf(
                                    "male" to if (strings.isBangla) "পুরুষ" else "Male",
                                    "female" to if (strings.isBangla) "নারী" else "Female"
                                )
                                genderOptions.forEach { (gCode, gLabel) ->
                                    val isSel = gender.equals(gCode, ignoreCase = true) || gender == gLabel
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isSel) MbdCrimson else MbdChipUnselected,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { gender = gCode }
                                            .testTag("profile_gender_$gCode")
                                    ) {
                                        Text(
                                            text = gLabel,
                                            color = if (isSel) Color.White else MbdTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = strings.areaLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MIRPUR_LOCATIONS) { loc ->
                            val selected = area == loc
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) MbdCrimson else MbdChipUnselected,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        area = loc
                                        if (address.isBlank() || address in MIRPUR_LOCATIONS) {
                                            address = loc
                                        }
                                    }
                            ) {
                                Text(
                                    text = loc.substringBefore(","),
                                    color = if (selected) Color.White else MbdTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(strings.addressLabel) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_address_input")
                    )

                    OutlinedTextField(
                        value = lastDonationDate,
                        onValueChange = {
                            lastDonationDate = it
                            val autoNext = DonationCountdownEngine.computeNextEligibleDateString(it, eligibilityIntervalDays)
                            if (autoNext.isNotBlank()) {
                                nextEligibleDate = autoNext
                            }
                        },
                        label = { Text(strings.lastDonationDateLabel) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        placeholder = { Text("2026-06-15") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_last_donation_input")
                    )

                    Button(
                        onClick = {
                            onSaveProfile(
                                fullName,
                                phoneNumber,
                                bloodGroup,
                                area,
                                address,
                                lastDonationDate,
                                availabilityStatus,
                                isPublicDonor,
                                notificationsEnabled,
                                totalDonations,
                                donationType,
                                eligibilityIntervalDays,
                                showPhonePublicly,
                                shareLocationArea,
                                showDonationHistoryPublicly,
                                age,
                                gender
                            )
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = strings.saveProfileBtn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // DONATION HISTORY TIMELINE ON OWN PROFILE (Section 6 of Prompt)
            // =========================================================================
            DonationHistoryTimelineSection(
                records = myRecords,
                strings = strings,
                canEditOrDeleteOwn = true,
                canAdminVerifyOrCorrect = isAdminUser,
                onRecordDonationClick = { showRecordDonationDialog = true },
                onApproveRecord = { rec -> onApproveOrUpdateDonationRecord(rec, "verified") },
                onMarkPendingRecord = { rec -> onApproveOrUpdateDonationRecord(rec, "pending") },
                onDeleteRecord = onDeleteDonationRecord
            )
        }
    }

    if (showRecordDonationDialog) {
        RecordBloodDonationDialog(
            strings = strings,
            defaultBloodGroup = bloodGroup,
            defaultLocation = area,
            isAdminUser = isAdminUser,
            eligibilityRules = eligibilityRules,
            existingRecords = myRecords,
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

    if (showAdminCorrectDatesDialog) {
        val targetDonor = existingDonor ?: DonorProfile(
            fullName = fullName.ifBlank { defaultName },
            bloodGroup = bloodGroup,
            area = area,
            address = address,
            phoneNumber = phoneNumber,
            lastDonationDate = lastDonationDate,
            nextEligibleDate = nextEligibleDate,
            eligibilityIntervalDays = eligibilityIntervalDays,
            totalDonations = totalDonations
        )
        AdminCorrectDonorDatesDialog(
            strings = strings,
            donorName = targetDonor.fullName,
            initialLastDonationDate = lastDonationDate,
            initialNextEligibleDate = nextEligibleDate,
            initialIntervalDays = eligibilityIntervalDays,
            initialTotalDonations = totalDonations,
            onDismiss = { showAdminCorrectDatesDialog = false },
            onConfirmCorrection = { lastD, nextD, intervalD, totalD ->
                lastDonationDate = lastD
                nextEligibleDate = nextD
                eligibilityIntervalDays = intervalD
                totalDonations = totalD
                onAdminCorrectDonorDates(targetDonor, lastD, nextD, intervalD, totalD)
                showAdminCorrectDatesDialog = false
            }
        )
    }
}

/**
 * My Donations History & Live Countdown Screen (Sections 2, 3, 6 of Prompt)
 */
@Composable
fun MyDonationsScreen(
    strings: MbdStrings,
    defaultBloodGroup: String,
    myDonorProfile: DonorProfile? = null,
    donationsState: UiState<List<DonationRecord>>,
    eligibilityRules: Map<String, EligibilityRule> = DEFAULT_ELIGIBILITY_RULES,
    isAdminUser: Boolean = false,
    isSaving: Boolean,
    onBack: () -> Unit,
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
    ) -> Unit,
    onApproveOrUpdateDonationRecord: (DonationRecord, String) -> Unit = { _, _ -> },
    onAdminCorrectDonorDates: (
        donor: DonorProfile,
        correctedLastDate: String,
        correctedNextDate: String,
        correctedIntervalDays: Long,
        correctedTotalDonations: Long
    ) -> Unit = { _, _, _, _, _ -> },
    onSaveEligibilityRule: (EligibilityRule) -> Unit = {},
    onDeleteDonation: (String) -> Unit
) {
    BackHandler { onBack() }

    var showRecordDialog by remember { mutableStateOf(false) }
    var showAdminCorrectDialog by remember { mutableStateOf(false) }
    var showAdminRulesDialog by remember { mutableStateOf(false) }

    val records = (donationsState as? UiState.Success)?.data.orEmpty()
    val latestVerifiedRecord = records.firstOrNull { it.verificationStatus == "verified" }
    val pendingCount = records.count { it.verificationStatus == "pending" }
    val multiEval = remember(records, eligibilityRules) {
        DonationCountdownEngine.evaluateMultiRecordEligibility(records, eligibilityRules)
    }
    val countsByType = remember(records) {
        DonationCountdownEngine.computeCountsByType(records)
    }

    val effectiveLastDate = multiEval.lastDonationDate.takeIf { it.isNotBlank() }
        ?: myDonorProfile?.lastDonationDate?.takeIf { it.isNotBlank() }
        ?: latestVerifiedRecord?.donationDate.orEmpty()
    val effectiveLastTime = multiEval.lastDonationTime.takeIf { it.isNotBlank() }
        ?: myDonorProfile?.lastDonationTime?.takeIf { it.isNotBlank() }
        ?: latestVerifiedRecord?.donationTime.orEmpty()
    val effectiveInterval = if (multiEval.hasVerifiedRecords) {
        multiEval.governingIntervalDays
    } else {
        myDonorProfile?.eligibilityIntervalDays
            ?: latestVerifiedRecord?.eligibilityIntervalDays
            ?: 90L
    }
    val effectiveNextDate = if (multiEval.hasVerifiedRecords) {
        multiEval.nextEligibleDate
    } else {
        myDonorProfile?.nextEligibleDate?.takeIf { it.isNotBlank() }
            ?: latestVerifiedRecord?.nextEligibleDate.orEmpty()
    }
    val effectiveType = multiEval.lastDonationType.takeIf { it.isNotBlank() && multiEval.hasVerifiedRecords }
        ?: myDonorProfile?.donationType?.takeIf { it.isNotBlank() }
        ?: latestVerifiedRecord?.donationType
        ?: "whole_blood"
    val effectiveCustomType = multiEval.customDonationTypeName.takeIf { it.isNotBlank() }
        ?: myDonorProfile?.customDonationTypeName?.takeIf { it.isNotBlank() }
        ?: latestVerifiedRecord?.customDonationTypeName.orEmpty()
    val effectiveRuleVersion = multiEval.governingRuleVersion.takeIf { it.isNotBlank() && multiEval.hasVerifiedRecords }
        ?: myDonorProfile?.ruleVersion?.takeIf { it.isNotBlank() }
        ?: latestVerifiedRecord?.ruleVersion
        ?: "v1.0"
    val effectiveTotalConfirmed = myDonorProfile?.totalDonations
        ?: records.count { it.verificationStatus == "verified" }.toLong()

    Scaffold(
        topBar = {
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
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = strings.myDonations,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showRecordDialog = true },
                containerColor = MbdCrimson,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_donation_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.recordNewDonationBtn)
            }
        }
    ) { innerPadding ->
        when (donationsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MbdCrimson)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = donationsState.message, color = MbdCrimson)
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        LiveDonationCountdownCard(
                            strings = strings,
                            lastDonationDate = effectiveLastDate,
                            lastDonationTime = effectiveLastTime,
                            nextEligibleDate = effectiveNextDate,
                            eligibilityIntervalDays = effectiveInterval,
                            donationType = effectiveType,
                            customDonationTypeName = effectiveCustomType,
                            ruleVersion = effectiveRuleVersion,
                            totalConfirmedDonations = effectiveTotalConfirmed,
                            pendingDonationsCount = pendingCount,
                            countsByType = countsByType,
                            governingRuleExplanation = if (strings.isBangla) {
                                multiEval.governingExplanationBn
                            } else {
                                multiEval.governingExplanationEn
                            },
                            isPublicProfileHiddenByPrivacy = false,
                            onRecordDonationClick = { showRecordDialog = true },
                            onAdminCorrectDatesClick = if (myDonorProfile != null) {
                                { showAdminCorrectDialog = true }
                            } else {
                                null
                            },
                            onAdminConfigureRulesClick = if (isAdminUser) {
                                { showAdminRulesDialog = true }
                            } else {
                                null
                            }
                        )
                    }

                    item {
                        DonationHistoryTimelineSection(
                            records = records,
                            strings = strings,
                            canEditOrDeleteOwn = true,
                            canAdminVerifyOrCorrect = isAdminUser,
                            onRecordDonationClick = { showRecordDialog = true },
                            onApproveRecord = { rec -> onApproveOrUpdateDonationRecord(rec, "verified") },
                            onMarkPendingRecord = { rec -> onApproveOrUpdateDonationRecord(rec, "pending") },
                            onDeleteRecord = onDeleteDonation
                        )
                    }
                }
            }
        }
    }

    if (showRecordDialog) {
        RecordBloodDonationDialog(
            strings = strings,
            defaultBloodGroup = defaultBloodGroup,
            defaultLocation = myDonorProfile?.area ?: "Mirpur-10, Dhaka",
            isAdminUser = isAdminUser,
            eligibilityRules = eligibilityRules,
            existingRecords = records,
            isSaving = isSaving,
            onDismiss = { showRecordDialog = false },
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
                showRecordDialog = false
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

    if (showAdminCorrectDialog && myDonorProfile != null) {
        AdminCorrectDonorDatesDialog(
            strings = strings,
            donorName = myDonorProfile.fullName,
            initialLastDonationDate = effectiveLastDate,
            initialNextEligibleDate = effectiveNextDate,
            initialIntervalDays = effectiveInterval,
            initialTotalDonations = effectiveTotalConfirmed,
            onDismiss = { showAdminCorrectDialog = false },
            onConfirmCorrection = { lastD, nextD, intervalD, totalD ->
                onAdminCorrectDonorDates(myDonorProfile, lastD, nextD, intervalD, totalD)
                showAdminCorrectDialog = false
            }
        )
    }
}

/**
 * Community Blood Requests & Notifications Screen (Features #5, #6, #10)
 */
@Composable
fun RequestsAndNotificationsScreen(
    title: String,
    strings: MbdStrings,
    currentUserId: String,
    bloodRequestsState: UiState<List<BloodRequest>>,
    onBack: () -> Unit,
    onCreateRequestClick: () -> Unit,
    onOpenRequestDetails: (BloodRequest) -> Unit,
    onOpenDonorLiveMap: (BloodRequest) -> Unit,
    onMarkRequestStatus: (String, String) -> Unit,
    onDeleteRequest: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("All") }
    var hasNotifPerm by remember {
        mutableStateOf(MbdNotificationHelper.hasNotificationPermission(context))
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
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
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCreateRequestClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.requestBlood,
                        tint = Color.White
                    )
                }
            }
        }

        // Notification Permission Banner if not granted on Android 13+
        if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MbdCrimsonLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MbdCrimson
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (strings.isBangla) {
                            "জরুরি রক্তের আবেদনের নোটিফিকেশন পেতে অনুমতি দিন"
                        } else {
                            "Enable notifications for urgent blood requests near you"
                        },
                        fontSize = 12.sp,
                        color = MbdTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(if (strings.isBangla) "অনুমতি দিন" else "Allow", fontSize = 12.sp)
                    }
                }
            }
        }

        // Blood Group Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("All") + BLOOD_GROUPS) { bg ->
                val isSelected = selectedFilter == bg
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) MbdCrimson else MbdChipUnselected,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { selectedFilter = bg }
                ) {
                    Text(
                        text = if (bg == "All") strings.allFilter else bg,
                        color = if (isSelected) Color.White else MbdTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }
            }
        }

        when (bloodRequestsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MbdCrimson)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = bloodRequestsState.message, color = MbdCrimson)
                }
            }
            is UiState.Success -> {
                val filtered = bloodRequestsState.data.filter {
                    selectedFilter == "All" || it.bloodGroup == selectedFilter
                }
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BloodDropBadgeIcon(modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = strings.noRequestsYet, color = MbdTextSecondary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onCreateRequestClick,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson)
                            ) {
                                Text(strings.createFirstRequest)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { req ->
                            RecentRequestCard(
                                request = req,
                                strings = strings,
                                currentUserId = currentUserId,
                                onViewClick = { onOpenRequestDetails(req) },
                                onViewLiveLocationClick = { onOpenDonorLiveMap(req) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Blood Donation Awareness & Eligibility Screen (Feature #9)
 */
@Composable
fun AwarenessAndEligibilityScreen(
    strings: MbdStrings,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

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
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = strings.awarenessEligibility,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
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
            AwarenessSectionCard(
                title = if (strings.isBangla) "রক্তদানের যোগ্যতা (Eligibility Criteria)" else "Donor Eligibility Criteria",
                items = if (strings.isBangla) {
                    listOf(
                        "বয়স: ১৮ থেকে ৬০ বছরের মধ্যে হতে হবে।",
                        "ওজন: ন্যূনতম ৫০ কেজি (বা ৪৮ কেজি+) হতে হবে।",
                        "হিমোগ্লোবিন: পুরুষদের ক্ষেত্রে কমপক্ষে ১৩.০ g/dL এবং নারীদের ক্ষেত্রে ১২.৫ g/dL।",
                        "রক্তদানের বিরতি: সর্বশেষ রক্তদানের পর অন্তত ৯০ থেকে ১২০ দিন (৩-৪ মাস) পূর্ণ হতে হবে।",
                        "রক্তচাপ ও নাড়ির গতি স্বাভাবিক থাকতে হবে এবং কোনো সংক্রামক রোগ থাকা যাবে না।"
                    )
                } else {
                    listOf(
                        "Age: Between 18 and 60 years old.",
                        "Weight: Minimum 50 kg (110 lbs).",
                        "Hemoglobin: At least 12.5 g/dL for females and 13.0 g/dL for males.",
                        "Donation Interval: At least 90 to 120 days (3–4 months) since last whole blood donation.",
                        "Normal blood pressure, pulse, and free from active infectious illnesses."
                    )
                }
            )

            AwarenessSectionCard(
                title = if (strings.isBangla) "রক্তদানের উপকারিতা" else "Benefits of Donating Blood",
                items = if (strings.isBangla) {
                    listOf(
                        "এক ব্যাগ রক্ত থেকে লোহিত কণিকা, প্লাজমা ও প্লাটিলেট আলাদা করে ৩ জন মুমূর্ষু রোগীর জীবন বাঁচানো সম্ভব।",
                        "শরীরে নতুন রক্তকণিকা তৈরির প্রক্রিয়া সচল ও গতিশীল হয়।",
                        "হৃদরোগ ও অতিরিক্ত আয়রনজনিত ঝুঁকি কমে এবং মানসিক প্রশান্তি পাওয়া যায়।"
                    )
                } else {
                    listOf(
                        "One unit of whole blood can save up to 3 lives (Red cells, Plasma, Platelets).",
                        "Stimulates production of fresh new blood cells in your body.",
                        "Includes a free mini health check-up (hemoglobin, blood pressure, screening)."
                    )
                }
            )

            AwarenessSectionCard(
                title = if (strings.isBangla) "রক্তের গ্রুপ সামঞ্জস্যতা (Compatibility)" else "Blood Group Compatibility Chart",
                items = listOf(
                    "O- : Universal Red Cell Donor (সকল গ্রুপকে দিতে পারে, শুধু O- নেয়)",
                    "O+ : Gives to O+, A+, B+, AB+ | Receives from O+, O-",
                    "A+ : Gives to A+, AB+ | Receives from A+, A-, O+, O-",
                    "A- : Gives to A+, A-, AB+, AB- | Receives from A-, O-",
                    "B+ : Gives to B+, AB+ | Receives from B+, B-, O+, O-",
                    "B- : Gives to B+, B-, AB+, AB- | Receives from B-, O-",
                    "AB+ : Universal Recipient (সকল গ্রুপ থেকে নিতে পারে, AB+ কে দেয়)",
                    "AB- : Gives to AB+, AB- | Receives from AB-, A-, B-, O-"
                )
            )
        }
    }
}

@Composable
private fun AwarenessSectionCard(title: String, items: List<String>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MbdSurface),
        border = BorderStroke(1.dp, MbdBorderGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = MbdCrimson,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            items.forEach { line ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MbdAvailableGreen,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = line,
                        fontSize = 13.sp,
                        color = MbdTextPrimary,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}


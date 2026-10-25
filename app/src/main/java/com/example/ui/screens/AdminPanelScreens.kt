package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminAnnouncement
import com.example.data.model.AdminLog
import com.example.data.model.AdminReport
import com.example.data.model.AdminUserBan
import com.example.data.model.AppConfig
import com.example.data.model.BloodRequest
import com.example.data.model.DonationRecord
import com.example.data.model.DonorProfile
import com.example.data.model.EligibilityRule
import com.example.data.model.UserProfile
import com.example.ui.components.AdminCorrectDonorDatesDialog
import com.example.ui.components.AdminEligibilityRulesDialog
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
import com.example.ui.viewmodel.AdminSubSection
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminPanelScreen(
    strings: MbdStrings,
    currentUserId: String,
    adminEmail: String,
    activeSubSection: AdminSubSection,
    onSelectSubSection: (AdminSubSection) -> Unit,
    onNavigateBack: () -> Unit,
    // Data States
    usersState: UiState<List<UserProfile>>,
    bansState: UiState<List<AdminUserBan>>,
    reportsState: UiState<List<AdminReport>>,
    requestsState: UiState<List<BloodRequest>>,
    donorsState: UiState<List<DonorProfile>>,
    donationHistoryState: UiState<List<DonationRecord>>,
    logsState: UiState<List<AdminLog>>,
    announcementsState: UiState<List<AdminAnnouncement>>,
    appConfig: AppConfig,
    eligibilityRules: Map<String, EligibilityRule>,
    // Actions
    onBanUser: (userId: String, reason: String) -> Unit,
    onUnbanUser: (userId: String) -> Unit,
    onDisableOrRestoreUser: (userId: String, disable: Boolean) -> Unit,
    onDeleteUser: (userId: String) -> Unit,
    onUpdateReportStatus: (reportId: String, status: String, notes: String) -> Unit,
    onModerateRequest: (requestId: String, newStatus: String, reason: String) -> Unit,
    onVerifyDonationRecord: (recordId: String, status: String, notes: String) -> Unit,
    onSetDonorVerification: (donorId: String, isPhoneVerified: Boolean, isAdminReviewed: Boolean) -> Unit,
    onSaveEligibilityRule: (EligibilityRule) -> Unit,
    onAdminCorrectDonorDates: (donorId: String, lastDate: String, lastTime: String, nextDate: String, type: String, customType: String, interval: Long, ruleVer: String, count: Long, wb: Long?, plt: Long?, pls: Long?, oth: Long?) -> Unit,
    onCreateAnnouncement: (title: String, message: String, type: String, isEmergency: Boolean) -> Unit,
    onDeleteAnnouncement: (announcementId: String) -> Unit,
    onSaveAppConfig: (AppConfig) -> Unit,
    onOpenDonorProfile: (DonorProfile) -> Unit
) {
    BackHandler { onNavigateBack() }
    val clipboardManager = LocalClipboardManager.current
    var showRulesDialog by remember { mutableStateOf(false) }
    var selectedRecordForCorrection by remember { mutableStateOf<DonationRecord?>(null) }

    val pendingReportsCount = when (reportsState) {
        is UiState.Success -> reportsState.data.count { it.status == "pending" }
        else -> 0
    }
    val pendingRequestsCount = when (requestsState) {
        is UiState.Success -> requestsState.data.count { it.resolvedStatus() == "pending" }
        else -> 0
    }
    val pendingDonationsCount = when (donationHistoryState) {
        is UiState.Success -> donationHistoryState.data.count { it.verificationStatus == "pending" }
        else -> 0
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MbdCrimson)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.adminPanelTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                        }
                        Text(
                            text = "Owner Admin • ${adminEmail.ifBlank { currentUserId }}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "MBD Shield",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 8 Sub-Section Tabs matching Requirement 12
                ScrollableTabRow(
                    selectedTabIndex = activeSubSection.ordinal,
                    containerColor = MbdCrimson,
                    contentColor = Color.White,
                    edgePadding = 12.dp
                ) {
                    AdminSubSection.entries.forEach { section ->
                        val isSelected = section == activeSubSection
                        val badgeCount = when (section) {
                            AdminSubSection.REPORTS -> pendingReportsCount
                            AdminSubSection.REQUESTS -> pendingRequestsCount
                            AdminSubSection.DONOR_VERIFICATION -> pendingDonationsCount
                            else -> 0
                        }
                        Tab(
                            selected = isSelected,
                            onClick = { onSelectSubSection(section) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (section) {
                                            AdminSubSection.DASHBOARD -> strings.adminDashboardTab
                                            AdminSubSection.USERS -> strings.adminUsersTab
                                            AdminSubSection.REPORTS -> strings.adminReportsTab
                                            AdminSubSection.REQUESTS -> strings.adminRequestsTab
                                            AdminSubSection.DONOR_VERIFICATION -> strings.adminVerificationTab
                                            AdminSubSection.LOGS -> strings.adminLogsTab
                                            AdminSubSection.NOTIFICATIONS -> strings.adminAnnouncementsTab
                                            AdminSubSection.APP_CONFIG -> strings.adminConfigTab
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    if (badgeCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$badgeCount",
                                                    color = MbdCrimson,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeSubSection) {
                AdminSubSection.DASHBOARD -> AdminDashboardView(
                    strings = strings,
                    currentUserId = currentUserId,
                    usersState = usersState,
                    bansState = bansState,
                    reportsState = reportsState,
                    requestsState = requestsState,
                    donorsState = donorsState,
                    donationHistoryState = donationHistoryState,
                    logsState = logsState,
                    onNavigateSection = onSelectSubSection
                )
                AdminSubSection.USERS -> AdminUsersView(
                    strings = strings,
                    currentUserId = currentUserId,
                    usersState = usersState,
                    bansState = bansState,
                    donorsState = donorsState,
                    onBanUser = onBanUser,
                    onUnbanUser = onUnbanUser,
                    onDisableOrRestoreUser = onDisableOrRestoreUser,
                    onDeleteUser = onDeleteUser,
                    onOpenDonorProfile = onOpenDonorProfile
                )
                AdminSubSection.REPORTS -> AdminReportsView(
                    strings = strings,
                    reportsState = reportsState,
                    onUpdateReportStatus = onUpdateReportStatus,
                    onModerateRequest = onModerateRequest,
                    onBanUser = onBanUser
                )
                AdminSubSection.REQUESTS -> AdminRequestsView(
                    strings = strings,
                    requestsState = requestsState,
                    onModerateRequest = onModerateRequest
                )
                AdminSubSection.DONOR_VERIFICATION -> AdminDonorVerificationView(
                    strings = strings,
                    donorsState = donorsState,
                    donationHistoryState = donationHistoryState,
                    onVerifyDonationRecord = onVerifyDonationRecord,
                    onSetDonorVerification = onSetDonorVerification,
                    onOpenCorrectDates = { rec -> selectedRecordForCorrection = rec },
                    onOpenEligibilityRules = { showRulesDialog = true }
                )
                AdminSubSection.LOGS -> AdminLogsView(
                    strings = strings,
                    logsState = logsState
                )
                AdminSubSection.NOTIFICATIONS -> AdminAnnouncementsView(
                    strings = strings,
                    announcementsState = announcementsState,
                    onCreateAnnouncement = onCreateAnnouncement,
                    onDeleteAnnouncement = onDeleteAnnouncement
                )
                AdminSubSection.APP_CONFIG -> AdminAppConfigView(
                    strings = strings,
                    appConfig = appConfig,
                    onSaveAppConfig = onSaveAppConfig,
                    onOpenEligibilityRules = { showRulesDialog = true }
                )
            }
        }
    }

    if (showRulesDialog) {
        AdminEligibilityRulesDialog(
            strings = strings,
            currentRules = eligibilityRules,
            onDismiss = { showRulesDialog = false },
            onSaveRuleAndRecalculate = onSaveEligibilityRule
        )
    }

    selectedRecordForCorrection?.let { rec ->
        val donor = (donorsState as? UiState.Success)?.data?.find { it.userId == rec.userId || it.id == rec.userId }
        AdminCorrectDonorDatesDialog(
            strings = strings,
            donorName = donor?.fullName ?: rec.recipientName.ifBlank { "MBD Donor" },
            initialLastDonationDate = rec.donationDate,
            initialNextEligibleDate = donor?.nextEligibleDate ?: "",
            initialIntervalDays = rec.eligibilityIntervalDays,
            initialTotalDonations = donor?.totalDonations ?: 1L,
            onDismiss = { selectedRecordForCorrection = null },
            onConfirmCorrection = { lastDate, nextDate, interval, totalCount ->
                onAdminCorrectDonorDates(
                    rec.userId,
                    lastDate,
                    rec.donationTime,
                    nextDate,
                    rec.donationType,
                    rec.customDonationTypeName,
                    interval,
                    rec.ruleVersion,
                    totalCount,
                    null,
                    null,
                    null,
                    null
                )
                selectedRecordForCorrection = null
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 1. Dashboard View with 10 Real Statistics
// -------------------------------------------------------------------------------------------------
@Composable
private fun AdminDashboardView(
    strings: MbdStrings,
    currentUserId: String,
    usersState: UiState<List<UserProfile>>,
    bansState: UiState<List<AdminUserBan>>,
    reportsState: UiState<List<AdminReport>>,
    requestsState: UiState<List<BloodRequest>>,
    donorsState: UiState<List<DonorProfile>>,
    donationHistoryState: UiState<List<DonationRecord>>,
    logsState: UiState<List<AdminLog>>,
    onNavigateSection: (AdminSubSection) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val totalUsers = (usersState as? UiState.Success)?.data?.size ?: 0
    val bannedUsers = (bansState as? UiState.Success)?.data?.size ?: 0
    val activeUsers = (totalUsers - bannedUsers).coerceAtLeast(0)
    val totalDonors = (donorsState as? UiState.Success)?.data?.size ?: 0
    val totalRequests = (requestsState as? UiState.Success)?.data?.size ?: 0
    val requestsList = (requestsState as? UiState.Success)?.data.orEmpty()
    val pendingRequests = requestsList.count { it.resolvedStatus() == "pending" }
    val emergencyRequests = requestsList.count { it.urgency.equals("Emergency", ignoreCase = true) && !it.isTerminal() }
    val completedRequests = requestsList.count { it.resolvedStatus() == "completed" }
    val pendingReports = (reportsState as? UiState.Success)?.data?.count { it.status == "pending" } ?: 0
    val pendingVerifications = (donationHistoryState as? UiState.Success)?.data?.count { it.verificationStatus == "pending" } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Owner Identity Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MbdCrimsonLight.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MbdCrimson.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MbdCrimson,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.adminSetupGuideTitle,
                        fontWeight = FontWeight.Bold,
                        color = MbdCrimson,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.adminSetupGuideDesc,
                    color = MbdTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Your UID: $currentUserId",
                        color = MbdTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    OutlinedButton(
                        onClick = { clipboardManager.setText(AnnotatedString(currentUserId)) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy UID",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (strings.isBangla) "লাইভ ডাটাবেজ পরিসংখ্যান" else "Live Database Metrics",
            fontWeight = FontWeight.Bold,
            color = MbdTextPrimary,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        // 10 Live Metrics Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricCard(
                title = strings.adminStatTotalUsers,
                value = "$totalUsers",
                icon = Icons.Default.Group,
                accentColor = Color(0xFF1976D2),
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = strings.adminStatActiveUsers,
                value = "$activeUsers",
                icon = Icons.Default.CheckCircle,
                accentColor = MbdAvailableGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricCard(
                title = strings.adminStatBannedUsers,
                value = "$bannedUsers",
                icon = Icons.Default.Block,
                accentColor = Color(0xFFE53935),
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = strings.adminStatTotalDonors,
                value = "$totalDonors",
                icon = Icons.Default.WaterDrop,
                accentColor = MbdCrimson,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricCard(
                title = strings.adminStatTotalRequests,
                value = "$totalRequests",
                icon = Icons.Default.Dashboard,
                accentColor = Color(0xFF7B1FA2),
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = strings.adminStatPendingRequests,
                value = "$pendingRequests",
                icon = Icons.Default.History,
                accentColor = Color(0xFFF57C00),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricCard(
                title = strings.adminStatEmergencyRequests,
                value = "$emergencyRequests",
                icon = Icons.Default.Warning,
                accentColor = Color(0xFFD32F2F),
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = strings.adminStatCompletedRequests,
                value = "$completedRequests",
                icon = Icons.Default.CheckCircle,
                accentColor = Color(0xFF388E3C),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricCard(
                title = strings.adminStatPendingReports,
                value = "$pendingReports",
                icon = Icons.Default.ReportProblem,
                accentColor = Color(0xFFC2185B),
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = strings.adminStatPendingVerifications,
                value = "$pendingVerifications",
                icon = Icons.Default.Verified,
                accentColor = Color(0xFF0097A7),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (strings.isBangla) "দ্রুত অ্যাকশন" else "Quick Shortcuts",
            fontWeight = FontWeight.Bold,
            color = MbdTextPrimary,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickShortcutChip(icon = Icons.Default.Group, label = strings.adminUsersTab, onClick = { onNavigateSection(AdminSubSection.USERS) })
            QuickShortcutChip(icon = Icons.Default.ReportProblem, label = strings.adminReportsTab, onClick = { onNavigateSection(AdminSubSection.REPORTS) })
            QuickShortcutChip(icon = Icons.Default.Dashboard, label = strings.adminRequestsTab, onClick = { onNavigateSection(AdminSubSection.REQUESTS) })
            QuickShortcutChip(icon = Icons.Default.Verified, label = strings.adminVerificationTab, onClick = { onNavigateSection(AdminSubSection.DONOR_VERIFICATION) })
            QuickShortcutChip(icon = Icons.Default.History, label = strings.adminLogsTab, onClick = { onNavigateSection(AdminSubSection.LOGS) })
            QuickShortcutChip(icon = Icons.Default.NotificationsActive, label = strings.adminAnnouncementsTab, onClick = { onNavigateSection(AdminSubSection.NOTIFICATIONS) })
            QuickShortcutChip(icon = Icons.Default.Settings, label = strings.adminConfigTab, onClick = { onNavigateSection(AdminSubSection.APP_CONFIG) })
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (strings.isBangla) "সর্বশেষ এডমিন অ্যাক্টিভিটি" else "Recent Admin Activity",
            fontWeight = FontWeight.Bold,
            color = MbdTextPrimary,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        val recentLogs = (logsState as? UiState.Success)?.data.orEmpty().take(5)
        if (recentLogs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MbdSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.noLogsFound,
                    color = MbdTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            recentLogs.forEach { log ->
                AdminLogItemRow(log = log)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MbdSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MbdTextPrimary
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MbdTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickShortcutChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MbdCrimsonLight,
        border = BorderStroke(1.dp, MbdCrimson.copy(alpha = 0.2f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MbdCrimson, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = MbdCrimson, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 2. User Management View
// -------------------------------------------------------------------------------------------------
@Composable
private fun AdminUsersView(
    strings: MbdStrings,
    currentUserId: String,
    usersState: UiState<List<UserProfile>>,
    bansState: UiState<List<AdminUserBan>>,
    donorsState: UiState<List<DonorProfile>>,
    onBanUser: (userId: String, reason: String) -> Unit,
    onUnbanUser: (userId: String) -> Unit,
    onDisableOrRestoreUser: (userId: String, disable: Boolean) -> Unit,
    onDeleteUser: (userId: String) -> Unit,
    onOpenDonorProfile: (DonorProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") }
    var userToBan by remember { mutableStateOf<UserProfile?>(null) }
    var banReasonInput by remember { mutableStateOf("") }
    var userToDelete by remember { mutableStateOf<UserProfile?>(null) }

    val allUsers = (usersState as? UiState.Success)?.data.orEmpty()
    val allBans = (bansState as? UiState.Success)?.data.orEmpty()
    val allDonors = (donorsState as? UiState.Success)?.data.orEmpty()
    val bannedUserIds = remember(allBans) { allBans.map { it.userId }.toSet() }

    val filteredUsers = remember(allUsers, allDonors, bannedUserIds, searchQuery, selectedFilter) {
        allUsers.filter { user ->
            val isBanned = bannedUserIds.contains(user.userId)
            val isDonor = allDonors.any { it.userId == user.userId }
            val matchesFilter = when (selectedFilter) {
                "active" -> !isBanned
                "banned" -> isBanned
                "donor" -> isDonor
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                user.fullName.lowercase().contains(q) ||
                user.email.lowercase().contains(q) ||
                user.phoneNumber.contains(q) ||
                user.userId.lowercase().contains(q) ||
                user.area.lowercase().contains(q)
            matchesFilter && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MbdSurface)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchUserPlaceholder, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_user_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MbdCrimson,
                    unfocusedBorderColor = MbdBorderGray
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    label = { Text("All (${allUsers.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
                )
                FilterChip(
                    selected = selectedFilter == "active",
                    onClick = { selectedFilter = "active" },
                    label = { Text("Active (${allUsers.size - bannedUserIds.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
                )
                FilterChip(
                    selected = selectedFilter == "banned",
                    onClick = { selectedFilter = "banned" },
                    label = { Text("Banned (${bannedUserIds.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
                )
                FilterChip(
                    selected = selectedFilter == "donor",
                    onClick = { selectedFilter = "donor" },
                    label = { Text("Donors (${allDonors.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
                )
            }
        }

        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noUsersFound,
                    color = MbdTextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers, key = { it.userId.ifBlank { it.id } }) { user ->
                    val isBanned = bannedUserIds.contains(user.userId)
                    val matchingDonor = allDonors.find { it.userId == user.userId }
                    val isOwnerAdmin = user.userId == currentUserId

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MbdSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isBanned) Color(0xFFFFEBEE) else MbdCrimsonLight,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = user.fullName.take(1).uppercase().ifBlank { "U" },
                                            fontWeight = FontWeight.Bold,
                                            color = if (isBanned) Color.Red else MbdCrimson,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.fullName.ifBlank { "MBD Member" },
                                            fontWeight = FontWeight.Bold,
                                            color = MbdTextPrimary,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isOwnerAdmin) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFE8F5E9)
                                            ) {
                                                Text(
                                                    text = "Owner Admin",
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${user.email.ifBlank { "No Email" }} • ${user.phoneNumber}",
                                        color = MbdTextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "UID: ${user.userId} • Blood: ${user.bloodGroup} • ${user.area}",
                                        color = MbdTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isBanned) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = if (isBanned) "Banned" else "Active",
                                        color = if (isBanned) Color(0xFFC62828) else Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MbdBorderGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (matchingDonor != null) {
                                    OutlinedButton(
                                        onClick = { onOpenDonorProfile(matchingDonor) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Donor Profile", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                if (!isOwnerAdmin) {
                                    if (isBanned) {
                                        Button(
                                            onClick = { onUnbanUser(user.userId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MbdAvailableGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(strings.adminUnbanUserBtn, fontSize = 11.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                userToBan = user
                                                banReasonInput = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(strings.adminBanUserBtn, fontSize = 11.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { userToDelete = user },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Account",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Ban Confirmation Dialog
    userToBan?.let { user ->
        AlertDialog(
            onDismissRequest = { userToBan = null },
            title = { Text(if (strings.isBangla) "ব্যবহারকারী স্থগিত করুন" else "Ban User", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("${if (strings.isBangla) "ব্যবহারকারী" else "User"}: ${user.fullName} (${user.userId})", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = banReasonInput,
                        onValueChange = { banReasonInput = it },
                        placeholder = { Text(if (strings.isBangla) "ব্যান করার কারণ লিখুন..." else "Enter ban reason...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = banReasonInput.ifBlank { "Violated terms and safety guidelines" }
                        onBanUser(user.userId, reason)
                        userToBan = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text(if (strings.isBangla) "স্থগিতকরণ নিশ্চিত করুন" else "Confirm Ban")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToBan = null }) { Text(strings.cancelBtn) }
            }
        )
    }

    // Delete Account Confirmation Dialog
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text(if (strings.isBangla) "অ্যাকাউন্ট মুছুন" else "Delete Account", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (strings.isBangla) {
                        "আপনি কি নিশ্চিত যে ${user.fullName} (${user.userId})-এর অ্যাকাউন্ট ও প্রোফাইল ডাটাবেজ থেকে মুছে ফেলতে চান? এই অ্যাকশনটি অডিট লগে রেকর্ড হবে।"
                    } else {
                        "Are you sure you want to delete the account and profile for ${user.fullName} (${user.userId})? This action will be recorded in audit logs."
                    },
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(user.userId)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(if (strings.isBangla) "অ্যাকাউন্ট মুছুন" else "Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) { Text(strings.cancelBtn) }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 3. Central Report Box View
// -------------------------------------------------------------------------------------------------
@Composable
private fun AdminReportsView(
    strings: MbdStrings,
    reportsState: UiState<List<AdminReport>>,
    onUpdateReportStatus: (reportId: String, status: String, notes: String) -> Unit,
    onModerateRequest: (requestId: String, newStatus: String, reason: String) -> Unit,
    onBanUser: (userId: String, reason: String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("all") }
    var reportForAction by remember { mutableStateOf<AdminReport?>(null) }
    var actionType by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val allReports = (reportsState as? UiState.Success)?.data.orEmpty()
    val filteredReports = remember(allReports, selectedFilter) {
        when (selectedFilter) {
            "pending" -> allReports.filter { it.status == "pending" }
            "reviewing" -> allReports.filter { it.status == "reviewing" }
            "resolved" -> allReports.filter { it.status == "resolved" }
            "rejected" -> allReports.filter { it.status == "rejected" }
            else -> allReports
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MbdSurface)
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "all",
                onClick = { selectedFilter = "all" },
                label = { Text("All (${allReports.size})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
            )
            FilterChip(
                selected = selectedFilter == "pending",
                onClick = { selectedFilter = "pending" },
                label = { Text("Pending (${allReports.count { it.status == "pending" }})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
            )
            FilterChip(
                selected = selectedFilter == "reviewing",
                onClick = { selectedFilter = "reviewing" },
                label = { Text("Reviewing (${allReports.count { it.status == "reviewing" }})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
            )
            FilterChip(
                selected = selectedFilter == "resolved",
                onClick = { selectedFilter = "resolved" },
                label = { Text("Resolved (${allReports.count { it.status == "resolved" }})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
            )
            FilterChip(
                selected = selectedFilter == "rejected",
                onClick = { selectedFilter = "rejected" },
                label = { Text("Rejected (${allReports.count { it.status == "rejected" }})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
            )
        }

        if (filteredReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noReportsFound,
                    color = MbdTextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredReports, key = { it.id }) { report ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MbdSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFFEBEE),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ReportProblem,
                                            contentDescription = null,
                                            tint = Color(0xFFC2185B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = report.category,
                                        fontWeight = FontWeight.Bold,
                                        color = MbdTextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Target: ${report.reportedType.uppercase()} • ID: ${report.reportedId}",
                                        color = MbdTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                val statusColor = when (report.status) {
                                    "resolved" -> Color(0xFF2E7D32)
                                    "reviewing" -> Color(0xFFF57C00)
                                    "rejected" -> Color.Gray
                                    else -> Color(0xFFD32F2F)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = statusColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = report.status.uppercase(),
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = report.description.ifBlank { "No detailed description provided." },
                                color = MbdTextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            if (report.adminNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF5F5F5),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Admin Note: ${report.adminNotes}",
                                        color = Color.DarkGray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MbdBorderGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (report.status != "reviewing" && report.status != "resolved") {
                                    OutlinedButton(
                                        onClick = {
                                            onUpdateReportStatus(report.id, "reviewing", "Admin started review")
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(strings.adminReviewingBtn, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                if (report.status != "resolved") {
                                    Button(
                                        onClick = {
                                            reportForAction = report
                                            actionType = "resolve"
                                            notesInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MbdAvailableGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(strings.adminResolveBtn, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                if (report.status != "rejected") {
                                    OutlinedButton(
                                        onClick = {
                                            reportForAction = report
                                            actionType = "reject"
                                            notesInput = ""
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(strings.adminRejectBtn, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reportForAction?.let { rep ->
        AlertDialog(
            onDismissRequest = { reportForAction = null },
            title = {
                Text(
                    text = if (actionType == "resolve") (if (strings.isBangla) "রিপোর্ট সমাধান করুন" else "Resolve Report") else (if (strings.isBangla) "রিপোর্ট প্রত্যাখ্যান করুন" else "Reject Report"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("${if (strings.isBangla) "রিপোর্ট আইডি" else "Report ID"}: ${rep.id}", fontSize = 12.sp, color = MbdTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        placeholder = { Text(if (strings.isBangla) "এডমিন রিভিউ নোট লিখুন..." else "Enter admin review note...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val status = if (actionType == "resolve") "resolved" else "rejected"
                        val notes = notesInput.ifBlank { "Action taken by administrator" }
                        onUpdateReportStatus(rep.id, status, notes)
                        reportForAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == "resolve") MbdAvailableGreen else Color.Gray
                    )
                ) {
                    Text(strings.confirmBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportForAction = null }) { Text(strings.cancelBtn) }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 4. Blood Request Management View
// -------------------------------------------------------------------------------------------------
@Composable
private fun AdminRequestsView(
    strings: MbdStrings,
    requestsState: UiState<List<BloodRequest>>,
    onModerateRequest: (requestId: String, newStatus: String, reason: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") }
    var requestToModerate by remember { mutableStateOf<BloodRequest?>(null) }
    var moderationReasonInput by remember { mutableStateOf("") }

    val allRequests = (requestsState as? UiState.Success)?.data.orEmpty()
    val phoneFrequencies = remember(allRequests) {
        allRequests.groupingBy { it.contactNumber.trim() }.eachCount()
    }

    val filteredRequests = remember(allRequests, searchQuery, selectedFilter) {
        allRequests.filter { req ->
            val status = req.resolvedStatus()
            val matchesFilter = when (selectedFilter) {
                "pending" -> status == "pending"
                "emergency" -> req.urgency.equals("Emergency", ignoreCase = true)
                "completed" -> status == "completed"
                "cancelled" -> status == "cancelled"
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                req.patientName.lowercase().contains(q) ||
                req.hospitalName.lowercase().contains(q) ||
                req.location.lowercase().contains(q) ||
                req.contactNumber.contains(q) ||
                req.bloodGroup.lowercase().contains(q)
            matchesFilter && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MbdSurface)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchRequestPlaceholder, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MbdCrimson,
                    unfocusedBorderColor = MbdBorderGray
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    label = { Text("All (${allRequests.size})") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
                )
                FilterChip(
                    selected = selectedFilter == "pending",
                    onClick = { selectedFilter = "pending" },
                    label = { Text("Pending (${allRequests.count { it.resolvedStatus() == "pending" }})") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
                )
                FilterChip(
                    selected = selectedFilter == "emergency",
                    onClick = { selectedFilter = "emergency" },
                    label = { Text("Emergency (${allRequests.count { it.urgency.equals("Emergency", ignoreCase = true) }})") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
                )
                FilterChip(
                    selected = selectedFilter == "completed",
                    onClick = { selectedFilter = "completed" },
                    label = { Text("Completed (${allRequests.count { it.resolvedStatus() == "completed" }})") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
                )
                FilterChip(
                    selected = selectedFilter == "cancelled",
                    onClick = { selectedFilter = "cancelled" },
                    label = { Text("Cancelled (${allRequests.count { it.resolvedStatus() == "cancelled" }})") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
                )
            }
        }

        if (filteredRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noRequestsFound,
                    color = MbdTextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRequests, key = { it.id }) { req ->
                    val isDuplicateRisk = (phoneFrequencies[req.contactNumber.trim()] ?: 0) > 1
                    val resolvedStatus = req.resolvedStatus()

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MbdSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MbdCrimson,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = req.bloodGroup,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = req.patientName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MbdTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (req.urgency.equals("Emergency", ignoreCase = true)) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFFEBEE)
                                            ) {
                                                Text(
                                                    text = "Emergency",
                                                    color = Color(0xFFC62828),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${req.hospitalName} • ${req.location}",
                                        color = MbdTextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${if (strings.isBangla) "ফোন" else "Phone"}: ${req.contactNumber} • ${if (strings.isBangla) "ব্যাগ" else "Bags"}: ${req.bagsNeeded} • ${if (strings.isBangla) "ধরনের নিয়ম" else "Type"}: ${req.donationTypeLabel(strings.isBangla)}",
                                        color = MbdTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (resolvedStatus) {
                                        "completed" -> Color(0xFFE8F5E9)
                                        "accepted" -> Color(0xFFE3F2FD)
                                        "donor_contacted" -> Color(0xFFFFF3E0)
                                        "cancelled" -> Color(0xFFFFEBEE)
                                        else -> Color(0xFFF5F5F5)
                                    }
                                ) {
                                    Text(
                                        text = resolvedStatus.replace("_", " ").uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = when (resolvedStatus) {
                                            "completed" -> Color(0xFF2E7D32)
                                            "accepted" -> Color(0xFF1565C0)
                                            "donor_contacted" -> Color(0xFFE65100)
                                            "cancelled" -> Color(0xFFC62828)
                                            else -> Color.DarkGray
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (isDuplicateRisk) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFF8E1),
                                    border = BorderStroke(1.dp, Color(0xFFFFE082))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFF57F17),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (strings.isBangla) "সতর্কতা: এই ফোন নম্বরে একাধিক আবেদন পাওয়া গেছে" else "Warning: Multiple requests found for this phone number",
                                            color = Color(0xFFF57F17),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MbdBorderGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (resolvedStatus != "completed") {
                                    Button(
                                        onClick = {
                                            onModerateRequest(req.id, "completed", "Marked completed by admin")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MbdAvailableGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(if (strings.isBangla) "সম্পন্ন চিহ্নিত করুন" else "Mark Completed", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                if (resolvedStatus != "cancelled") {
                                    OutlinedButton(
                                        onClick = {
                                            requestToModerate = req
                                            moderationReasonInput = ""
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(if (strings.isBangla) "বাতিল করুন" else "Cancel Request", fontSize = 11.sp)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            onModerateRequest(req.id, "pending", "Restored to active by admin")
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(if (strings.isBangla) "পুনরুদ্ধার করুন" else "Restore", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    requestToModerate?.let { req ->
        AlertDialog(
            onDismissRequest = { requestToModerate = null },
            title = { Text(if (strings.isBangla) "রক্তের আবেদন বাতিল করুন" else "Cancel Blood Request", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("${if (strings.isBangla) "রোগী" else "Patient"}: ${req.patientName} (${req.bloodGroup})", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = moderationReasonInput,
                        onValueChange = { moderationReasonInput = it },
                        placeholder = { Text(if (strings.isBangla) "বাতিল করার কারণ লিখুন..." else "Enter cancellation reason...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = moderationReasonInput.ifBlank { "Invalidated by administrator" }
                        onModerateRequest(req.id, "cancelled", reason)
                        requestToModerate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(if (strings.isBangla) "বাতিল নিশ্চিত করুন" else "Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { requestToModerate = null }) { Text(strings.cancelBtn) }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 5. Donor Verification & Donation Records View
// -------------------------------------------------------------------------------------------------
@Composable
private fun AdminDonorVerificationView(
    strings: MbdStrings,
    donorsState: UiState<List<DonorProfile>>,
    donationHistoryState: UiState<List<DonationRecord>>,
    onVerifyDonationRecord: (recordId: String, status: String, notes: String) -> Unit,
    onSetDonorVerification: (donorId: String, isPhoneVerified: Boolean, isAdminReviewed: Boolean) -> Unit,
    onOpenCorrectDates: (DonationRecord) -> Unit,
    onOpenEligibilityRules: () -> Unit
) {
    var subTab by remember { mutableStateOf(0) } // 0: Donations, 1: Donors
    val donationsList = (donationHistoryState as? UiState.Success)?.data.orEmpty()
    val donorsList = (donorsState as? UiState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MbdSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                label = { Text("${if (strings.isBangla) "রক্তদানের রেকর্ড" else "Donation Records"} (${donationsList.size})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                label = { Text("${if (strings.isBangla) "রক্তদাতা প্রোফাইল" else "Donor Profiles"} (${donorsList.size})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenEligibilityRules) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (strings.isBangla) "রুলস" else "Rules", fontSize = 12.sp)
            }
        }

        if (subTab == 0) {
            if (donationsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (strings.isBangla) "কোনো রক্তদানের তথ্য নেই।" else "No donation records found.", color = MbdTextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(donationsList, key = { it.id }) { rec ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MbdSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${rec.donorName.ifBlank { "Donor" }} • ${rec.donationType.replace("_", " ").uppercase()}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MbdTextPrimary
                                        )
                                        Text(
                                            text = "${if (strings.isBangla) "তারিখ" else "Date"}: ${rec.donationDate} ${rec.donationTime} • ${rec.hospitalName}",
                                            color = MbdTextSecondary,
                                            fontSize = 12.sp
                                        )
                                        if (rec.procedureConfirmation.isNotBlank()) {
                                            Text(
                                                text = "Slip / Ref: ${rec.procedureConfirmation}",
                                                color = MbdCrimson,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (rec.verificationStatus == "verified") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                    ) {
                                        Text(
                                            text = rec.verificationStatus.uppercase(),
                                            color = if (rec.verificationStatus == "verified") Color(0xFF2E7D32) else Color(0xFFE65100),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MbdBorderGray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenCorrectDates(rec) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.correctDatesBtn, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (rec.verificationStatus != "verified") {
                                        Button(
                                            onClick = { onVerifyDonationRecord(rec.id, "verified", "Approved by admin verification") },
                                            colors = ButtonDefaults.buttonColors(containerColor = MbdAvailableGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(strings.approveBtn, fontSize = 11.sp)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { onVerifyDonationRecord(rec.id, "rejected", "Rejected during admin re-audit") },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(strings.rejectBtn, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(donorsList, key = { it.id.ifBlank { it.userId } }) { donor ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MbdSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MbdCrimson,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = donor.bloodGroup,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = donor.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MbdTextPrimary
                                    )
                                    Text(
                                        text = "${donor.phoneNumber} • ${donor.area}",
                                        color = MbdTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(strings.phoneVerifiedLabel, fontSize = 12.sp, color = MbdTextSecondary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = donor.isPhoneVerified,
                                        onCheckedChange = { next ->
                                            onSetDonorVerification(donor.id.ifBlank { donor.userId }, next, donor.isAdminReviewed)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = MbdCrimson, checkedTrackColor = MbdCrimsonLight)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(strings.adminReviewedLabel, fontSize = 12.sp, color = MbdTextSecondary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = donor.isAdminReviewed,
                                        onCheckedChange = { next ->
                                            onSetDonorVerification(donor.id.ifBlank { donor.userId }, donor.isPhoneVerified, next)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = MbdCrimson, checkedTrackColor = MbdCrimsonLight)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 6. Activity Logs View
// -------------------------------------------------------------------------------------------------
@Composable
private fun AdminLogsView(
    strings: MbdStrings,
    logsState: UiState<List<AdminLog>>
) {
    var selectedActionType by remember { mutableStateOf("ALL") }
    val allLogs = (logsState as? UiState.Success)?.data.orEmpty()
    val filteredLogs = remember(allLogs, selectedActionType) {
        if (selectedActionType == "ALL") allLogs else allLogs.filter { it.actionType == selectedActionType }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MbdSurface)
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "USER_BANNED", "USER_UNBANNED", "REPORT_RESOLVED", "REQUEST_MODERATED", "DONATION_VERIFIED", "NOTIFICATION_SENT", "CONFIG_UPDATED").forEach { type ->
                FilterChip(
                    selected = selectedActionType == type,
                    onClick = { selectedActionType = type },
                    label = { Text(type.replace("_", " "), fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbdCrimsonLight, selectedLabelColor = MbdCrimson)
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো লগ রেকর্ড পাওয়া যায়নি।", color = MbdTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id.ifBlank { it.hashCode().toString() } }) { log ->
                    AdminLogItemRow(log = log)
                }
            }
        }
    }
}

@Composable
private fun AdminLogItemRow(log: AdminLog) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val timeStr = log.createdAt?.toDate()?.let { formatter.format(it) } ?: "Just now"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MbdSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    log.actionType.contains("BANNED") -> Color(0xFFFFEBEE)
                    log.actionType.contains("RESOLVED") || log.actionType.contains("VERIFIED") -> Color(0xFFE8F5E9)
                    else -> MbdCrimsonLight
                },
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            log.actionType.contains("USER") -> Icons.Default.Group
                            log.actionType.contains("REPORT") -> Icons.Default.ReportProblem
                            log.actionType.contains("REQUEST") -> Icons.Default.Dashboard
                            log.actionType.contains("DONATION") -> Icons.Default.Verified
                            log.actionType.contains("NOTIFICATION") -> Icons.Default.NotificationsActive
                            else -> Icons.Default.History
                        },
                        contentDescription = null,
                        tint = when {
                            log.actionType.contains("BANNED") -> Color(0xFFC62828)
                            log.actionType.contains("RESOLVED") || log.actionType.contains("VERIFIED") -> Color(0xFF2E7D32)
                            else -> MbdCrimson
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.actionType.replace("_", " "),
                        fontWeight = FontWeight.Bold,
                        color = MbdTextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = timeStr,
                        color = MbdTextSecondary,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "Target: ${log.targetId.ifBlank { "N/A" }} • Admin: ${log.adminEmail.ifBlank { log.adminUid }}",
                    color = MbdTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (log.reason.isNotBlank()) {
                    Text(
                        text = log.reason,
                        color = MbdTextPrimary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 7. Announcements View
// -------------------------------------------------------------------------------------------------
@Composable
private fun AdminAnnouncementsView(
    strings: MbdStrings,
    announcementsState: UiState<List<AdminAnnouncement>>,
    onCreateAnnouncement: (title: String, message: String, type: String, isEmergency: Boolean) -> Unit,
    onDeleteAnnouncement: (announcementId: String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var announcementType by remember { mutableStateOf("info") }
    var isEmergencyChecked by remember { mutableStateOf(false) }

    val allAnnouncements = (announcementsState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MbdCrimson,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Announcement")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (allAnnouncements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.noAnnouncementsFound, color = MbdTextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allAnnouncements, key = { it.id }) { ann ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (ann.isEmergency) Color(0xFFFFF5F5) else MbdSurface
                            ),
                            border = if (ann.isEmergency) BorderStroke(1.dp, Color(0xFFEF9A9A)) else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (ann.isEmergency) Icons.Default.Warning else Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (ann.isEmergency) Color(0xFFD32F2F) else MbdCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = ann.title,
                                        fontWeight = FontWeight.Bold,
                                        color = MbdTextPrimary,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDeleteAnnouncement(ann.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Announcement",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = ann.message,
                                    color = MbdTextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Broadcasted by: ${ann.createdByEmail.ifBlank { "Admin" }}",
                                    color = MbdTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(strings.adminBroadcastTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        placeholder = { Text(if (strings.isBangla) "শিরোনাম লিখুন..." else "Enter title...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text(if (strings.isBangla) "ঘোষণার বিস্তারিত লিখুন..." else "Enter announcement details...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isEmergencyChecked,
                            onCheckedChange = { isEmergencyChecked = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD32F2F))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.adminEmergencyToggle, fontSize = 12.sp, color = MbdTextPrimary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = titleInput.ifBlank { "Important Notice" }
                        val msg = messageInput.ifBlank { "Service update from MBD admin." }
                        val type = if (isEmergencyChecked) "emergency" else "info"
                        onCreateAnnouncement(title, msg, type, isEmergencyChecked)
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson)
                ) {
                    Text(strings.adminBroadcastSubmit)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(strings.cancelBtn) }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 8. App Management & Config View
// -------------------------------------------------------------------------------------------------
@Composable
private fun AdminAppConfigView(
    strings: MbdStrings,
    appConfig: AppConfig,
    onSaveAppConfig: (AppConfig) -> Unit,
    onOpenEligibilityRules: () -> Unit
) {
    var emergencyBannerText by remember { mutableStateOf(appConfig.emergencyBannerText) }
    var emergencyBannerActive by remember { mutableStateOf(appConfig.emergencyBannerActive) }
    var supportPhone by remember { mutableStateOf(appConfig.supportPhone) }
    var supportEmail by remember { mutableStateOf(appConfig.supportEmail) }
    var privacyPolicyUrl by remember { mutableStateOf(appConfig.privacyPolicyUrl) }
    var termsUrl by remember { mutableStateOf(appConfig.termsUrl) }
    var latestVersion by remember { mutableStateOf(appConfig.latestAppVersion) }
    var latestVersionCodeText by remember { mutableStateOf(appConfig.latestVersionCode.toString()) }
    var minVersionCodeText by remember { mutableStateOf(appConfig.minSupportedVersionCode.toString()) }
    var downloadUrl by remember { mutableStateOf(appConfig.downloadUrl) }
    var releaseNotesBn by remember { mutableStateOf(appConfig.releaseNotesBn) }
    var releaseNotesEn by remember { mutableStateOf(appConfig.releaseNotesEn) }
    var isUpdateMandatory by remember { mutableStateOf(appConfig.isUpdateMandatory) }
    var updateMessage by remember { mutableStateOf(appConfig.updateMessage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MbdSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (strings.isBangla) "জরুরি ব্যানার কনফিগারেশন" else "Emergency Banner Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (strings.isBangla) "ব্যানার প্রদর্শন সক্রিয় করুন:" else "Enable emergency banner:", fontSize = 13.sp, color = MbdTextSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = emergencyBannerActive,
                        onCheckedChange = { emergencyBannerActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MbdCrimson)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emergencyBannerText,
                    onValueChange = { emergencyBannerText = it },
                    placeholder = { Text(if (strings.isBangla) "ব্যানারে প্রদর্শিত টেক্সট লিখুন..." else "Enter text displayed in banner...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MbdSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (strings.isBangla) "সাপোর্ট ও যোগাযোগ" else "Support Contacts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = supportPhone,
                    onValueChange = { supportPhone = it },
                    label = { Text("Helpline Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = supportEmail,
                    onValueChange = { supportEmail = it },
                    label = { Text("Support Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MbdSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (strings.isBangla) "অ্যাপ সংস্করণ ও বাধ্যতামূলক আপডেট কনফিগারেশন" else "App Version & Mandatory Update", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (strings.isBangla) "বাধ্যতামূলক আপডেট (Mandatory Lock):" else "Mandatory Update Lock:", fontSize = 13.sp, color = MbdTextSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = isUpdateMandatory,
                        onCheckedChange = { isUpdateMandatory = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MbdCrimson)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latestVersion,
                        onValueChange = { latestVersion = it },
                        label = { Text("Latest Version Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = latestVersionCodeText,
                        onValueChange = { latestVersionCodeText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Version Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = minVersionCodeText,
                    onValueChange = { minVersionCodeText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Minimum Supported Version Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    label = { Text("Direct APK Download URL") },
                    placeholder = { Text("https://github.com/.../releases/download/.../app-release.apk") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = releaseNotesBn,
                    onValueChange = { releaseNotesBn = it },
                    label = { Text("Release Notes (বাংলা)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = releaseNotesEn,
                    onValueChange = { releaseNotesEn = it },
                    label = { Text("Release Notes (English)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = updateMessage,
                    onValueChange = { updateMessage = it },
                    label = { Text("Update Status Message") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MbdSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (strings.isBangla) "নীতিমালা ও শর্তাবলী" else "Privacy & Terms Links", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = privacyPolicyUrl,
                    onValueChange = { privacyPolicyUrl = it },
                    label = { Text("Privacy Policy URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = termsUrl,
                    onValueChange = { termsUrl = it },
                    label = { Text("Terms and Conditions URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Eligibility Rules Engine Shortcut
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MbdCrimsonLight),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenEligibilityRules() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, tint = MbdCrimson, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (strings.isBangla) "মেডিকেল এলিজিবিলিটি রুলস ইঞ্জিন" else "Medical Eligibility Rules Engine", fontWeight = FontWeight.Bold, color = MbdCrimson, fontSize = 14.sp)
                    Text(if (strings.isBangla) "রক্তদানের ধরন অনুযায়ী দিন ও বিরতির নিয়ম কনফিগার করুন" else "Configure interval rules by donation type", color = MbdTextSecondary, fontSize = 11.sp)
                }
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MbdCrimson)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val latestCode = latestVersionCodeText.toLongOrNull() ?: appConfig.latestVersionCode
                val minCode = minVersionCodeText.toLongOrNull() ?: appConfig.minSupportedVersionCode
                onSaveAppConfig(
                    appConfig.copy(
                        emergencyBannerText = emergencyBannerText.trim(),
                        emergencyBannerActive = emergencyBannerActive,
                        supportPhone = supportPhone.trim(),
                        supportEmail = supportEmail.trim(),
                        privacyPolicyUrl = privacyPolicyUrl.trim(),
                        termsUrl = termsUrl.trim(),
                        latestAppVersion = latestVersion.trim(),
                        latestVersionCode = latestCode,
                        minSupportedVersionCode = minCode,
                        downloadUrl = downloadUrl.trim(),
                        releaseNotesBn = releaseNotesBn.trim(),
                        releaseNotesEn = releaseNotesEn.trim(),
                        isUpdateMandatory = isUpdateMandatory,
                        updateMessage = updateMessage.trim()
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (strings.isBangla) "গ্লোবাল কনফিগারেশন সংরক্ষণ করুন" else "Save Global Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

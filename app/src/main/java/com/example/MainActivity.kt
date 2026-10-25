package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.data.model.BloodRequest
import com.example.data.repository.MbdRepository
import com.example.ui.i18n.getMbdStrings
import com.example.ui.screens.AboutContactPrivacyScreen
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AppUpdateScreen
import com.example.ui.screens.AwarenessAndEligibilityScreen
import com.example.ui.screens.BloodRequestDetailsScreen
import com.example.ui.screens.DonorLiveMapViewScreen
import com.example.ui.screens.DonorProfileDetailsScreen
import com.example.ui.screens.FindDonorsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveLocationSharingStatusScreen
import com.example.ui.screens.LocationSharingEndedScreen
import com.example.ui.screens.MbdDrawerSheetContent
import com.example.ui.screens.MyDonationsScreen
import com.example.ui.screens.OnboardingAuthScreen
import com.example.ui.screens.ProfileAndDonorRegistrationScreen
import com.example.ui.screens.RequestBloodScreen
import com.example.ui.screens.RequestsAndNotificationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShareLiveLocationConfirmationScreen
import com.example.ui.screens.signOutMbd
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdCrimsonDark
import com.example.ui.theme.MbdCrimsonLight
import com.example.ui.theme.MbdPreferencesManager
import com.example.ui.theme.MbdSurface
import com.example.ui.theme.MbdTextSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MbdScreen
import com.example.ui.viewmodel.MbdViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

import android.content.Intent
import com.example.ui.util.MbdNotificationHelper

class MainActivity : ComponentActivity() {
    private var pendingTargetScreen: String? = null
    private var pendingRequestId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MbdPreferencesManager.ensureInitialized(this)
        MbdNotificationHelper.createNotificationChannels(this)
        enableEdgeToEdge()

        handleNotificationIntent(intent)

        setContent {
            MyApplicationTheme {
                MbdAppRoot(
                    initialTargetScreen = pendingTargetScreen,
                    initialRequestId = pendingRequestId,
                    onHandledDeepLink = {
                        pendingTargetScreen = null
                        pendingRequestId = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent != null) {
            val screen = intent.getStringExtra(MbdNotificationHelper.EXTRA_TARGET_SCREEN)
            val reqId = intent.getStringExtra(MbdNotificationHelper.EXTRA_REQUEST_ID)
            if (!screen.isNullOrBlank() || !reqId.isNullOrBlank()) {
                pendingTargetScreen = screen
                pendingRequestId = reqId
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun MbdAppRoot(
    auth: FirebaseAuth = Firebase.auth,
    initialTargetScreen: String? = null,
    initialRequestId: String? = null,
    onHandledDeepLink: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        MbdPreferencesManager.ensureInitialized(context)
    }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val savedLanguage by MbdPreferencesManager.languageFlow.collectAsStateWithLifecycle()
    var isGuestMode by remember { mutableStateOf(false) }

    val user = currentUser
    if (user != null) {
        MbdAuthenticatedShell(
            currentUserId = user.uid,
            defaultDisplayName = user.displayName ?: "MBD Member",
            defaultEmail = user.email ?: "",
            isGuest = false,
            onExitGuestMode = {},
            initialTargetScreen = initialTargetScreen,
            initialRequestId = initialRequestId,
            onHandledDeepLink = onHandledDeepLink
        )
    } else if (isGuestMode) {
        MbdAuthenticatedShell(
            currentUserId = "guest_preview_user",
            defaultDisplayName = "Md Shahin (Guest)",
            defaultEmail = "shahin@gmail.com",
            isGuest = true,
            onExitGuestMode = { isGuestMode = false },
            initialTargetScreen = initialTargetScreen,
            initialRequestId = initialRequestId,
            onHandledDeepLink = onHandledDeepLink
        )
    } else {
        OnboardingAuthScreen(
            language = savedLanguage,
            onToggleLanguage = {
                val nextLang = if (savedLanguage == "bn") "en" else "bn"
                MbdPreferencesManager.setLanguage(context, nextLang)
            },
            onAuthSuccess = {},
            onEnterGuestMode = { isGuestMode = true }
        )
    }
}

@Composable
fun MbdAuthenticatedShell(
    currentUserId: String,
    defaultDisplayName: String,
    defaultEmail: String,
    isGuest: Boolean = false,
    onExitGuestMode: () -> Unit = {},
    initialTargetScreen: String? = null,
    initialRequestId: String? = null,
    onHandledDeepLink: () -> Unit = {}
) {
    val viewModel: MbdViewModel = viewModel(
        key = "${currentUserId}_$isGuest",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MbdViewModel(
                    repository = MbdRepository(db),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName,
                    defaultEmail = defaultEmail,
                    isGuest = isGuest
                )
            }
        }
    )

    val language by viewModel.language.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedBloodGroup by viewModel.selectedBloodGroup.collectAsStateWithLifecycle()
    val availableOnly by viewModel.availableOnly.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val myDonorState by viewModel.myDonorState.collectAsStateWithLifecycle()
    val publicDonorsState by viewModel.publicDonorsState.collectAsStateWithLifecycle()
    val bloodRequestsState by viewModel.bloodRequestsState.collectAsStateWithLifecycle()
    val myDonationsState by viewModel.myDonationsState.collectAsStateWithLifecycle()
    val selectedRequestId by viewModel.selectedRequestId.collectAsStateWithLifecycle()
    val activeLiveSessionState by viewModel.activeLiveSessionState.collectAsStateWithLifecycle()
    val selectedDonorProfileState by viewModel.selectedDonorProfileState.collectAsStateWithLifecycle()
    val selectedDonorDonationsState by viewModel.selectedDonorDonationsState.collectAsStateWithLifecycle()
    val preselectedRequestDonor by viewModel.preselectedRequestDonor.collectAsStateWithLifecycle()
    val eligibilityRules by viewModel.eligibilityRulesState.collectAsStateWithLifecycle()

    val isUserBanned by viewModel.isCurrentUserBanned.collectAsStateWithLifecycle()
    val adminSubSection by viewModel.adminSubSection.collectAsStateWithLifecycle()
    val adminUsersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val adminBansState by viewModel.adminBansState.collectAsStateWithLifecycle()
    val adminReportsState by viewModel.adminReportsState.collectAsStateWithLifecycle()
    val adminLogsState by viewModel.adminLogsState.collectAsStateWithLifecycle()
    val adminAnnouncementsState by viewModel.adminAnnouncementsState.collectAsStateWithLifecycle()
    val appConfigState by viewModel.appConfigState.collectAsStateWithLifecycle()
    val allDonationHistoryState by viewModel.allDonationHistoryState.collectAsStateWithLifecycle()

    val donorsListState = rememberLazyListState()
    val strings = remember(language) { getMbdStrings(language) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(initialTargetScreen, initialRequestId, bloodRequestsState) {
        if (!initialRequestId.isNullOrBlank()) {
            val requests = (bloodRequestsState as? UiState.Success)?.data.orEmpty()
            val match = requests.find { it.id == initialRequestId }
            if (match != null) {
                viewModel.openRequestDetails(match)
            } else if (!initialTargetScreen.isNullOrBlank()) {
                when (initialTargetScreen.uppercase()) {
                    "DONOR_LIVE_MAP_VIEW" -> viewModel.navigateTo(MbdScreen.DONOR_LIVE_MAP_VIEW)
                    "NOTIFICATIONS" -> viewModel.navigateTo(MbdScreen.NOTIFICATIONS)
                    "REQUESTS_LIST" -> viewModel.navigateTo(MbdScreen.REQUESTS_LIST)
                    "ADMIN_PANEL" -> viewModel.navigateTo(MbdScreen.ADMIN_PANEL)
                    else -> viewModel.navigateTo(MbdScreen.REQUESTS_LIST)
                }
            }
            onHandledDeepLink()
        } else if (!initialTargetScreen.isNullOrBlank()) {
            when (initialTargetScreen.uppercase()) {
                "DONOR_LIVE_MAP_VIEW" -> viewModel.navigateTo(MbdScreen.DONOR_LIVE_MAP_VIEW)
                "NOTIFICATIONS" -> viewModel.navigateTo(MbdScreen.NOTIFICATIONS)
                "REQUESTS_LIST" -> viewModel.navigateTo(MbdScreen.REQUESTS_LIST)
                "ADMIN_PANEL" -> viewModel.navigateTo(MbdScreen.ADMIN_PANEL)
                else -> viewModel.navigateTo(MbdScreen.HOME)
            }
            onHandledDeepLink()
        }
    }

    val userProfile = (userProfileState as? UiState.Success)?.data
    val myDonor = (myDonorState as? UiState.Success)?.data
    val displayUserName = userProfile?.fullName?.ifBlank { defaultDisplayName } ?: defaultDisplayName
    val displayEmail = userProfile?.email?.ifBlank { defaultEmail } ?: defaultEmail
    val displayBloodGroup = userProfile?.bloodGroup ?: myDonor?.bloodGroup

    val openRequestsCount = when (val reqs = bloodRequestsState) {
        is UiState.Success -> reqs.data.count { it.status == "open" }
        else -> 0
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MbdDrawerSheetContent(
                strings = strings,
                userName = displayUserName,
                userEmail = displayEmail,
                userBloodGroup = displayBloodGroup,
                openRequestsCount = openRequestsCount,
                currentScreen = currentScreen,
                isAdmin = viewModel.isAdminUser,
                onSelectScreen = { target ->
                    scope.launch { drawerState.close() }
                    viewModel.navigateTo(target)
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    if (isGuest) {
                        onExitGuestMode()
                    } else {
                        signOutMbd(
                            credentialManager = credentialManager,
                            onSignOutComplete = {},
                            scope = scope
                        )
                    }
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                // Bottom Navigation Bar matching Reference Screen 2 (Home, Donors, Requests, Profile)
                NavigationBar(
                    containerColor = MbdSurface,
                    tonalElevation = 8.dp
                ) {
                    val isHome = currentScreen == MbdScreen.HOME
                    val isDonors = currentScreen == MbdScreen.FIND_DONORS ||
                        currentScreen == MbdScreen.DONOR_PROFILE_DETAILS
                    val isRequests = currentScreen == MbdScreen.REQUESTS_LIST ||
                        currentScreen == MbdScreen.REQUEST_BLOOD ||
                        currentScreen == MbdScreen.REQUEST_DETAILS ||
                        currentScreen == MbdScreen.SHARE_LIVE_LOCATION_CONFIRM ||
                        currentScreen == MbdScreen.LIVE_LOCATION_SHARING_STATUS ||
                        currentScreen == MbdScreen.DONOR_LIVE_MAP_VIEW ||
                        currentScreen == MbdScreen.LOCATION_SHARING_ENDED
                    val isProfile = currentScreen == MbdScreen.PROFILE

                    NavigationBarItem(
                        selected = isHome,
                        onClick = { viewModel.navigateTo(MbdScreen.HOME) },
                        icon = {
                            Icon(
                                imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = strings.navHome
                            )
                        },
                        label = {
                            Text(
                                text = strings.navHome,
                                fontWeight = if (isHome) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MbdCrimson,
                            selectedTextColor = MbdCrimson,
                            indicatorColor = MbdCrimsonLight,
                            unselectedIconColor = MbdTextSecondary,
                            unselectedTextColor = MbdTextSecondary
                        ),
                        modifier = Modifier.testTag("bottom_nav_home")
                    )

                    NavigationBarItem(
                        selected = isDonors,
                        onClick = { viewModel.navigateTo(MbdScreen.FIND_DONORS) },
                        icon = {
                            Icon(
                                imageVector = if (isDonors) Icons.Filled.Groups else Icons.Outlined.Groups,
                                contentDescription = strings.navDonors
                            )
                        },
                        label = {
                            Text(
                                text = strings.navDonors,
                                fontWeight = if (isDonors) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MbdCrimson,
                            selectedTextColor = MbdCrimson,
                            indicatorColor = MbdCrimsonLight,
                            unselectedIconColor = MbdTextSecondary,
                            unselectedTextColor = MbdTextSecondary
                        ),
                        modifier = Modifier.testTag("bottom_nav_donors")
                    )

                    NavigationBarItem(
                        selected = isRequests,
                        onClick = { viewModel.navigateTo(MbdScreen.REQUESTS_LIST) },
                        icon = {
                            Icon(
                                imageVector = if (isRequests) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday,
                                contentDescription = strings.navRequests
                            )
                        },
                        label = {
                            Text(
                                text = strings.navRequests,
                                fontWeight = if (isRequests) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MbdCrimson,
                            selectedTextColor = MbdCrimson,
                            indicatorColor = MbdCrimsonLight,
                            unselectedIconColor = MbdTextSecondary,
                            unselectedTextColor = MbdTextSecondary
                        ),
                        modifier = Modifier.testTag("bottom_nav_requests")
                    )

                    NavigationBarItem(
                        selected = isProfile,
                        onClick = { viewModel.navigateTo(MbdScreen.PROFILE) },
                        icon = {
                            Icon(
                                imageVector = if (isProfile) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = strings.navProfile
                            )
                        },
                        label = {
                            Text(
                                text = strings.navProfile,
                                fontWeight = if (isProfile) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MbdCrimson,
                            selectedTextColor = MbdCrimson,
                            indicatorColor = MbdCrimsonLight,
                            unselectedIconColor = MbdTextSecondary,
                            unselectedTextColor = MbdTextSecondary
                        ),
                        modifier = Modifier.testTag("bottom_nav_profile")
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Guest Mode indicator bar with quick "Google Sign-In" exit button
                if (isGuest) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MbdCrimsonDark)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.guestModeBanner,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onExitGuestMode() }
                                .testTag("exit_guest_mode_button")
                        ) {
                            Text(
                                text = if (strings.isBangla) "Google লগইন" else "Sign In",
                                color = MbdCrimson,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (isUserBanned) {
                    val banRecord = (adminBansState as? UiState.Success)?.data?.find { it.userId == currentUserId }
                    val banReason = banRecord?.reason?.ifBlank { null }
                    Surface(
                        color = Color(0xFFB00020),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.userBannedNotice,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (!banReason.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${strings.banReasonLabel}: $banReason",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                val allRequests = (bloodRequestsState as? UiState.Success<*>)?.data as? List<BloodRequest> ?: emptyList()
                val activeSelectedRequest: BloodRequest? = remember(allRequests, selectedRequestId) {
                    allRequests.find { it.id == selectedRequestId } ?: allRequests.firstOrNull()
                }

                val currentAppConfig = (appConfigState as? UiState.Success<*>)?.data as? com.example.data.model.AppConfig ?: com.example.data.model.AppConfig()
                val updateManager = remember(context) { com.example.util.UpdateManager(context) }
                val updateInfo = remember(currentAppConfig) { updateManager.checkUpdate(currentAppConfig) }

                if (updateInfo.isMandatory && updateInfo.requiresUpdate) {
                    com.example.ui.screens.MandatoryUpdateScreen(
                        updateInfo = updateInfo,
                        isBangla = strings.isBangla,
                        updateManager = updateManager
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        MbdScreen.HOME -> {
                            HomeScreen(
                                strings = strings,
                                currentUserId = currentUserId,
                                myDonorState = myDonorState,
                                bloodRequestsState = bloodRequestsState,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                onNavigate = { viewModel.navigateTo(it) },
                                onOpenRequestDetails = { req -> viewModel.openRequestDetails(req) },
                                onOpenDonorLiveMap = { req -> viewModel.openDonorLiveMapForRequest(req) },
                                onMarkRequestStatus = { id, status -> viewModel.markRequestStatus(id, status) },
                                onDeleteRequest = { id -> viewModel.deleteRequest(id) }
                            )
                        }
                        MbdScreen.FIND_DONORS -> {
                            FindDonorsScreen(
                                strings = strings,
                                donorsState = publicDonorsState,
                                searchQuery = searchQuery,
                                selectedBloodGroup = selectedBloodGroup,
                                availableOnly = availableOnly,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onSelectBloodGroup = { viewModel.setSelectedBloodGroup(it) },
                                onToggleAvailableOnly = { viewModel.setAvailableOnly(it) },
                                onBack = { viewModel.navigateBack() },
                                onRegisterDonorClick = { viewModel.navigateTo(MbdScreen.PROFILE) },
                                onDonorClick = { donor -> viewModel.openDonorProfileDetails(donor) },
                                listState = donorsListState
                            )
                        }
                        MbdScreen.DONOR_PROFILE_DETAILS -> {
                            val selectedDonor = (selectedDonorProfileState as? UiState.Success)?.data
                            val donorKey = selectedDonor?.id?.ifBlank { selectedDonor.userId } ?: ""
                            val authorizedLiveReq = remember(allRequests, donorKey, currentUserId) {
                                val now = System.currentTimeMillis()
                                allRequests.firstOrNull { req ->
                                    req.status == "open" &&
                                        req.liveSessionActive &&
                                        req.liveSessionExpiresAt > now &&
                                        (
                                            (req.userId == donorKey && req.authorizedDonorId == currentUserId) ||
                                                (req.userId == currentUserId && req.authorizedDonorId == donorKey)
                                            )
                                }
                            }
                            DonorProfileDetailsScreen(
                                donorState = selectedDonorProfileState,
                                donorDonationsState = selectedDonorDonationsState,
                                eligibilityRules = eligibilityRules,
                                strings = strings,
                                currentUserId = currentUserId,
                                isAdminUser = viewModel.isAdminUser,
                                isSaving = isSaving,
                                authorizedLiveRequest = authorizedLiveReq,
                                onBack = { viewModel.navigateBack() },
                                onRefreshDonor = { viewModel.refreshSelectedDonor() },
                                onSendBloodRequestToDonor = { donor ->
                                    viewModel.openRequestBloodForDonor(donor)
                                },
                                onViewAuthorizedLiveLocation = { req ->
                                    viewModel.openDonorLiveMapForRequest(req)
                                },
                                onRecordBloodDonation = { date, time, type, customTypeName, procConfirm, interval, ruleVer, hosp, loc, rec, bg, notes, verStatus ->
                                    viewModel.recordBloodDonation(
                                        donationDate = date,
                                        donationTime = time,
                                        donationType = type,
                                        customDonationTypeName = customTypeName,
                                        procedureConfirmation = procConfirm,
                                        eligibilityIntervalDays = interval,
                                        ruleVersion = ruleVer,
                                        hospitalName = hosp,
                                        location = loc,
                                        recipientName = rec,
                                        bloodGroup = bg,
                                        notes = notes,
                                        verificationStatus = verStatus
                                    )
                                },
                                onAdminCorrectDonorDates = { donor, lastD, nextD, intervalD, totalD ->
                                    viewModel.adminCorrectDonorDates(donor, lastD, nextD, intervalD, totalD)
                                },
                                onSaveEligibilityRule = { updatedRule ->
                                    viewModel.saveEligibilityRuleAndRecalculate(updatedRule)
                                },
                                onApproveOrUpdateDonationRecord = { record, newStatus ->
                                    viewModel.verifyOrUpdateDonationRecord(record, newStatus)
                                },
                                onDeleteOwnDonationRecord = { id ->
                                    viewModel.deleteDonation(id)
                                },
                                onReportDonor = { donorId, reason ->
                                    viewModel.reportDonorProfile(donorId, reason)
                                }
                            )
                        }
                        MbdScreen.REQUEST_BLOOD -> {
                            RequestBloodScreen(
                                strings = strings,
                                defaultPhoneNumber = userProfile?.phoneNumber ?: myDonor?.phoneNumber ?: "",
                                isSaving = isSaving,
                                preselectedDonor = preselectedRequestDonor,
                                onClearPreselectedDonor = { viewModel.clearPreselectedRequestDonor() },
                                onBack = { viewModel.navigateBack() },
                                onSubmitRequest = { bg, hosp, loc, pat, phone, dtNeeded, bags, urg, notes, donationType ->
                                    viewModel.submitBloodRequest(
                                        bloodGroup = bg,
                                        hospitalName = hosp,
                                        location = loc,
                                        patientName = pat,
                                        contactNumber = phone,
                                        neededDateTime = dtNeeded,
                                        bagsNeeded = bags,
                                        urgency = urg,
                                        notes = notes,
                                        donationType = donationType,
                                        onSuccess = {
                                            viewModel.navigateTo(MbdScreen.REQUEST_DETAILS)
                                        }
                                    )
                                }
                            )
                        }
                        MbdScreen.REQUESTS_LIST -> {
                            RequestsAndNotificationsScreen(
                                title = strings.recentRequests,
                                strings = strings,
                                currentUserId = currentUserId,
                                bloodRequestsState = bloodRequestsState,
                                onBack = { viewModel.navigateBack() },
                                onCreateRequestClick = { viewModel.navigateTo(MbdScreen.REQUEST_BLOOD) },
                                onOpenRequestDetails = { req -> viewModel.openRequestDetails(req) },
                                onOpenDonorLiveMap = { req -> viewModel.openDonorLiveMapForRequest(req) },
                                onMarkRequestStatus = { id, status -> viewModel.markRequestStatus(id, status) },
                                onDeleteRequest = { id -> viewModel.deleteRequest(id) }
                            )
                        }
                        MbdScreen.NOTIFICATIONS -> {
                            RequestsAndNotificationsScreen(
                                title = strings.notifications,
                                strings = strings,
                                currentUserId = currentUserId,
                                bloodRequestsState = bloodRequestsState,
                                onBack = { viewModel.navigateBack() },
                                onCreateRequestClick = { viewModel.navigateTo(MbdScreen.REQUEST_BLOOD) },
                                onOpenRequestDetails = { req -> viewModel.openRequestDetails(req) },
                                onOpenDonorLiveMap = { req -> viewModel.openDonorLiveMapForRequest(req) },
                                onMarkRequestStatus = { id, status -> viewModel.markRequestStatus(id, status) },
                                onDeleteRequest = { id -> viewModel.deleteRequest(id) }
                            )
                        }
                        MbdScreen.REQUEST_DETAILS -> {
                            val req = activeSelectedRequest
                            if (req != null) {
                                BloodRequestDetailsScreen(
                                    request = req,
                                    strings = strings,
                                    currentUserId = currentUserId,
                                    liveSessionState = activeLiveSessionState,
                                    isAdminUser = viewModel.isAdminUser,
                                    onBack = { viewModel.navigateBack() },
                                    onOpenShareConfirmation = {
                                        viewModel.navigateTo(MbdScreen.SHARE_LIVE_LOCATION_CONFIRM)
                                    },
                                    onOpenSharingStatus = {
                                        viewModel.navigateTo(MbdScreen.LIVE_LOCATION_SHARING_STATUS)
                                    },
                                    onOpenDonorLiveMap = {
                                        viewModel.navigateTo(MbdScreen.DONOR_LIVE_MAP_VIEW)
                                    },
                                    onStopSharingLocation = {
                                        viewModel.stopLiveLocationSharing(req.id, navigateToEndedScreen = true)
                                    },
                                    onUpdateStatus = { status ->
                                        viewModel.markRequestStatus(req.id, status)
                                    },
                                    onMarkFulfilled = {
                                        viewModel.markRequestStatus(req.id, "completed")
                                    },
                                    onDeleteRequest = {
                                        viewModel.deleteRequest(req.id)
                                        viewModel.navigateTo(MbdScreen.HOME)
                                    },
                                    onReportRequest = { reason ->
                                        viewModel.reportBloodRequest(req.id, reason)
                                    }
                                )
                            } else {
                                viewModel.navigateTo(MbdScreen.HOME)
                            }
                        }
                        MbdScreen.SHARE_LIVE_LOCATION_CONFIRM -> {
                            val req = activeSelectedRequest
                            if (req != null) {
                                ShareLiveLocationConfirmationScreen(
                                    request = req,
                                    strings = strings,
                                    donorsState = publicDonorsState,
                                    onBack = { viewModel.navigateBack() },
                                    onConfirmStartSharing = { donorId, donorName, durationMins, lat, lng, acc ->
                                        viewModel.startLiveLocationSharing(
                                            request = req,
                                            authorizedDonorId = donorId,
                                            authorizedDonorName = donorName,
                                            durationMinutes = durationMins,
                                            initialLat = lat,
                                            initialLng = lng,
                                            initialAcc = acc,
                                            onStarted = {
                                                viewModel.navigateTo(MbdScreen.LIVE_LOCATION_SHARING_STATUS)
                                            }
                                        )
                                    }
                                )
                            } else {
                                viewModel.navigateTo(MbdScreen.HOME)
                            }
                        }
                        MbdScreen.LIVE_LOCATION_SHARING_STATUS -> {
                            val req = activeSelectedRequest
                            if (req != null) {
                                LiveLocationSharingStatusScreen(
                                    request = req,
                                    strings = strings,
                                    liveSessionState = activeLiveSessionState,
                                    onBack = { viewModel.navigateBack() },
                                    onUpdateRealCoordinates = { lat, lng, acc ->
                                        viewModel.updateLiveLocationCoordinates(req.id, lat, lng, acc)
                                    },
                                    onStopSharingLocation = {
                                        viewModel.stopLiveLocationSharing(req.id, navigateToEndedScreen = true)
                                    },
                                    onOpenInteractiveMap = {
                                        viewModel.navigateTo(MbdScreen.DONOR_LIVE_MAP_VIEW)
                                    },
                                    onSessionExpired = {
                                        viewModel.stopLiveLocationSharing(req.id, navigateToEndedScreen = true)
                                    }
                                )
                            } else {
                                viewModel.navigateTo(MbdScreen.HOME)
                            }
                        }
                        MbdScreen.DONOR_LIVE_MAP_VIEW -> {
                            val req = activeSelectedRequest
                            if (req != null) {
                                DonorLiveMapViewScreen(
                                    request = req,
                                    strings = strings,
                                    currentUserId = currentUserId,
                                    liveSessionState = activeLiveSessionState,
                                    onBack = { viewModel.navigateBack() },
                                    onStopSharingLocation = {
                                        viewModel.stopLiveLocationSharing(req.id, navigateToEndedScreen = true)
                                    },
                                    onSessionEndedOrExpired = {
                                        viewModel.navigateTo(MbdScreen.LOCATION_SHARING_ENDED)
                                    }
                                )
                            } else {
                                viewModel.navigateTo(MbdScreen.HOME)
                            }
                        }
                        MbdScreen.LOCATION_SHARING_ENDED -> {
                            LocationSharingEndedScreen(
                                strings = strings,
                                onBackToRequestDetails = {
                                    viewModel.navigateTo(MbdScreen.REQUEST_DETAILS)
                                },
                                onBackToHome = {
                                    viewModel.navigateTo(MbdScreen.HOME)
                                }
                            )
                        }
                        MbdScreen.MY_DONATIONS -> {
                            MyDonationsScreen(
                                strings = strings,
                                defaultBloodGroup = displayBloodGroup ?: "A+",
                                myDonorProfile = myDonor,
                                donationsState = myDonationsState,
                                eligibilityRules = eligibilityRules,
                                isAdminUser = viewModel.isAdminUser,
                                isSaving = isSaving,
                                onBack = { viewModel.navigateBack() },
                                onRecordBloodDonation = { date, time, type, customTypeName, procConfirm, interval, ruleVer, hosp, loc, rec, bg, notes, verStatus ->
                                    viewModel.recordBloodDonation(
                                        donationDate = date,
                                        donationTime = time,
                                        donationType = type,
                                        customDonationTypeName = customTypeName,
                                        procedureConfirmation = procConfirm,
                                        eligibilityIntervalDays = interval,
                                        ruleVersion = ruleVer,
                                        hospitalName = hosp,
                                        location = loc,
                                        recipientName = rec,
                                        bloodGroup = bg,
                                        notes = notes,
                                        verificationStatus = verStatus
                                    )
                                },
                                onApproveOrUpdateDonationRecord = { record, newStatus ->
                                    viewModel.verifyOrUpdateDonationRecord(record, newStatus)
                                },
                                onAdminCorrectDonorDates = { donor, lastD, nextD, intervalD, totalD ->
                                    viewModel.adminCorrectDonorDates(donor, lastD, nextD, intervalD, totalD)
                                },
                                onSaveEligibilityRule = { updatedRule ->
                                    viewModel.saveEligibilityRuleAndRecalculate(updatedRule)
                                },
                                onDeleteDonation = { id -> viewModel.deleteDonation(id) }
                            )
                        }
                        MbdScreen.PROFILE -> {
                            ProfileAndDonorRegistrationScreen(
                                strings = strings,
                                defaultName = defaultDisplayName,
                                defaultEmail = defaultEmail,
                                userProfileState = userProfileState,
                                myDonorState = myDonorState,
                                myDonationsState = myDonationsState,
                                eligibilityRules = eligibilityRules,
                                isAdminUser = viewModel.isAdminUser,
                                isSaving = isSaving,
                                onBack = { viewModel.navigateBack() },
                                onSaveProfile = { name, phone, bg, area, addr, lastDate, availStatus, pub, notif, total, donType, intervalDays, showPhone, shareArea, showHistory ->
                                    viewModel.saveProfileAndDonorStatus(
                                        fullName = name,
                                        phoneNumber = phone,
                                        bloodGroup = bg,
                                        area = area,
                                        address = addr,
                                        lastDonationDate = lastDate,
                                        availabilityStatus = availStatus,
                                        isPublicDonor = pub,
                                        notificationsEnabled = notif,
                                        totalDonations = total,
                                        donationType = donType,
                                        eligibilityIntervalDays = intervalDays,
                                        showPhonePublicly = showPhone,
                                        shareLocationArea = shareArea,
                                        showDonationHistoryPublicly = showHistory
                                    )
                                },
                                onRecordBloodDonation = { date, time, type, customTypeName, procConfirm, interval, ruleVer, hosp, loc, rec, bg, notes, verStatus ->
                                    viewModel.recordBloodDonation(
                                        donationDate = date,
                                        donationTime = time,
                                        donationType = type,
                                        customDonationTypeName = customTypeName,
                                        procedureConfirmation = procConfirm,
                                        eligibilityIntervalDays = interval,
                                        ruleVersion = ruleVer,
                                        hospitalName = hosp,
                                        location = loc,
                                        recipientName = rec,
                                        bloodGroup = bg,
                                        notes = notes,
                                        verificationStatus = verStatus
                                    )
                                },
                                onAdminCorrectDonorDates = { donor, lastD, nextD, intervalD, totalD ->
                                    viewModel.adminCorrectDonorDates(donor, lastD, nextD, intervalD, totalD)
                                },
                                onSaveEligibilityRule = { updatedRule ->
                                    viewModel.saveEligibilityRuleAndRecalculate(updatedRule)
                                },
                                onApproveOrUpdateDonationRecord = { record, newStatus ->
                                    viewModel.verifyOrUpdateDonationRecord(record, newStatus)
                                },
                                onDeleteDonationRecord = { id ->
                                    viewModel.deleteDonation(id)
                                },
                                onLogout = {
                                    if (isGuest) {
                                        onExitGuestMode()
                                    } else {
                                        signOutMbd(
                                            credentialManager = credentialManager,
                                            onSignOutComplete = {},
                                            scope = scope
                                        )
                                    }
                                }
                            )
                        }
                        MbdScreen.AWARENESS -> {
                            AwarenessAndEligibilityScreen(
                                strings = strings,
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                        MbdScreen.SETTINGS -> {
                            SettingsScreen(
                                strings = strings,
                                currentLanguage = language,
                                currentThemeMode = themeMode,
                                onSelectLanguage = { lang ->
                                    MbdPreferencesManager.setLanguage(context, lang)
                                    viewModel.setLanguage(lang)
                                },
                                onSelectThemeMode = { mode ->
                                    MbdPreferencesManager.setThemeMode(context, mode)
                                    viewModel.setThemeMode(mode)
                                },
                                onOpenAppUpdate = {
                                    viewModel.navigateTo(MbdScreen.APP_UPDATE)
                                },
                                onOpenAbout = {
                                    viewModel.navigateTo(MbdScreen.ABOUT_CONTACT_PRIVACY)
                                },
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                        MbdScreen.APP_UPDATE -> {
                            com.example.ui.screens.MandatoryUpdateScreen(
                                updateInfo = updateInfo,
                                isBangla = strings.isBangla,
                                updateManager = updateManager,
                                onSkipOptionalUpdate = { viewModel.navigateBack() }
                            )
                        }
                        MbdScreen.ABOUT_CONTACT_PRIVACY -> {
                            AboutContactPrivacyScreen(
                                strings = strings,
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                        MbdScreen.ADMIN_PANEL -> {
                            AdminPanelScreen(
                                strings = strings,
                                currentUserId = currentUserId,
                                adminEmail = displayEmail,
                                activeSubSection = adminSubSection,
                                onSelectSubSection = { viewModel.setAdminSubSection(it) },
                                onNavigateBack = { viewModel.navigateTo(MbdScreen.HOME) },
                                usersState = adminUsersState,
                                bansState = adminBansState,
                                reportsState = adminReportsState,
                                requestsState = bloodRequestsState,
                                donorsState = publicDonorsState,
                                donationHistoryState = allDonationHistoryState,
                                logsState = adminLogsState,
                                announcementsState = adminAnnouncementsState,
                                appConfig = appConfigState,
                                eligibilityRules = eligibilityRules,
                                onBanUser = { uid, reason -> viewModel.banUser(uid, reason) },
                                onUnbanUser = { uid -> viewModel.unbanUser(uid) },
                                onDisableOrRestoreUser = { uid, disable -> viewModel.disableOrRestoreUser(uid, disable) },
                                onDeleteUser = { uid -> viewModel.deleteUserAccount(uid) },
                                onUpdateReportStatus = { rId, status, notes -> viewModel.updateReportStatus(rId, status, notes) },
                                onModerateRequest = { rId, status, reason -> viewModel.adminModerateBloodRequest(rId, status, reason) },
                                onVerifyDonationRecord = { rId, status, notes -> viewModel.adminVerifyDonationRecord(rId, status, notes) },
                                onSetDonorVerification = { dId, isPhone, isAdminRev -> viewModel.adminSetDonorVerificationStatus(dId, isPhone, isAdminRev) },
                                onSaveEligibilityRule = { rule -> viewModel.saveEligibilityRuleAndRecalculate(rule) },
                                onAdminCorrectDonorDates = { donorId, lastDate, lastTime, nextDate, type, customType, interval, ruleVer, count, wb, plt, pls, oth ->
                                    viewModel.adminCorrectDonorDates(donorId, lastDate, lastTime, nextDate, type, customType, interval, ruleVer, count, wb, plt, pls, oth)
                                },
                                onCreateAnnouncement = { title, msg, type, isEmerg -> viewModel.createBroadcastAnnouncement(title, msg, type, isEmerg) },
                                onDeleteAnnouncement = { aId -> viewModel.deleteAnnouncement(aId) },
                                onSaveAppConfig = { cfg -> viewModel.saveAppConfig(cfg) },
                                onOpenDonorProfile = { donor -> viewModel.openDonorProfileDetails(donor) }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

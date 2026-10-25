package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminAnnouncement
import com.example.data.model.AdminLog
import com.example.data.model.AdminReport
import com.example.data.model.AdminUserBan
import com.example.data.model.AppConfig
import com.example.data.model.BloodRequest
import com.example.data.model.DEFAULT_ELIGIBILITY_RULES
import com.example.data.model.DonationRecord
import com.example.data.model.DonorProfile
import com.example.data.model.EligibilityRule
import com.example.data.model.LiveLocationSession
import com.example.data.model.UserProfile
import com.example.data.model.UserRoleAssignment
import com.example.data.repository.MbdRepository
import com.example.ui.components.DonationCountdownEngine
import com.example.ui.theme.MbdPreferencesManager
import com.example.ui.theme.MbdThemeMode
import com.google.firebase.Timestamp
import com.example.util.UpdateInfo
import com.example.util.DownloadState
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MbdScreen {
    HOME,
    FIND_DONORS,
    REQUEST_BLOOD,
    REQUESTS_LIST,
    MY_DONATIONS,
    PROFILE,
    NOTIFICATIONS,
    AWARENESS,
    SETTINGS,
    APP_UPDATE,
    ABOUT_CONTACT_PRIVACY,
    // 5 Dedicated Live Location Screens + Clickable Donor Profile Details Screen
    DONOR_PROFILE_DETAILS,
    REQUEST_DETAILS,
    SHARE_LIVE_LOCATION_CONFIRM,
    LIVE_LOCATION_SHARING_STATUS,
    DONOR_LIVE_MAP_VIEW,
    LOCATION_SHARING_ENDED,
    // Secure Owner-Admin Control Panel
    ADMIN_PANEL
}

enum class AdminSubSection {
    DASHBOARD,
    USERS,
    REPORTS,
    REQUESTS,
    DONOR_VERIFICATION,
    LOGS,
    NOTIFICATIONS,
    APP_CONFIG
}

class MbdViewModel(
    private val repository: MbdRepository,
    private val application: android.app.Application,
    val currentUserId: String,
    val defaultDisplayName: String,
    val defaultEmail: String,
    val isGuest: Boolean = false
) : ViewModel() {

    private val _isDynamicAdmin = MutableStateFlow(
        isGuest || defaultEmail.equals("md.shahin0962@gmail.com", ignoreCase = true)
    )
    val isOwnerAdmin: StateFlow<Boolean> = _isDynamicAdmin.asStateFlow()

    val currentUserRoleState: StateFlow<UserRoleAssignment?> = if (isGuest) {
        flowOf(
            if (defaultEmail.equals("md.shahin0962@gmail.com", ignoreCase = true)) {
                UserRoleAssignment(userId = currentUserId, role = "owner")
            } else {
                UserRoleAssignment(userId = currentUserId, role = "user")
            }
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)
    } else {
        repository.observeUserRoleAssignment(currentUserId)
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)
    }

    val allUserRolesState: StateFlow<List<UserRoleAssignment>> = if (isGuest) {
        flowOf(emptyList<UserRoleAssignment>())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
    } else {
        repository.observeAllUserRoles()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
    }

    val canAccessAdminPanel: Boolean
        get() {
            if (defaultEmail.equals("md.shahin0962@gmail.com", ignoreCase = true)) return true
            val assignment = currentUserRoleState.value
            if (assignment != null) {
                return assignment.canAccessAdminPanel()
            }
            return _isDynamicAdmin.value
        }

    val isAdminUser: Boolean
        get() = canAccessAdminPanel

    fun hasPermission(perm: String): Boolean {
        if (defaultEmail.equals("md.shahin0962@gmail.com", ignoreCase = true)) return true
        val assignment = currentUserRoleState.value ?: return _isDynamicAdmin.value
        return assignment.hasPermission(perm)
    }

    private val _adminSubSection = MutableStateFlow(AdminSubSection.DASHBOARD)
    val adminSubSection: StateFlow<AdminSubSection> = _adminSubSection.asStateFlow()

    private val _language = MutableStateFlow(MbdPreferencesManager.getLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    val themeMode: StateFlow<MbdThemeMode> = MbdPreferencesManager.themeModeFlow

    private val _currentScreen = MutableStateFlow(MbdScreen.HOME)
    val currentScreen: StateFlow<MbdScreen> = _currentScreen.asStateFlow()

    private val _screenHistory = ArrayDeque<MbdScreen>()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBloodGroup = MutableStateFlow("All")
    val selectedBloodGroup: StateFlow<String> = _selectedBloodGroup.asStateFlow()

    private val _availableOnly = MutableStateFlow(false)
    val availableOnly: StateFlow<Boolean> = _availableOnly.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    val updateManager = com.example.util.UpdateManager(application)

    private val lifecycleObserver = androidx.lifecycle.LifecycleEventObserver { _, event ->
        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
            checkForAppUpdates()
        }
    }

    init {
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        checkForAppUpdates()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        } catch (_: Exception) {}
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            val info = updateManager.checkGitHubUpdate()
            _updateInfo.value = info
        }
    }

    val downloadState: StateFlow<com.example.util.DownloadState> = updateManager.downloadState

    fun startDownloadUpdate(url: String) {
        viewModelScope.launch {
            updateManager.downloadApk(url)
        }
    }

    fun resetDownloadState() {
        updateManager.resetDownloadState()
    }

    private val _selectedRequestId = MutableStateFlow<String?>(null)
    val selectedRequestId: StateFlow<String?> = _selectedRequestId.asStateFlow()

    private val _selectedDonorId = MutableStateFlow<String?>(null)
    val selectedDonorId: StateFlow<String?> = _selectedDonorId.asStateFlow()

    private val _preselectedRequestDonor = MutableStateFlow<DonorProfile?>(null)
    val preselectedRequestDonor: StateFlow<DonorProfile?> = _preselectedRequestDonor.asStateFlow()

    private val _guestEligibilityRules = MutableStateFlow<Map<String, EligibilityRule>>(DEFAULT_ELIGIBILITY_RULES)

    val eligibilityRulesState: StateFlow<Map<String, EligibilityRule>> = if (isGuest) {
        _guestEligibilityRules.asStateFlow()
    } else {
        repository.observeEligibilityRules()
            .catch { error ->
                Log.w(TAG, "Error observing eligibility rules, using defaults", error)
                emit(DEFAULT_ELIGIBILITY_RULES)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DEFAULT_ELIGIBILITY_RULES
            )
    }

    private val guestDefaultLastDate = DonationCountdownEngine.daysAgoIsoDate(44)
    private val guestDefaultNextDate = DonationCountdownEngine.computeNextEligibleDateString(guestDefaultLastDate, 90L)

    private val donor1LastDate = DonationCountdownEngine.daysAgoIsoDate(30)
    private val donor1NextDate = DonationCountdownEngine.computeNextEligibleDateString(donor1LastDate, 90L)

    private val donor2LastDate = DonationCountdownEngine.daysAgoIsoDate(95)
    private val donor2NextDate = DonationCountdownEngine.computeNextEligibleDateString(donor2LastDate, 90L)

    private val donor3LastDate = DonationCountdownEngine.daysAgoIsoDate(6)
    private val donor3NextDate = DonationCountdownEngine.computeNextEligibleDateString(donor3LastDate, 14L)

    private val donor4LastDate = DonationCountdownEngine.daysAgoIsoDate(15)
    private val donor4NextDate = DonationCountdownEngine.computeNextEligibleDateString(donor4LastDate, 28L)

    private val donor5LastDate = DonationCountdownEngine.daysAgoIsoDate(110)
    private val donor5NextDate = DonationCountdownEngine.computeNextEligibleDateString(donor5LastDate, 90L)

    private val donor6LastDate = DonationCountdownEngine.daysAgoIsoDate(10)
    private val donor6NextDate = DonationCountdownEngine.computeNextEligibleDateString(donor6LastDate, 90L)

    // In-memory interactive sandbox state for Guest Mode (never queries Firestore while unauthenticated)
    private val _guestUserProfile = MutableStateFlow<UiState<UserProfile?>>(
        UiState.Success(
            UserProfile(
                id = currentUserId,
                userId = currentUserId,
                fullName = defaultDisplayName,
                email = defaultEmail,
                phoneNumber = "01711000000",
                bloodGroup = "O+",
                area = "Mirpur-10, Dhaka",
                address = "Block C, Mirpur-10, Dhaka",
                lastDonationDate = guestDefaultLastDate,
                lastDonationTime = "14:30",
                nextEligibleDate = guestDefaultNextDate,
                donationType = "whole_blood",
                eligibilityIntervalDays = 90L,
                ruleVersion = "v1.0",
                wholeBloodCount = 2L,
                plateletsCount = 1L,
                plasmaCount = 0L,
                otherDonationCount = 0L,
                availabilityStatus = "available",
                isAvailable = true,
                notificationsEnabled = true,
                language = "bn",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        )
    )

    private val _guestMyDonor = MutableStateFlow<UiState<DonorProfile?>>(
        UiState.Success(
            DonorProfile(
                id = currentUserId,
                userId = currentUserId,
                fullName = defaultDisplayName,
                bloodGroup = "O+",
                area = "Mirpur-10, Dhaka",
                address = "Block C, Mirpur-10, Dhaka",
                phoneNumber = "01711000000",
                isAvailable = true,
                availabilityStatus = "available",
                lastDonationDate = guestDefaultLastDate,
                lastDonationTime = "14:30",
                nextEligibleDate = guestDefaultNextDate,
                donationType = "whole_blood",
                eligibilityIntervalDays = 90L,
                ruleVersion = "v1.0",
                totalDonations = 3L,
                wholeBloodCount = 2L,
                plateletsCount = 1L,
                plasmaCount = 0L,
                otherDonationCount = 0L,
                visibility = "public",
                showPhonePublicly = true,
                shareLocationArea = true,
                showDonationHistoryPublicly = true,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        )
    )

    private val _guestPublicDonors = MutableStateFlow<UiState<List<DonorProfile>>>(
        UiState.Success(
            listOf(
                DonorProfile(
                    id = "guest_donor_1",
                    userId = "guest_donor_1",
                    fullName = "Rafiqul Islam",
                    bloodGroup = "A+",
                    area = "Mirpur-10, Dhaka",
                    address = "Block C, Road 5, Mirpur-10, Dhaka (2.3 km)",
                    phoneNumber = "01711223344",
                    isAvailable = true,
                    availabilityStatus = "available",
                    lastDonationDate = donor1LastDate,
                    lastDonationTime = "11:00",
                    nextEligibleDate = donor1NextDate,
                    donationType = "whole_blood",
                    eligibilityIntervalDays = 90L,
                    ruleVersion = "v1.0",
                    totalDonations = 5L,
                    wholeBloodCount = 4L,
                    plateletsCount = 1L,
                    visibility = "public",
                    age = "29",
                    gender = "Male (পুরুষ)",
                    showPhonePublicly = true,
                    shareLocationArea = true,
                    showDonationHistoryPublicly = true,
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 180L * 24 * 3600 * 1000L))
                ),
                DonorProfile(
                    id = "guest_donor_2",
                    userId = "guest_donor_2",
                    fullName = "Tanvir Hasan",
                    bloodGroup = "O+",
                    area = "Mirpur-2, Dhaka",
                    address = "Near Sher-e-Bangla Stadium, Mirpur-2, Dhaka (4.1 km)",
                    phoneNumber = "01819223344",
                    isAvailable = true,
                    availabilityStatus = "available",
                    lastDonationDate = donor2LastDate,
                    lastDonationTime = "16:15",
                    nextEligibleDate = donor2NextDate,
                    donationType = "whole_blood",
                    eligibilityIntervalDays = 90L,
                    ruleVersion = "v1.0",
                    totalDonations = 4L,
                    wholeBloodCount = 3L,
                    plasmaCount = 1L,
                    visibility = "public",
                    age = "26",
                    gender = "Male (পুরুষ)",
                    showPhonePublicly = true,
                    shareLocationArea = true,
                    showDonationHistoryPublicly = true,
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 150L * 24 * 3600 * 1000L))
                ),
                DonorProfile(
                    id = "guest_donor_3",
                    userId = "guest_donor_3",
                    fullName = "Nusrat Jahan",
                    bloodGroup = "B+",
                    area = "Mohammadpur, Dhaka",
                    address = "Tajmahal Road, Mohammadpur, Dhaka (5.6 km)",
                    phoneNumber = "01911223344",
                    isAvailable = false,
                    availabilityStatus = "temporarily_unavailable",
                    lastDonationDate = donor3LastDate,
                    lastDonationTime = "10:30",
                    nextEligibleDate = donor3NextDate,
                    donationType = "platelets",
                    eligibilityIntervalDays = 14L,
                    ruleVersion = "v1.0",
                    totalDonations = 2L,
                    wholeBloodCount = 1L,
                    plateletsCount = 1L,
                    visibility = "public",
                    age = "24",
                    gender = "Female (নারী)",
                    showPhonePublicly = false,
                    shareLocationArea = true,
                    showDonationHistoryPublicly = true,
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 120L * 24 * 3600 * 1000L))
                ),
                DonorProfile(
                    id = "guest_donor_4",
                    userId = "guest_donor_4",
                    fullName = "Sabbir Ahmed",
                    bloodGroup = "AB+",
                    area = "Mirpur-11, Dhaka",
                    address = "Avenue 3, Mirpur-11, Dhaka (6.8 km)",
                    phoneNumber = "01611223344",
                    isAvailable = true,
                    availabilityStatus = "available",
                    lastDonationDate = donor4LastDate,
                    lastDonationTime = "15:00",
                    nextEligibleDate = donor4NextDate,
                    donationType = "plasma",
                    eligibilityIntervalDays = 28L,
                    ruleVersion = "v1.0",
                    totalDonations = 6L,
                    wholeBloodCount = 4L,
                    plasmaCount = 2L,
                    visibility = "public",
                    age = "32",
                    gender = "Male (পুরুষ)",
                    showPhonePublicly = true,
                    shareLocationArea = true,
                    showDonationHistoryPublicly = true,
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 240L * 24 * 3600 * 1000L))
                ),
                DonorProfile(
                    id = "guest_donor_5",
                    userId = "guest_donor_5",
                    fullName = "Mahmudul Hasan",
                    bloodGroup = "O-",
                    area = "Pallabi, Mirpur",
                    address = "Pallabi Phase-2, Mirpur-12, Dhaka",
                    phoneNumber = "01511223344",
                    isAvailable = true,
                    availabilityStatus = "available",
                    lastDonationDate = donor5LastDate,
                    lastDonationTime = "09:45",
                    nextEligibleDate = donor5NextDate,
                    donationType = "whole_blood",
                    eligibilityIntervalDays = 90L,
                    ruleVersion = "v1.0",
                    totalDonations = 7L,
                    wholeBloodCount = 5L,
                    plateletsCount = 2L,
                    visibility = "public",
                    age = "34",
                    gender = "Male (পুরুষ)",
                    showPhonePublicly = true,
                    shareLocationArea = true,
                    showDonationHistoryPublicly = true,
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 300L * 24 * 3600 * 1000L))
                ),
                DonorProfile(
                    id = "guest_donor_6",
                    userId = "guest_donor_6",
                    fullName = "Farhana Akter",
                    bloodGroup = "A-",
                    area = "Kazipara, Mirpur",
                    address = "West Kazipara, Mirpur, Dhaka",
                    phoneNumber = "01755667788",
                    isAvailable = false,
                    availabilityStatus = "not_accepting",
                    lastDonationDate = donor6LastDate,
                    lastDonationTime = "13:20",
                    nextEligibleDate = donor6NextDate,
                    donationType = "whole_blood",
                    eligibilityIntervalDays = 90L,
                    ruleVersion = "v1.0",
                    totalDonations = 3L,
                    wholeBloodCount = 3L,
                    visibility = "public",
                    age = "27",
                    gender = "Female (নারী)",
                    showPhonePublicly = true,
                    shareLocationArea = true,
                    showDonationHistoryPublicly = true,
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 95L * 24 * 3600 * 1000L))
                )
            )
        )
    )

    private val _guestBloodRequests = MutableStateFlow<UiState<List<BloodRequest>>>(
        UiState.Success(
            listOf(
                BloodRequest(
                    id = "guest_req_1",
                    userId = currentUserId,
                    requesterName = defaultDisplayName,
                    patientName = "Abdul Karim",
                    bloodGroup = "A+",
                    bagsNeeded = 2L,
                    hospitalName = "City Hospital",
                    location = "Dhaka",
                    contactNumber = "01711998877",
                    neededDateTime = "আজ রাত ৮:০০ (Today 8:00 PM)",
                    urgency = "Emergency",
                    notes = "Urgent surgery at City Hospital, Dhaka",
                    status = "open",
                    visibility = "community",
                    authorizedDonorId = "",
                    authorizedDonorName = "",
                    liveSessionActive = false,
                    liveSessionExpiresAt = 0L,
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 2 * 3600 * 1000L))
                ),
                BloodRequest(
                    id = "guest_req_2",
                    userId = "guest_donor_2",
                    requesterName = "Tanvir Hasan",
                    patientName = "Rokeya Begum",
                    bloodGroup = "O+",
                    bagsNeeded = 1L,
                    hospitalName = "National Heart Foundation",
                    location = "Mirpur-2, Dhaka",
                    contactNumber = "01819223344",
                    neededDateTime = "আজ সন্ধ্যা ৭:৩০ (Today 7:30 PM)",
                    urgency = "Emergency",
                    notes = "Cardiac unit ICU — live location shared with donor",
                    status = "open",
                    visibility = "community",
                    authorizedDonorId = currentUserId,
                    authorizedDonorName = defaultDisplayName,
                    liveSessionActive = true,
                    liveSessionExpiresAt = System.currentTimeMillis() + 45 * 60 * 1000L,
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 4 * 3600 * 1000L))
                ),
                BloodRequest(
                    id = "guest_req_3",
                    userId = "guest_donor_3",
                    requesterName = "Nusrat Jahan",
                    patientName = "Sumon Mia",
                    bloodGroup = "B-",
                    bagsNeeded = 2L,
                    hospitalName = "Delta Medical College Hospital",
                    location = "Mirpur-1, Dhaka",
                    contactNumber = "01911223344",
                    neededDateTime = "আগামীকাল সকাল ১০:০০",
                    urgency = "Urgent",
                    notes = "Oncology ward",
                    status = "open",
                    visibility = "community",
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 6 * 3600 * 1000L))
                )
            )
        )
    )

    private val _guestLiveSessions = MutableStateFlow<Map<String, LiveLocationSession>>(
        mapOf(
            "guest_req_2" to LiveLocationSession(
                id = "guest_req_2",
                requestId = "guest_req_2",
                requesterId = "guest_donor_2",
                requesterName = "Tanvir Hasan",
                authorizedDonorId = currentUserId,
                authorizedDonorName = defaultDisplayName,
                latitude = 23.8042,
                longitude = 90.3625,
                accuracyMeters = 11.0,
                isActive = true,
                expiresAtEpochMs = System.currentTimeMillis() + 45 * 60 * 1000L,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        )
    )

    private val _guestDonations = MutableStateFlow<UiState<List<DonationRecord>>>(
        UiState.Success(
            listOf(
                DonationRecord(
                    id = "guest_don_1",
                    userId = currentUserId,
                    donorName = defaultDisplayName,
                    hospitalName = "National Heart Foundation Hospital",
                    location = "Mirpur-2, Dhaka",
                    recipientName = "Rahim Uddin",
                    bloodGroup = "O+",
                    donationDate = guestDefaultLastDate,
                    donationTime = "14:30",
                    donationType = "whole_blood",
                    procedureConfirmation = "NHFH-WB-2026-884",
                    eligibilityIntervalDays = 90L,
                    ruleVersion = "v1.0",
                    nextEligibleDate = guestDefaultNextDate,
                    verificationStatus = "verified",
                    visibility = "public",
                    notes = "1 Bag Whole Blood • Mirpur-2",
                    createdAt = Timestamp.now()
                ),
                DonationRecord(
                    id = "guest_don_1b",
                    userId = currentUserId,
                    donorName = defaultDisplayName,
                    hospitalName = "Square Hospital Blood Bank",
                    location = "Dhaka",
                    recipientName = "Imran Hossain",
                    bloodGroup = "O+",
                    donationDate = DonationCountdownEngine.daysAgoIsoDate(95),
                    donationTime = "11:15",
                    donationType = "platelets",
                    procedureConfirmation = "Apheresis-PLT-402",
                    eligibilityIntervalDays = 14L,
                    ruleVersion = "v1.0",
                    nextEligibleDate = DonationCountdownEngine.daysAgoIsoDate(81),
                    verificationStatus = "verified",
                    visibility = "public",
                    notes = "Apheresis Platelets donation",
                    createdAt = Timestamp.now()
                ),
                DonationRecord(
                    id = "guest_don_2",
                    userId = currentUserId,
                    donorName = defaultDisplayName,
                    hospitalName = "Islami Bank Hospital",
                    location = "Mirpur-11, Dhaka",
                    recipientName = "Salma Khatun",
                    bloodGroup = "O+",
                    donationDate = DonationCountdownEngine.daysAgoIsoDate(195),
                    donationTime = "16:00",
                    donationType = "whole_blood",
                    procedureConfirmation = "IBH-WB-109",
                    eligibilityIntervalDays = 90L,
                    ruleVersion = "v1.0",
                    nextEligibleDate = DonationCountdownEngine.daysAgoIsoDate(105),
                    verificationStatus = "verified",
                    visibility = "public",
                    notes = "Emergency surgery support",
                    createdAt = Timestamp.now()
                ),
                DonationRecord(
                    id = "guest_don_3",
                    userId = "guest_donor_1",
                    donorName = "Rafiqul Islam",
                    hospitalName = "Delta Medical College Hospital",
                    location = "Mirpur-1, Dhaka",
                    recipientName = "Karim Mia",
                    bloodGroup = "A+",
                    donationDate = donor1LastDate,
                    donationTime = "11:00",
                    donationType = "whole_blood",
                    procedureConfirmation = "DMCH-992",
                    eligibilityIntervalDays = 90L,
                    ruleVersion = "v1.0",
                    nextEligibleDate = donor1NextDate,
                    verificationStatus = "verified",
                    visibility = "public",
                    notes = "Whole blood donation",
                    createdAt = Timestamp.now()
                ),
                DonationRecord(
                    id = "guest_don_4",
                    userId = "guest_donor_2",
                    donorName = "Tanvir Hasan",
                    hospitalName = "National Heart Foundation",
                    location = "Mirpur-2, Dhaka",
                    recipientName = "Abul Hossain",
                    bloodGroup = "O+",
                    donationDate = donor2LastDate,
                    donationTime = "16:15",
                    donationType = "whole_blood",
                    procedureConfirmation = "NHF-310",
                    eligibilityIntervalDays = 90L,
                    ruleVersion = "v1.0",
                    nextEligibleDate = donor2NextDate,
                    verificationStatus = "verified",
                    visibility = "public",
                    notes = "Cardiac surgery donation",
                    createdAt = Timestamp.now()
                ),
                DonationRecord(
                    id = "guest_don_5",
                    userId = "guest_donor_3",
                    donorName = "Nusrat Jahan",
                    hospitalName = "Dhaka Shishu Hospital",
                    location = "Agargaon, Dhaka",
                    recipientName = "Rafiul",
                    bloodGroup = "B+",
                    donationDate = donor3LastDate,
                    donationTime = "10:30",
                    donationType = "platelets",
                    procedureConfirmation = "DSH-PLT-77",
                    eligibilityIntervalDays = 14L,
                    ruleVersion = "v1.0",
                    nextEligibleDate = donor3NextDate,
                    verificationStatus = "verified",
                    visibility = "public",
                    notes = "Dengue patient platelets support",
                    createdAt = Timestamp.now()
                )
            )
        )
    )

    // Two-Tier Kotlin Flow Error Handling Architecture (Auth-Gated when isGuest == true)
    val userProfileState: StateFlow<UiState<UserProfile?>> = if (isGuest) {
        _guestUserProfile.asStateFlow()
    } else {
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { profile ->
                if (profile != null && profile.language in listOf("bn", "en") &&
                    !MbdPreferencesManager.hasExplicitLanguagePreference
                ) {
                    _language.value = profile.language
                }
                UiState.Success(profile)
            }
            .catch { error ->
                Log.w(TAG, "Error observing user profile", error)
                emit(UiState.Error(error.localizedMessage ?: "Failed to load user profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    val myDonorState: StateFlow<UiState<DonorProfile?>> = if (isGuest) {
        _guestMyDonor.asStateFlow()
    } else {
        repository.observeMyDonorProfile(currentUserId)
            .map<DonorProfile?, UiState<DonorProfile?>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing donor profile", error)
                emit(UiState.Error(error.localizedMessage ?: "Failed to load donor profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    val publicDonorsState: StateFlow<UiState<List<DonorProfile>>> = if (isGuest) {
        _guestPublicDonors.asStateFlow()
    } else {
        repository.observePublicDonors()
            .map<List<DonorProfile>, UiState<List<DonorProfile>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing public donors", error)
                emit(UiState.Error(error.localizedMessage ?: "Failed to load donors"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    val bloodRequestsState: StateFlow<UiState<List<BloodRequest>>> = if (isGuest) {
        _guestBloodRequests.asStateFlow()
    } else {
        repository.observeCommunityBloodRequests()
            .map<List<BloodRequest>, UiState<List<BloodRequest>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing blood requests", error)
                emit(UiState.Error(error.localizedMessage ?: "Failed to load blood requests"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeLiveSessionState: StateFlow<UiState<LiveLocationSession?>> =
        _selectedRequestId.flatMapLatest { reqId ->
            if (reqId.isNullOrBlank()) {
                flowOf(UiState.Success(null))
            } else if (isGuest) {
                _guestLiveSessions.map { map -> UiState.Success(map[reqId]) }
            } else {
                repository.observeLiveLocationSession(reqId)
                    .map<LiveLocationSession?, UiState<LiveLocationSession?>> { UiState.Success(it) }
                    .catch { error ->
                        Log.w(TAG, "Live location session unavailable or restricted for $reqId", error)
                        emit(UiState.Success(null))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(null)
        )

    val myDonationsState: StateFlow<UiState<List<DonationRecord>>> = if (isGuest) {
        _guestDonations.map { state ->
            when (state) {
                is UiState.Success -> UiState.Success(state.data.filter { it.userId == currentUserId })
                is UiState.Error -> state
                is UiState.Loading -> state
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )
    } else {
        repository.observeDonations(currentUserId)
            .map<List<DonationRecord>, UiState<List<DonationRecord>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing donations", error)
                emit(UiState.Error(error.localizedMessage ?: "Failed to load donation history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDonorProfileState: StateFlow<UiState<DonorProfile?>> =
        _selectedDonorId.flatMapLatest { donorId ->
            if (donorId.isNullOrBlank()) {
                flowOf(UiState.Success(null))
            } else if (isGuest) {
                _guestPublicDonors.map { state ->
                    val list = (state as? UiState.Success)?.data.orEmpty()
                    val match = list.find { it.id == donorId || it.userId == donorId }
                        ?: ((_guestMyDonor.value as? UiState.Success)?.data?.takeIf {
                            it.id == donorId || it.userId == donorId
                        })
                    UiState.Success(match)
                }
            } else {
                repository.observeDonorById(donorId)
                    .map<DonorProfile?, UiState<DonorProfile?>> { UiState.Success(it) }
                    .catch { error ->
                        Log.w(TAG, "Error observing selected donor profile $donorId", error)
                        val fallback = ((publicDonorsState.value as? UiState.Success)?.data.orEmpty())
                            .find { it.id == donorId || it.userId == donorId }
                        if (fallback != null) {
                            emit(UiState.Success(fallback))
                        } else {
                            emit(UiState.Error(error.localizedMessage ?: "Failed to load donor profile"))
                        }
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDonorDonationsState: StateFlow<UiState<List<DonationRecord>>> =
        _selectedDonorId.flatMapLatest { donorId ->
            if (donorId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else if (isGuest) {
                _guestDonations.map { state ->
                    val all = (state as? UiState.Success)?.data.orEmpty()
                    UiState.Success(all.filter { it.userId == donorId })
                }
            } else if (donorId == currentUserId) {
                repository.observeDonations(currentUserId)
                    .map<List<DonationRecord>, UiState<List<DonationRecord>>> { UiState.Success(it) }
                    .catch { error ->
                        Log.w(TAG, "Error observing own donor donations", error)
                        emit(UiState.Success(emptyList()))
                    }
            } else {
                repository.observePublicDonorDonations(donorId)
                    .map<List<DonationRecord>, UiState<List<DonationRecord>>> { UiState.Success(it) }
                    .catch { error ->
                        Log.w(TAG, "Error observing public donor donations for $donorId", error)
                        emit(UiState.Success(emptyList()))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    // Admin Guest Backing Data
    private val _guestReports = MutableStateFlow<UiState<List<AdminReport>>>(
        UiState.Success(
            listOf(
                AdminReport(
                    id = "guest_rep_1",
                    reporterUid = "guest_donor_1",
                    reporterName = "Rafiqul Islam",
                    reportedId = "guest_req_2",
                    reportedType = "request",
                    category = "Fake Blood Request",
                    description = "Phone number is switched off and hospital stated patient was discharged last week.",
                    status = "pending",
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 2 * 3600 * 1000L))
                ),
                AdminReport(
                    id = "guest_rep_2",
                    reporterUid = "guest_donor_3",
                    reporterName = "Nusrat Jahan",
                    reportedId = "guest_donor_4",
                    reportedType = "donor",
                    category = "Incorrect Donor Info",
                    description = "Donor profile mentions available but donor is currently out of Dhaka.",
                    status = "reviewing",
                    adminNotes = "Contacted donor on WhatsApp, verifying return date.",
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 14 * 3600 * 1000L))
                )
            )
        )
    )

    private val _guestBans = MutableStateFlow<UiState<List<AdminUserBan>>>(
        UiState.Success(
            listOf(
                AdminUserBan(
                    userId = "guest_spammer_99",
                    bannedByUid = currentUserId,
                    bannedByEmail = defaultEmail,
                    reason = "Repeated fraudulent emergency requests",
                    bannedAt = Timestamp(Date(System.currentTimeMillis() - 48 * 3600 * 1000L))
                )
            )
        )
    )

    private val _guestAdminLogs = MutableStateFlow<UiState<List<AdminLog>>>(
        UiState.Success(
            listOf(
                AdminLog(
                    id = "log_init_1",
                    adminUid = currentUserId,
                    adminEmail = defaultEmail,
                    actionType = "USER_BANNED",
                    targetId = "guest_spammer_99",
                    targetType = "user",
                    reason = "Repeated fraudulent emergency requests",
                    status = "success",
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 48 * 3600 * 1000L))
                ),
                AdminLog(
                    id = "log_init_2",
                    adminUid = currentUserId,
                    adminEmail = defaultEmail,
                    actionType = "CONFIG_UPDATED",
                    targetId = "main_config",
                    targetType = "config",
                    reason = "Emergency announcement banner verified",
                    status = "success",
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 24 * 3600 * 1000L))
                )
            )
        )
    )

    private val _guestAnnouncements = MutableStateFlow<UiState<List<AdminAnnouncement>>>(
        UiState.Success(
            listOf(
                AdminAnnouncement(
                    id = "ann_1",
                    title = "জরুরি ও-নেগেটিভ রক্ত সহায়তা আহ্বান",
                    message = "মিরপুর এলাকার ও-নেগেটিভ রক্তদাতাদের জরুরিভাবে হার্ট ফাউন্ডেশন হাসপাতালে যোগাযোগ করতে অনুরোধ করা হচ্ছে।",
                    type = "emergency",
                    isEmergency = true,
                    isActive = true,
                    createdByEmail = defaultEmail,
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 6 * 3600 * 1000L))
                ),
                AdminAnnouncement(
                    id = "ann_2",
                    title = "MBD সিস্টেম আপডেট নোটিশ",
                    message = "রক্তদানের ইতিহাস এবং নতুন কাউন্টডাউন সুবিধা এখন সক্রিয়। রক্তদানের পর আপনার রেকর্ড যোগ করুন।",
                    type = "service_update",
                    isEmergency = false,
                    isActive = true,
                    createdByEmail = defaultEmail,
                    createdAt = Timestamp(Date(System.currentTimeMillis() - 72 * 3600 * 1000L))
                )
            )
        )
    )

    private val _guestAppConfig = MutableStateFlow(
        AppConfig(
            id = "main_config",
            emergencyBannerText = "জরুরি রক্তের প্রয়োজন হলে সরাসরি ০১৭০০০০০০০০ নম্বরে হেল্পলাইনে যোগাযোগ করুন।",
            emergencyBannerActive = true,
            supportPhone = "01700000000",
            supportEmail = "support@mirpurblooddonation.com",
            privacyPolicyUrl = "https://mirpurblooddonation.org/privacy",
            termsUrl = "https://mirpurblooddonation.org/terms",
            latestAppVersion = "1.0.0",
            minRequiredVersion = "1.0.0",
            updateMessage = "আপনার অ্যাপটি সর্বশেষ সংস্করণে আছে"
        )
    )

    val adminUsersState: StateFlow<UiState<List<UserProfile>>> = if (isGuest) {
        _guestPublicDonors.map { state ->
            when (state) {
                is UiState.Success -> UiState.Success(
                    state.data.map { donor ->
                        UserProfile(
                            id = donor.id,
                            userId = donor.userId,
                            fullName = donor.fullName,
                            email = "${donor.userId}@mbd.org",
                            phoneNumber = donor.phoneNumber,
                            bloodGroup = donor.bloodGroup,
                            area = donor.area,
                            address = donor.address,
                            lastDonationDate = donor.lastDonationDate,
                            isAvailable = donor.isAvailable,
                            createdAt = donor.createdAt
                        )
                    }
                )
                is UiState.Error -> state
                is UiState.Loading -> state
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
    } else {
        repository.observeAllUsers()
            .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
            .catch { emit(UiState.Error(it.localizedMessage ?: "Failed to load users")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
    }

    val adminBansState: StateFlow<UiState<List<AdminUserBan>>> = if (isGuest) {
        _guestBans.asStateFlow()
    } else {
        repository.observeAllBans()
            .map<List<AdminUserBan>, UiState<List<AdminUserBan>>> { UiState.Success(it) }
            .catch { emit(UiState.Success(emptyList())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(emptyList()))
    }

    val isCurrentUserBanned: StateFlow<Boolean> = adminBansState.map { state ->
        when (state) {
            is UiState.Success -> state.data.any { it.userId == currentUserId }
            else -> false
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val adminReportsState: StateFlow<UiState<List<AdminReport>>> = if (isGuest) {
        _guestReports.asStateFlow()
    } else {
        repository.observeAllReports()
            .map<List<AdminReport>, UiState<List<AdminReport>>> { UiState.Success(it) }
            .catch { emit(UiState.Error(it.localizedMessage ?: "Failed to load reports")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
    }

    val adminLogsState: StateFlow<UiState<List<AdminLog>>> = if (isGuest) {
        _guestAdminLogs.asStateFlow()
    } else {
        repository.observeAdminLogs()
            .map<List<AdminLog>, UiState<List<AdminLog>>> { UiState.Success(it) }
            .catch { emit(UiState.Success(emptyList())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
    }

    val adminAnnouncementsState: StateFlow<UiState<List<AdminAnnouncement>>> = if (isGuest) {
        _guestAnnouncements.asStateFlow()
    } else {
        repository.observeAnnouncements()
            .map<List<AdminAnnouncement>, UiState<List<AdminAnnouncement>>> { UiState.Success(it) }
            .catch { emit(UiState.Success(emptyList())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
    }

    val appConfigState: StateFlow<AppConfig> = if (isGuest) {
        _guestAppConfig.asStateFlow()
    } else {
        repository.observeAppConfig()
            .catch { emit(AppConfig()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppConfig())
    }

    val allDonationHistoryState: StateFlow<UiState<List<DonationRecord>>> = if (isGuest) {
        _guestDonations.asStateFlow()
    } else {
        repository.observeAllDonationHistory()
            .map<List<DonationRecord>, UiState<List<DonationRecord>>> { UiState.Success(it) }
            .catch { emit(UiState.Success(emptyList())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)
    }

    fun navigateTo(screen: MbdScreen) {
        if (screen == MbdScreen.ADMIN_PANEL && !isAdminUser) {
            _statusMessage.value = if (_language.value == "bn") {
                "অ্যাক্সেস প্রত্যাখ্যান করা হয়েছে: শুধুমাত্র অনুমোদিত এডমিন।"
            } else {
                "Access Denied: Owner-admin privileges required."
            }
            return
        }
        val current = _currentScreen.value
        if (current != screen) {
            _screenHistory.addLast(current)
            _currentScreen.value = screen
        }
    }

    fun setAdminSubSection(section: AdminSubSection) {
        _adminSubSection.value = section
    }

    fun openDonorProfileDetails(donor: DonorProfile) {
        val uniqueId = donor.id.ifBlank { donor.userId }
        _selectedDonorId.value = uniqueId
        navigateTo(MbdScreen.DONOR_PROFILE_DETAILS)
    }

    fun refreshSelectedDonor() {
        val donorId = _selectedDonorId.value ?: return
        if (isGuest) {
            _statusMessage.value = if (_language.value == "bn") {
                "রক্তদাতার প্রোফাইল তথ্য আপডেট আছে"
            } else {
                "Donor profile is up to date"
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.getDonorById(donorId)
                _statusMessage.value = if (_language.value == "bn") {
                    "রক্তদাতার সর্বশেষ প্রোফাইল তথ্য রিফ্রেশ করা হয়েছে"
                } else {
                    "Donor profile refreshed from database"
                }
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to refresh donor profile"
            }
        }
    }

    fun openRequestBloodForDonor(donor: DonorProfile) {
        val isSelf = donor.userId == currentUserId || donor.id == currentUserId
        val isBn = _language.value == "bn"
        if (isSelf) {
            _statusMessage.value = if (isBn) "নিজের কাছে রক্তের আবেদন পাঠানো সম্ভব নয়" else "You cannot send a blood request to yourself"
            return
        }
        if (!donor.isAvailable) {
            _statusMessage.value = if (isBn) "এই রক্তদাতা বর্তমানে রক্তদানের জন্য প্রস্তুত নন" else "This donor is currently unavailable for blood donation"
            return
        }
        _preselectedRequestDonor.value = donor
        navigateTo(MbdScreen.REQUEST_BLOOD)
    }

    fun clearPreselectedRequestDonor() {
        _preselectedRequestDonor.value = null
    }

    fun openRequestDetails(request: BloodRequest) {
        _selectedRequestId.value = request.id
        navigateTo(MbdScreen.REQUEST_DETAILS)
    }

    fun openDonorLiveMapForRequest(request: BloodRequest) {
        _selectedRequestId.value = request.id
        navigateTo(MbdScreen.DONOR_LIVE_MAP_VIEW)
    }

    fun navigateBack(): Boolean {
        return if (_screenHistory.isNotEmpty()) {
            _currentScreen.value = _screenHistory.removeLast()
            true
        } else if (_currentScreen.value != MbdScreen.HOME) {
            _currentScreen.value = MbdScreen.HOME
            true
        } else {
            false
        }
    }

    fun toggleLanguage() {
        val next = if (_language.value == "bn") "en" else "bn"
        _language.value = next
        MbdPreferencesManager.setLanguage(null, next, isExplicitUserChoice = true)
    }

    fun setLanguage(lang: String) {
        if (lang in listOf("bn", "en")) {
            _language.value = lang
            MbdPreferencesManager.setLanguage(null, lang, isExplicitUserChoice = true)
        }
    }

    fun setThemeMode(mode: MbdThemeMode) {
        MbdPreferencesManager.setThemeMode(null, mode)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedBloodGroup(bloodGroup: String) {
        _selectedBloodGroup.value = bloodGroup
    }

    fun setAvailableOnly(onlyAvailable: Boolean) {
        _availableOnly.value = onlyAvailable
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun saveProfileAndDonorStatus(
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
        donationType: String = "whole_blood",
        eligibilityIntervalDays: Long = 90L,
        showPhonePublicly: Boolean = true,
        shareLocationArea: Boolean = true,
        showDonationHistoryPublicly: Boolean = true,
        age: String = "",
        gender: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val trimmedName = fullName.trim()
        val trimmedPhone = phoneNumber.trim()
        val trimmedArea = area.trim().ifEmpty { "Mirpur-10, Dhaka" }
        val trimmedAddress = address.trim().ifEmpty { trimmedArea }
        val cleanLastDate = lastDonationDate.trim()
        val rules = eligibilityRulesState.value
        val activeRule = rules[donationType] ?: DEFAULT_ELIGIBILITY_RULES[donationType]
        val safeInterval = if (donationType == "other" && (activeRule == null || !activeRule.isVerifiedRule)) {
            0L
        } else {
            eligibilityIntervalDays.coerceIn(14L, 365L)
        }
        val computedNextDate = if (safeInterval <= 0L && cleanLastDate.isNotBlank()) {
            "UNDECIDED"
        } else {
            DonationCountdownEngine.computeNextEligibleDateString(cleanLastDate, safeInterval)
        }
        val resolvedStatus = when (availabilityStatus) {
            "available", "temporarily_unavailable", "not_accepting" -> availabilityStatus
            else -> "available"
        }
        val isAvailableBool = resolvedStatus == "available"
        val currentUser = (userProfileState.value as? UiState.Success)?.data
        val currentDonor = (myDonorState.value as? UiState.Success)?.data

        if (trimmedName.isEmpty()) {
            _statusMessage.value = if (_language.value == "bn") "অনুগ্রহ করে আপনার নাম লিখুন" else "Please enter your full name"
            return
        }
        if (trimmedPhone.length < 6) {
            _statusMessage.value = if (_language.value == "bn") "সঠিক মোবাইল নম্বর লিখুন" else "Please enter a valid phone number"
            return
        }

        if (isGuest) {
            val updatedUser = UserProfile(
                id = currentUserId,
                userId = currentUserId,
                fullName = trimmedName,
                email = defaultEmail,
                phoneNumber = trimmedPhone,
                bloodGroup = bloodGroup,
                area = trimmedArea,
                address = trimmedAddress,
                lastDonationDate = cleanLastDate,
                lastDonationTime = currentUser?.lastDonationTime ?: "",
                nextEligibleDate = computedNextDate,
                donationType = donationType,
                customDonationTypeName = currentUser?.customDonationTypeName ?: "",
                eligibilityIntervalDays = safeInterval,
                ruleVersion = activeRule?.ruleVersion ?: "v1.0",
                wholeBloodCount = currentUser?.wholeBloodCount ?: 0L,
                plateletsCount = currentUser?.plateletsCount ?: 0L,
                plasmaCount = currentUser?.plasmaCount ?: 0L,
                otherDonationCount = currentUser?.otherDonationCount ?: 0L,
                availabilityStatus = resolvedStatus,
                isAvailable = isAvailableBool,
                notificationsEnabled = notificationsEnabled,
                language = _language.value,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            val updatedDonor = DonorProfile(
                id = currentUserId,
                userId = currentUserId,
                fullName = trimmedName,
                bloodGroup = bloodGroup,
                area = trimmedArea,
                address = trimmedAddress,
                phoneNumber = trimmedPhone,
                isAvailable = isAvailableBool,
                availabilityStatus = resolvedStatus,
                lastDonationDate = cleanLastDate,
                lastDonationTime = currentDonor?.lastDonationTime ?: "",
                nextEligibleDate = computedNextDate,
                donationType = donationType,
                customDonationTypeName = currentDonor?.customDonationTypeName ?: "",
                eligibilityIntervalDays = safeInterval,
                ruleVersion = activeRule?.ruleVersion ?: "v1.0",
                totalDonations = totalDonations.coerceAtLeast(0L),
                wholeBloodCount = currentDonor?.wholeBloodCount ?: 0L,
                plateletsCount = currentDonor?.plateletsCount ?: 0L,
                plasmaCount = currentDonor?.plasmaCount ?: 0L,
                otherDonationCount = currentDonor?.otherDonationCount ?: 0L,
                visibility = if (isPublicDonor) "public" else "private",
                age = age.trim(),
                gender = gender.trim(),
                showPhonePublicly = showPhonePublicly,
                shareLocationArea = shareLocationArea,
                showDonationHistoryPublicly = showDonationHistoryPublicly,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            _guestUserProfile.value = UiState.Success(updatedUser)
            _guestMyDonor.value = UiState.Success(updatedDonor)
            val existingList = (_guestPublicDonors.value as? UiState.Success)?.data.orEmpty()
                .filterNot { it.id == currentUserId || it.userId == currentUserId }
            _guestPublicDonors.value = UiState.Success(
                if (isPublicDonor) listOf(updatedDonor) + existingList else existingList
            )
            _statusMessage.value = if (_language.value == "bn") {
                "গেস্ট প্রোফাইল, প্রাপ্যতা ও কাউন্টডাউন তথ্য আপডেট হয়েছে!"
            } else {
                "Guest profile, availability & countdown updated!"
            }
            onSuccess()
            return
        }

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val userProfile = UserProfile(
                    userId = currentUserId,
                    fullName = trimmedName,
                    email = defaultEmail,
                    phoneNumber = trimmedPhone,
                    bloodGroup = bloodGroup,
                    area = trimmedArea,
                    address = trimmedAddress,
                    lastDonationDate = cleanLastDate,
                    lastDonationTime = currentUser?.lastDonationTime ?: "",
                    nextEligibleDate = computedNextDate,
                    donationType = donationType,
                    customDonationTypeName = currentUser?.customDonationTypeName ?: "",
                    eligibilityIntervalDays = safeInterval,
                    ruleVersion = activeRule?.ruleVersion ?: "v1.0",
                    wholeBloodCount = currentUser?.wholeBloodCount ?: 0L,
                    plateletsCount = currentUser?.plateletsCount ?: 0L,
                    plasmaCount = currentUser?.plasmaCount ?: 0L,
                    otherDonationCount = currentUser?.otherDonationCount ?: 0L,
                    availabilityStatus = resolvedStatus,
                    isAvailable = isAvailableBool,
                    notificationsEnabled = notificationsEnabled,
                    language = _language.value
                )
                repository.saveUserProfile(userProfile)

                val donorProfile = DonorProfile(
                    userId = currentUserId,
                    fullName = trimmedName,
                    bloodGroup = bloodGroup,
                    area = trimmedArea,
                    address = trimmedAddress,
                    phoneNumber = trimmedPhone,
                    isAvailable = isAvailableBool,
                    availabilityStatus = resolvedStatus,
                    lastDonationDate = cleanLastDate,
                    lastDonationTime = currentDonor?.lastDonationTime ?: "",
                    nextEligibleDate = computedNextDate,
                    donationType = donationType,
                    customDonationTypeName = currentDonor?.customDonationTypeName ?: "",
                    eligibilityIntervalDays = safeInterval,
                    ruleVersion = activeRule?.ruleVersion ?: "v1.0",
                    totalDonations = totalDonations.coerceAtLeast(0L),
                    wholeBloodCount = currentDonor?.wholeBloodCount ?: 0L,
                    plateletsCount = currentDonor?.plateletsCount ?: 0L,
                    plasmaCount = currentDonor?.plasmaCount ?: 0L,
                    otherDonationCount = currentDonor?.otherDonationCount ?: 0L,
                    visibility = if (isPublicDonor) "public" else "private",
                    age = age.trim(),
                    gender = gender.trim(),
                    showPhonePublicly = showPhonePublicly,
                    shareLocationArea = shareLocationArea,
                    showDonationHistoryPublicly = showDonationHistoryPublicly
                )
                repository.saveDonorProfile(donorProfile)

                _statusMessage.value = if (_language.value == "bn") {
                    "প্রোফাইল ও রক্তদাতা তথ্য সফলভাবে সংরক্ষিত হয়েছে!"
                } else {
                    "Profile and donor status saved successfully!"
                }
                onSuccess()
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to save profile"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Records a new blood donation for the authenticated donor with Donation-Type-Specific Smart Countdown:
     * 1. Identifies the selected donation type (`whole_blood`, `platelets`, `plasma`, `other`).
     * 2. Retrieves the applicable medical eligibility rule from `eligibilityRulesState`.
     * 3. Never guesses an interval for `"other"` unless an admin has configured a verified rule (`UNDECIDED`).
     * 4. Evaluates multi-record cross-donation eligibility across all verified records and updates
     *    `lastDonationDate`, `lastDonationTime`, `nextEligibleDate`, `ruleVersion`, and per-type counts.
     */
    fun recordBloodDonation(
        donationDate: String,
        donationTime: String = "12:00",
        donationType: String = "whole_blood",
        customDonationTypeName: String = "",
        procedureConfirmation: String = "",
        eligibilityIntervalDays: Long = 90L,
        ruleVersion: String = "v1.0",
        hospitalName: String = "",
        location: String = "Mirpur, Dhaka",
        recipientName: String = "",
        bloodGroup: String = "O+",
        notes: String = "",
        verificationStatus: String = "verified",
        onSuccess: () -> Unit = {}
    ) {
        val cleanDate = donationDate.trim()
        val cleanTime = donationTime.trim().ifBlank { "12:00" }
        if (isCurrentUserBanned.value) {
            _statusMessage.value = if (_language.value == "bn") {
                "আপনার অ্যাকাউন্ট স্থগিত থাকায় রক্তদানের তথ্য যোগ করা সম্ভব নয়।"
            } else {
                "Your account is suspended. You cannot record new donations."
            }
            return
        }
        if (!DonationCountdownEngine.isValidIsoDate(cleanDate)) {
            _statusMessage.value = if (_language.value == "bn") {
                "অনুগ্রহ করে সঠিক ফরম্যাটে রক্তদানের তারিখ দিন (YYYY-MM-DD)"
            } else {
                "Please enter a valid donation date (YYYY-MM-DD)"
            }
            return
        }

        val rules = eligibilityRulesState.value
        val activeRule = rules[donationType]
            ?: DEFAULT_ELIGIBILITY_RULES[donationType]
            ?: DEFAULT_ELIGIBILITY_RULES["whole_blood"]!!

        val isRuleUndecided = donationType == "other" && !activeRule.isVerifiedRule && eligibilityIntervalDays <= 0L
        val safeInterval = if (isRuleUndecided) {
            0L
        } else if (eligibilityIntervalDays > 0L) {
            eligibilityIntervalDays.coerceIn(14L, 365L)
        } else {
            activeRule.intervalDays.coerceIn(14L, 365L)
        }
        val computedNextEligible = if (isRuleUndecided) {
            "UNDECIDED"
        } else {
            DonationCountdownEngine.computeNextEligibleDateString(cleanDate, safeInterval)
        }
        val resolvedRuleVersion = ruleVersion.ifBlank { activeRule.ruleVersion.ifBlank { "v1.0" } }

        val currentUser = (userProfileState.value as? UiState.Success)?.data
        val currentDonor = (myDonorState.value as? UiState.Success)?.data
        val resolvedDonorName = currentDonor?.fullName?.ifBlank { currentUser?.fullName }
            ?.ifBlank { defaultDisplayName } ?: defaultDisplayName
        val resolvedBloodGroup = bloodGroup.ifBlank {
            currentDonor?.bloodGroup ?: currentUser?.bloodGroup ?: "O+"
        }
        val showPublicly = currentDonor?.showDonationHistoryPublicly ?: true
        val resolvedVerification = if (verificationStatus in listOf("verified", "pending")) {
            verificationStatus
        } else {
            "verified"
        }

        if (isGuest) {
            val newRec = DonationRecord(
                id = "guest_don_${UUID.randomUUID().toString().take(8)}",
                userId = currentUserId,
                donorName = resolvedDonorName,
                hospitalName = hospitalName.trim(),
                location = location.trim().ifEmpty { "Mirpur, Dhaka" },
                recipientName = recipientName.trim(),
                bloodGroup = resolvedBloodGroup,
                donationDate = cleanDate,
                donationTime = cleanTime,
                donationType = donationType,
                customDonationTypeName = customDonationTypeName.trim(),
                procedureConfirmation = procedureConfirmation.trim(),
                eligibilityIntervalDays = safeInterval,
                ruleVersion = resolvedRuleVersion,
                nextEligibleDate = computedNextEligible,
                verificationStatus = resolvedVerification,
                visibility = if (showPublicly) "public" else "private",
                notes = notes.trim(),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            val currentList = (_guestDonations.value as? UiState.Success)?.data.orEmpty()
            val updatedList = listOf(newRec) + currentList
            _guestDonations.value = UiState.Success(updatedList)

            if (resolvedVerification == "verified") {
                val myRecords = updatedList.filter { it.userId == currentUserId }
                val eval = DonationCountdownEngine.evaluateMultiRecordEligibility(myRecords, rules)

                val baseUser = currentUser ?: UserProfile(
                    id = currentUserId,
                    userId = currentUserId,
                    fullName = resolvedDonorName,
                    email = defaultEmail,
                    phoneNumber = "01711000000",
                    bloodGroup = resolvedBloodGroup
                )
                val updatedUser = baseUser.copy(
                    lastDonationDate = eval.lastDonationDate,
                    lastDonationTime = eval.lastDonationTime,
                    nextEligibleDate = eval.nextEligibleDate,
                    donationType = eval.lastDonationType,
                    customDonationTypeName = eval.customDonationTypeName,
                    eligibilityIntervalDays = eval.governingIntervalDays,
                    ruleVersion = eval.governingRuleVersion,
                    wholeBloodCount = eval.countsByType.wholeBloodCount,
                    plateletsCount = eval.countsByType.plateletsCount,
                    plasmaCount = eval.countsByType.plasmaCount,
                    otherDonationCount = eval.countsByType.otherDonationCount,
                    isAvailable = eval.nextEligibleDate.isBlank() || eval.nextEligibleDate == "UNDECIDED" || DonationCountdownEngine.computeCountdown(eval.lastDonationDate, eval.nextEligibleDate, eval.governingIntervalDays, eval.lastDonationType, eval.lastDonationTime).isZeroReached,
                    availabilityStatus = if (eval.nextEligibleDate != "UNDECIDED" && !DonationCountdownEngine.computeCountdown(eval.lastDonationDate, eval.nextEligibleDate, eval.governingIntervalDays, eval.lastDonationType, eval.lastDonationTime).isZeroReached) "temporarily_unavailable" else "available",
                    updatedAt = Timestamp.now()
                )
                _guestUserProfile.value = UiState.Success(updatedUser)

                val baseDonor = currentDonor ?: DonorProfile(
                    id = currentUserId,
                    userId = currentUserId,
                    fullName = resolvedDonorName,
                    bloodGroup = resolvedBloodGroup,
                    area = baseUser.area,
                    address = baseUser.address,
                    phoneNumber = baseUser.phoneNumber
                )
                val updatedDonor = baseDonor.copy(
                    lastDonationDate = eval.lastDonationDate,
                    lastDonationTime = eval.lastDonationTime,
                    nextEligibleDate = eval.nextEligibleDate,
                    donationType = eval.lastDonationType,
                    customDonationTypeName = eval.customDonationTypeName,
                    eligibilityIntervalDays = eval.governingIntervalDays,
                    ruleVersion = eval.governingRuleVersion,
                    totalDonations = eval.countsByType.totalConfirmed.coerceAtLeast(1L),
                    wholeBloodCount = eval.countsByType.wholeBloodCount,
                    plateletsCount = eval.countsByType.plateletsCount,
                    plasmaCount = eval.countsByType.plasmaCount,
                    otherDonationCount = eval.countsByType.otherDonationCount,
                    isAvailable = updatedUser.isAvailable,
                    availabilityStatus = updatedUser.availabilityStatus,
                    updatedAt = Timestamp.now()
                )
                _guestMyDonor.value = UiState.Success(updatedDonor)
                val existingPublic = (_guestPublicDonors.value as? UiState.Success)?.data.orEmpty()
                    .filterNot { it.id == currentUserId || it.userId == currentUserId }
                if (updatedDonor.visibility == "public") {
                    _guestPublicDonors.value = UiState.Success(listOf(updatedDonor) + existingPublic)
                }
                _statusMessage.value = if (eval.isRuleUndecided) {
                    if (_language.value == "bn") {
                        "রক্তদান সংরক্ষিত হয়েছে। পরবর্তী তারিখ নির্ধারণ করা যায়নি (কাউন্টডাউন বন্ধ রয়েছে)।"
                    } else {
                        "Donation recorded. Next eligible date could not be determined (Countdown stopped)."
                    }
                } else {
                    if (_language.value == "bn") {
                        "রক্তদান রেকর্ড হয়েছে এবং প্রযোজ্য নিয়ম অনুযায়ী লাইভ কাউন্টডাউন শুরু হয়েছে!"
                    } else {
                        "Blood donation recorded & smart live countdown started!"
                    }
                }
            } else {
                _statusMessage.value = if (_language.value == "bn") {
                    "রক্তদানের রেকর্ড যাচাইকরণের জন্য জমা হয়েছে (যাচাইকরণ অপেক্ষমাণ)"
                } else {
                    "Donation submitted for verification (Pending Verification)"
                }
            }
            onSuccess()
            return
        }

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val record = DonationRecord(
                    userId = currentUserId,
                    donorName = resolvedDonorName,
                    hospitalName = hospitalName.trim(),
                    location = location.trim().ifEmpty { "Mirpur, Dhaka" },
                    recipientName = recipientName.trim(),
                    bloodGroup = resolvedBloodGroup,
                    donationDate = cleanDate,
                    donationTime = cleanTime,
                    donationType = donationType,
                    customDonationTypeName = customDonationTypeName.trim(),
                    procedureConfirmation = procedureConfirmation.trim(),
                    eligibilityIntervalDays = safeInterval,
                    ruleVersion = resolvedRuleVersion,
                    nextEligibleDate = computedNextEligible,
                    verificationStatus = resolvedVerification,
                    visibility = if (showPublicly) "public" else "private",
                    notes = notes.trim()
                )
                repository.addDonationRecord(record)

                if (resolvedVerification == "verified") {
                    val existingMyRecords = (myDonationsState.value as? UiState.Success)?.data.orEmpty()
                    val combinedRecords = listOf(record) + existingMyRecords
                    val eval = DonationCountdownEngine.evaluateMultiRecordEligibility(combinedRecords, rules)

                    val isDonorAvailable = if (eval.nextEligibleDate.isNotBlank() && eval.nextEligibleDate != "UNDECIDED") {
                        val cd = DonationCountdownEngine.computeCountdown(
                            lastDonationDate = eval.lastDonationDate,
                            explicitNextEligibleDate = eval.nextEligibleDate,
                            intervalDays = eval.governingIntervalDays,
                            donationType = eval.lastDonationType,
                            lastDonationTime = eval.lastDonationTime,
                            customDonationTypeName = eval.customDonationTypeName,
                            ruleVersion = eval.governingRuleVersion
                        )
                        cd.isZeroReached
                    } else if (eval.nextEligibleDate == "UNDECIDED") {
                        false
                    } else {
                        true
                    }

                    if (currentUser != null) {
                        repository.saveUserProfile(
                            currentUser.copy(
                                lastDonationDate = eval.lastDonationDate,
                                lastDonationTime = eval.lastDonationTime,
                                nextEligibleDate = eval.nextEligibleDate,
                                donationType = eval.lastDonationType,
                                customDonationTypeName = eval.customDonationTypeName,
                                eligibilityIntervalDays = eval.governingIntervalDays,
                                ruleVersion = eval.governingRuleVersion,
                                wholeBloodCount = eval.countsByType.wholeBloodCount,
                                plateletsCount = eval.countsByType.plateletsCount,
                                plasmaCount = eval.countsByType.plasmaCount,
                                otherDonationCount = eval.countsByType.otherDonationCount,
                                isAvailable = isDonorAvailable,
                                availabilityStatus = if (!isDonorAvailable) "temporarily_unavailable" else "available"
                            )
                        )
                    }
                    val baseDonor = currentDonor ?: DonorProfile(
                        userId = currentUserId,
                        fullName = resolvedDonorName,
                        bloodGroup = resolvedBloodGroup,
                        area = currentUser?.area ?: "Mirpur-10, Dhaka",
                        address = currentUser?.address ?: "Mirpur-10, Dhaka",
                        phoneNumber = currentUser?.phoneNumber ?: "01700000000"
                    )
                    repository.saveDonorProfile(
                        baseDonor.copy(
                            lastDonationDate = eval.lastDonationDate,
                            lastDonationTime = eval.lastDonationTime,
                            nextEligibleDate = eval.nextEligibleDate,
                            donationType = eval.lastDonationType,
                            customDonationTypeName = eval.customDonationTypeName,
                            eligibilityIntervalDays = eval.governingIntervalDays,
                            ruleVersion = eval.governingRuleVersion,
                            totalDonations = eval.countsByType.totalConfirmed.coerceAtLeast(1L),
                            wholeBloodCount = eval.countsByType.wholeBloodCount,
                            plateletsCount = eval.countsByType.plateletsCount,
                            plasmaCount = eval.countsByType.plasmaCount,
                            otherDonationCount = eval.countsByType.otherDonationCount,
                            isAvailable = isDonorAvailable,
                            availabilityStatus = if (!isDonorAvailable) "temporarily_unavailable" else "available"
                        )
                    )
                    _statusMessage.value = if (eval.isRuleUndecided) {
                        if (_language.value == "bn") {
                            "রক্তদান সংরক্ষিত হয়েছে। পরবর্তী তারিখ নির্ধারণ করা যায়নি (কাউন্টডাউন বন্ধ রয়েছে)।"
                        } else {
                            "Donation recorded. Next eligible date could not be determined (Countdown stopped)."
                        }
                    } else {
                        if (_language.value == "bn") {
                            "রক্তদান রেকর্ড হয়েছে এবং প্রযোজ্য নিয়ম অনুযায়ী লাইভ কাউন্টডাউন শুরু হয়েছে!"
                        } else {
                            "Blood donation recorded & smart live countdown started!"
                        }
                    }
                } else {
                    _statusMessage.value = if (_language.value == "bn") {
                        "রক্তদানের রেকর্ড যাচাইকরণের জন্য জমা হয়েছে (যাচাইকরণ অপেক্ষমাণ)"
                    } else {
                        "Donation submitted for verification (Pending Verification)"
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to record blood donation"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Updates an EligibilityRule in `/eligibility_rules/{donationType}` and automatically recalculates
     * target dates and rule versions for affected donation records and donor profiles (Section 4.6 of Prompt).
     */
    fun saveEligibilityRuleAndRecalculate(updatedRule: EligibilityRule) {
        val ruleKey = updatedRule.donationType.ifBlank { "whole_blood" }
        val normalized = updatedRule.copy(
            id = ruleKey,
            donationType = ruleKey,
            updatedByEmail = defaultEmail.ifBlank { "md.shahin0962@gmail.com" }
        )

        if (isGuest) {
            val newRulesMap = _guestEligibilityRules.value.toMutableMap().apply {
                put(ruleKey, normalized)
            }
            _guestEligibilityRules.value = newRulesMap

            // Recalculate all guest donation records of this type
            val allDonations = (_guestDonations.value as? UiState.Success)?.data.orEmpty()
            val recalculatedDonations = allDonations.map { rec ->
                if (rec.donationType == ruleKey) {
                    val newNext = if (!normalized.isVerifiedRule || normalized.intervalDays <= 0L) {
                        "UNDECIDED"
                    } else {
                        DonationCountdownEngine.computeNextEligibleDateString(rec.donationDate, normalized.intervalDays)
                    }
                    rec.copy(
                        eligibilityIntervalDays = if (normalized.isVerifiedRule) normalized.intervalDays else 0L,
                        ruleVersion = normalized.ruleVersion,
                        nextEligibleDate = newNext,
                        updatedAt = Timestamp.now()
                    )
                } else {
                    rec
                }
            }
            _guestDonations.value = UiState.Success(recalculatedDonations)

            // Recalculate guest user & donor profile
            val myRecs = recalculatedDonations.filter { it.userId == currentUserId }
            val myEval = DonationCountdownEngine.evaluateMultiRecordEligibility(myRecs, newRulesMap)
            val currDonor = (_guestMyDonor.value as? UiState.Success)?.data
            if (currDonor != null && myEval.hasVerifiedRecords) {
                val updatedMyDonor = currDonor.copy(
                    lastDonationDate = myEval.lastDonationDate,
                    lastDonationTime = myEval.lastDonationTime,
                    nextEligibleDate = myEval.nextEligibleDate,
                    donationType = myEval.lastDonationType,
                    eligibilityIntervalDays = myEval.governingIntervalDays,
                    ruleVersion = myEval.governingRuleVersion,
                    updatedAt = Timestamp.now()
                )
                _guestMyDonor.value = UiState.Success(updatedMyDonor)
            }

            // Recalculate public guest donors
            val pubDonors = (_guestPublicDonors.value as? UiState.Success)?.data.orEmpty()
            _guestPublicDonors.value = UiState.Success(
                pubDonors.map { donor ->
                    if (donor.donationType == ruleKey) {
                        val newNext = if (!normalized.isVerifiedRule || normalized.intervalDays <= 0L) {
                            "UNDECIDED"
                        } else {
                            DonationCountdownEngine.computeNextEligibleDateString(donor.lastDonationDate, normalized.intervalDays)
                        }
                        donor.copy(
                            eligibilityIntervalDays = if (normalized.isVerifiedRule) normalized.intervalDays else 0L,
                            ruleVersion = normalized.ruleVersion,
                            nextEligibleDate = newNext,
                            updatedAt = Timestamp.now()
                        )
                    } else {
                        donor
                    }
                }
            )

            _statusMessage.value = if (_language.value == "bn") {
                "চিকিৎসাগত নিয়ম (${normalized.ruleVersion}) সংরক্ষিত হয়েছে এবং পরবর্তী সম্ভাব্য তারিখ পুনঃগণনা করা হয়েছে!"
            } else {
                "Eligibility rule (${normalized.ruleVersion}) saved and target dates recalculated!"
            }
            return
        }

        viewModelScope.launch {
            try {
                repository.saveEligibilityRule(normalized)
                val updatedRulesMap = eligibilityRulesState.value.toMutableMap().apply {
                    put(ruleKey, normalized)
                }

                // Recalculate current user's affected records
                val myRecords = (myDonationsState.value as? UiState.Success)?.data.orEmpty()
                val updatedMyRecords = myRecords.map { rec ->
                    if (rec.donationType == ruleKey) {
                        val newNext = if (!normalized.isVerifiedRule || normalized.intervalDays <= 0L) {
                            "UNDECIDED"
                        } else {
                            DonationCountdownEngine.computeNextEligibleDateString(rec.donationDate, normalized.intervalDays)
                        }
                        val newIv = if (normalized.isVerifiedRule) normalized.intervalDays else 0L
                        repository.updateDonationRecordVerificationAndDates(
                            recordId = rec.id,
                            verificationStatus = rec.verificationStatus,
                            donationDate = rec.donationDate,
                            donationTime = rec.donationTime,
                            donationType = rec.donationType,
                            customDonationTypeName = rec.customDonationTypeName,
                            procedureConfirmation = rec.procedureConfirmation,
                            eligibilityIntervalDays = newIv,
                            ruleVersion = normalized.ruleVersion,
                            nextEligibleDate = newNext,
                            notes = rec.notes
                        )
                        rec.copy(
                            eligibilityIntervalDays = newIv,
                            ruleVersion = normalized.ruleVersion,
                            nextEligibleDate = newNext
                        )
                    } else {
                        rec
                    }
                }

                val eval = DonationCountdownEngine.evaluateMultiRecordEligibility(updatedMyRecords, updatedRulesMap)
                val currDonor = (myDonorState.value as? UiState.Success)?.data
                if (currDonor != null && eval.hasVerifiedRecords) {
                    repository.saveDonorProfile(
                        currDonor.copy(
                            lastDonationDate = eval.lastDonationDate,
                            lastDonationTime = eval.lastDonationTime,
                            nextEligibleDate = eval.nextEligibleDate,
                            donationType = eval.lastDonationType,
                            customDonationTypeName = eval.customDonationTypeName,
                            eligibilityIntervalDays = eval.governingIntervalDays,
                            ruleVersion = eval.governingRuleVersion
                        )
                    )
                }

                _statusMessage.value = if (_language.value == "bn") {
                    "চিকিৎসাগত নিয়ম (${normalized.ruleVersion}) সংরক্ষিত হয়েছে এবং পরবর্তী সম্ভাব্য তারিখ পুনঃগণনা করা হয়েছে!"
                } else {
                    "Eligibility rule (${normalized.ruleVersion}) saved and target dates recalculated!"
                }
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to update eligibility rule"
            }
        }
    }

    fun verifyOrUpdateDonationRecord(
        record: DonationRecord,
        newVerificationStatus: String
    ) {
        val rules = eligibilityRulesState.value
        val activeRule = rules[record.donationType] ?: DEFAULT_ELIGIBILITY_RULES[record.donationType]
        val isUndecided = record.donationType == "other" && (activeRule == null || !activeRule.isVerifiedRule) && record.eligibilityIntervalDays <= 0L

        if (isGuest) {
            val currentList = (_guestDonations.value as? UiState.Success)?.data.orEmpty()
            val updatedList = currentList.map {
                if (it.id == record.id) it.copy(verificationStatus = newVerificationStatus) else it
            }
            _guestDonations.value = UiState.Success(updatedList)
            if (record.userId == currentUserId) {
                val myRecs = updatedList.filter { it.userId == currentUserId }
                val eval = DonationCountdownEngine.evaluateMultiRecordEligibility(myRecs, rules)
                val currentDonor = (_guestMyDonor.value as? UiState.Success)?.data
                if (currentDonor != null && eval.hasVerifiedRecords) {
                    val updatedDonor = currentDonor.copy(
                        lastDonationDate = eval.lastDonationDate,
                        lastDonationTime = eval.lastDonationTime,
                        nextEligibleDate = eval.nextEligibleDate,
                        donationType = eval.lastDonationType,
                        customDonationTypeName = eval.customDonationTypeName,
                        eligibilityIntervalDays = eval.governingIntervalDays,
                        ruleVersion = eval.governingRuleVersion,
                        totalDonations = eval.countsByType.totalConfirmed,
                        wholeBloodCount = eval.countsByType.wholeBloodCount,
                        plateletsCount = eval.countsByType.plateletsCount,
                        plasmaCount = eval.countsByType.plasmaCount,
                        otherDonationCount = eval.countsByType.otherDonationCount
                    )
                    _guestMyDonor.value = UiState.Success(updatedDonor)
                }
            }
            _statusMessage.value = if (_language.value == "bn") {
                "রক্তদান রেকর্ডের যাচাইকরণ অবস্থা আপডেট করা হয়েছে"
            } else {
                "Donation record verification status updated"
            }
            return
        }

        viewModelScope.launch {
            try {
                val resolvedNext = if (isUndecided) {
                    "UNDECIDED"
                } else {
                    record.nextEligibleDate.ifBlank {
                        DonationCountdownEngine.computeNextEligibleDateString(
                            record.donationDate,
                            record.eligibilityIntervalDays
                        )
                    }
                }
                repository.updateDonationRecordVerificationAndDates(
                    recordId = record.id,
                    verificationStatus = newVerificationStatus,
                    donationDate = record.donationDate,
                    donationTime = record.donationTime,
                    donationType = record.donationType,
                    customDonationTypeName = record.customDonationTypeName,
                    procedureConfirmation = record.procedureConfirmation,
                    eligibilityIntervalDays = record.eligibilityIntervalDays,
                    ruleVersion = record.ruleVersion,
                    nextEligibleDate = resolvedNext,
                    notes = record.notes
                )
                if (newVerificationStatus == "verified" && record.userId == currentUserId) {
                    val currentDonor = (myDonorState.value as? UiState.Success)?.data
                    val myRecs = (myDonationsState.value as? UiState.Success)?.data.orEmpty().map {
                        if (it.id == record.id) it.copy(verificationStatus = newVerificationStatus, nextEligibleDate = resolvedNext) else it
                    }
                    val eval = DonationCountdownEngine.evaluateMultiRecordEligibility(myRecs, rules)
                    if (currentDonor != null && eval.hasVerifiedRecords) {
                        repository.saveDonorProfile(
                            currentDonor.copy(
                                lastDonationDate = eval.lastDonationDate,
                                lastDonationTime = eval.lastDonationTime,
                                nextEligibleDate = eval.nextEligibleDate,
                                donationType = eval.lastDonationType,
                                customDonationTypeName = eval.customDonationTypeName,
                                eligibilityIntervalDays = eval.governingIntervalDays,
                                ruleVersion = eval.governingRuleVersion,
                                totalDonations = eval.countsByType.totalConfirmed,
                                wholeBloodCount = eval.countsByType.wholeBloodCount,
                                plateletsCount = eval.countsByType.plateletsCount,
                                plasmaCount = eval.countsByType.plasmaCount,
                                otherDonationCount = eval.countsByType.otherDonationCount
                            )
                        )
                    }
                }
                _statusMessage.value = if (_language.value == "bn") {
                    "রক্তদান রেকর্ডের যাচাইকরণ অবস্থা আপডেট করা হয়েছে"
                } else {
                    "Donation record verification status updated"
                }
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to update verification status"
            }
        }
    }

    fun adminCorrectDonorDates(
        donor: DonorProfile,
        correctedLastDonationDate: String,
        correctedNextEligibleDate: String,
        correctedIntervalDays: Long,
        correctedTotalDonations: Long
    ) {
        val donorId = donor.id.ifBlank { donor.userId }
        if (isGuest) {
            val updatedDonor = donor.copy(
                lastDonationDate = correctedLastDonationDate,
                nextEligibleDate = correctedNextEligibleDate,
                eligibilityIntervalDays = correctedIntervalDays,
                totalDonations = correctedTotalDonations,
                updatedAt = Timestamp.now()
            )
            if (donorId == currentUserId) {
                _guestMyDonor.value = UiState.Success(updatedDonor)
                val currUser = (_guestUserProfile.value as? UiState.Success)?.data
                if (currUser != null) {
                    _guestUserProfile.value = UiState.Success(
                        currUser.copy(
                            lastDonationDate = correctedLastDonationDate,
                            nextEligibleDate = correctedNextEligibleDate,
                            eligibilityIntervalDays = correctedIntervalDays
                        )
                    )
                }
            }
            val currentPublic = (_guestPublicDonors.value as? UiState.Success)?.data.orEmpty()
            _guestPublicDonors.value = UiState.Success(
                currentPublic.map {
                    if (it.id == donorId || it.userId == donorId) updatedDonor else it
                }
            )
            _statusMessage.value = if (_language.value == "bn") {
                "রক্তদাতার তারিখ ও পরবর্তী যোগ্যতার সময় সংশোধন করা হয়েছে!"
            } else {
                "Donor donation date & eligibility countdown corrected!"
            }
            return
        }

        viewModelScope.launch {
            try {
                repository.adminCorrectDonorDates(
                    donorId = donorId,
                    lastDonationDate = correctedLastDonationDate,
                    lastDonationTime = donor.lastDonationTime,
                    nextEligibleDate = correctedNextEligibleDate,
                    donationType = donor.donationType,
                    customDonationTypeName = donor.customDonationTypeName,
                    eligibilityIntervalDays = correctedIntervalDays,
                    ruleVersion = donor.ruleVersion,
                    totalDonations = correctedTotalDonations
                )
                _statusMessage.value = if (_language.value == "bn") {
                    "রক্তদাতার তারিখ ও পরবর্তী যোগ্যতার সময় সংশোধন করা হয়েছে!"
                } else {
                    "Donor donation date & eligibility countdown corrected!"
                }
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to correct donor dates"
            }
        }
    }

    fun adminCorrectDonorDates(
        donorId: String,
        lastDate: String,
        lastTime: String,
        nextDate: String,
        type: String,
        customType: String,
        interval: Long,
        ruleVer: String,
        count: Long,
        wb: Long?,
        plt: Long?,
        pls: Long?,
        oth: Long?
    ) {
        if (isGuest) {
            val currentPublic = (_guestPublicDonors.value as? UiState.Success)?.data.orEmpty()
            val existing = currentPublic.find { it.id == donorId || it.userId == donorId }
            if (existing != null) {
                adminCorrectDonorDates(
                    donor = existing,
                    correctedLastDonationDate = lastDate,
                    correctedNextEligibleDate = nextDate,
                    correctedIntervalDays = interval,
                    correctedTotalDonations = existing.totalDonations.coerceAtLeast(count)
                )
            } else {
                _statusMessage.value = if (_language.value == "bn") "রক্তদাতার তারিখ সংশোধন সম্পন্ন!" else "Donor dates updated!"
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.adminCorrectDonorDates(
                    donorId = donorId,
                    lastDonationDate = lastDate,
                    lastDonationTime = lastTime,
                    nextEligibleDate = nextDate,
                    donationType = type,
                    customDonationTypeName = customType,
                    eligibilityIntervalDays = interval,
                    ruleVersion = ruleVer,
                    totalDonations = count
                )
                repository.logAdminAction(
                    AdminLog(
                        adminUid = currentUserId,
                        adminEmail = defaultEmail,
                        actionType = "DONOR_DATES_CORRECTED",
                        targetId = donorId,
                        targetType = "donor",
                        reason = "Corrected donation dates: last=$lastDate, next=$nextDate, type=$type",
                        status = "success"
                    )
                )
                _statusMessage.value = if (_language.value == "bn") {
                    "রক্তদাতার তারিখ ও পরবর্তী যোগ্যতার সময় সংশোধন করা হয়েছে!"
                } else {
                    "Donor donation date & eligibility countdown corrected!"
                }
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to correct donor dates"
            }
        }
    }

    fun submitBloodRequest(
        bloodGroup: String,
        hospitalName: String,
        location: String,
        patientName: String,
        contactNumber: String,
        neededDateTime: String,
        bagsNeeded: Long,
        urgency: String,
        notes: String,
        donationType: String = "whole_blood",
        onSuccess: (String) -> Unit
    ) {
        if (isCurrentUserBanned.value) {
            _statusMessage.value = if (_language.value == "bn") {
                "আপনার অ্যাকাউন্ট স্থগিত থাকায় নতুন রক্তের আবেদন তৈরি করা সম্ভব নয়।"
            } else {
                "Your account is suspended. You cannot create new blood requests."
            }
            return
        }
        val hName = hospitalName.trim()
        val loc = location.trim()
        val pName = patientName.trim()
        val phone = contactNumber.trim()
        val dtNeeded = neededDateTime.trim()
        val dType = donationType.trim().ifBlank { "whole_blood" }

        if (hName.isEmpty() || loc.isEmpty() || pName.isEmpty() || phone.length < 6) {
            _statusMessage.value = if (_language.value == "bn") {
                "অনুগ্রহ করে সকল তারকা (*) চিহ্নিত ঘর সঠিকভাবে পূরণ করুন"
            } else {
                "Please fill in all required (*) fields with valid info"
            }
            return
        }

        val targetDonor = _preselectedRequestDonor.value
        val assocDonorId = targetDonor?.userId?.ifBlank { targetDonor.id } ?: ""
        val assocDonorName = targetDonor?.fullName ?: ""

        if (isGuest) {
            val currentProfile = (userProfileState.value as? UiState.Success)?.data
            val requester = currentProfile?.fullName?.ifBlank { defaultDisplayName }
                ?: defaultDisplayName.ifBlank { "MBD Guest" }
            val newId = "guest_req_${UUID.randomUUID().toString().take(8)}"
            val newReq = BloodRequest(
                id = newId,
                userId = currentUserId,
                requesterName = requester,
                patientName = pName,
                bloodGroup = bloodGroup,
                donationType = dType,
                bagsNeeded = bagsNeeded.coerceIn(1L, 20L),
                hospitalName = hName,
                location = loc,
                contactNumber = phone,
                neededDateTime = dtNeeded,
                urgency = urgency,
                notes = notes.trim(),
                status = "open",
                visibility = "community",
                authorizedDonorId = assocDonorId,
                authorizedDonorName = assocDonorName,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            val currentList = (_guestBloodRequests.value as? UiState.Success)?.data.orEmpty()
            _guestBloodRequests.value = UiState.Success(listOf(newReq) + currentList)
            _selectedRequestId.value = newId
            _preselectedRequestDonor.value = null
            _statusMessage.value = if (_language.value == "bn") {
                if (assocDonorName.isNotBlank()) {
                    "রক্তের আবেদন প্রকাশিত হয়েছে এবং রক্তদাতা $assocDonorName-এর সাথে যুক্ত করা হয়েছে!"
                } else {
                    "রক্তের আবেদন প্রকাশিত হয়েছে! এখন চাইলে রক্তদাতার সাথে লাইভ লোকেশন শেয়ার করতে পারেন।"
                }
            } else {
                if (assocDonorName.isNotBlank()) {
                    "Blood request submitted and associated with donor $assocDonorName!"
                } else {
                    "Blood request submitted! You can now share your live location with a donor."
                }
            }
            onSuccess(newId)
            return
        }

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val currentProfile = (userProfileState.value as? UiState.Success)?.data
                val requester = currentProfile?.fullName?.ifBlank { defaultDisplayName }
                    ?: defaultDisplayName.ifBlank { "MBD Member" }

                val createdId = repository.createBloodRequest(
                    BloodRequest(
                        userId = currentUserId,
                        requesterName = requester,
                        patientName = pName,
                        bloodGroup = bloodGroup,
                        donationType = dType,
                        bagsNeeded = bagsNeeded.coerceIn(1L, 20L),
                        hospitalName = hName,
                        location = loc,
                        contactNumber = phone,
                        neededDateTime = dtNeeded,
                        urgency = urgency,
                        notes = notes.trim(),
                        status = "open",
                        visibility = "community",
                        authorizedDonorId = assocDonorId,
                        authorizedDonorName = assocDonorName
                    )
                )
                _selectedRequestId.value = createdId
                _preselectedRequestDonor.value = null
                _statusMessage.value = if (_language.value == "bn") {
                    if (assocDonorName.isNotBlank()) {
                        "রক্তের আবেদন প্রকাশিত হয়েছে এবং রক্তদাতা $assocDonorName-এর সাথে যুক্ত করা হয়েছে!"
                    } else {
                        "রক্তের আবেদন প্রকাশিত হয়েছে! এখন চাইলে রক্তদাতার সাথে লাইভ লোকেশন শেয়ার করতে পারেন।"
                    }
                } else {
                    if (assocDonorName.isNotBlank()) {
                        "Blood request submitted and associated with donor $assocDonorName!"
                    } else {
                        "Blood request submitted! You can now share your live location with a donor."
                    }
                }
                onSuccess(createdId)
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to submit blood request"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun startLiveLocationSharing(
        request: BloodRequest,
        authorizedDonorId: String,
        authorizedDonorName: String,
        durationMinutes: Int,
        initialLat: Double,
        initialLng: Double,
        initialAcc: Double,
        onStarted: () -> Unit
    ) {
        val expiresAtMs = System.currentTimeMillis() + durationMinutes.coerceIn(5, 240) * 60_000L
        val session = LiveLocationSession(
            id = request.id,
            requestId = request.id,
            requesterId = currentUserId,
            requesterName = request.requesterName.ifBlank { defaultDisplayName },
            authorizedDonorId = authorizedDonorId,
            authorizedDonorName = authorizedDonorName,
            latitude = initialLat,
            longitude = initialLng,
            accuracyMeters = initialAcc.coerceIn(1.0, 50000.0),
            isActive = true,
            expiresAtEpochMs = expiresAtMs,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        if (isGuest) {
            _guestLiveSessions.value = _guestLiveSessions.value + (request.id to session)
            val currentList = (_guestBloodRequests.value as? UiState.Success)?.data.orEmpty()
            _guestBloodRequests.value = UiState.Success(
                currentList.map {
                    if (it.id == request.id) {
                        it.copy(
                            authorizedDonorId = authorizedDonorId,
                            authorizedDonorName = authorizedDonorName,
                            liveSessionActive = true,
                            liveSessionExpiresAt = expiresAtMs,
                            updatedAt = Timestamp.now()
                        )
                    } else {
                        it
                    }
                }
            )
            _statusMessage.value = if (_language.value == "bn") {
                "আপনার লোকেশন শেয়ার হচ্ছে ($authorizedDonorName)"
            } else {
                "Live location sharing started with $authorizedDonorName"
            }
            onStarted()
            return
        }

        viewModelScope.launch {
            try {
                repository.saveLiveLocationSession(session)
                repository.updateBloodRequestLiveSessionMetadata(
                    requestId = request.id,
                    authorizedDonorId = authorizedDonorId,
                    authorizedDonorName = authorizedDonorName,
                    liveSessionActive = true,
                    liveSessionExpiresAt = expiresAtMs
                )
                _statusMessage.value = if (_language.value == "bn") {
                    "আপনার লোকেশন শেয়ার হচ্ছে ($authorizedDonorName)"
                } else {
                    "Live location sharing started with $authorizedDonorName"
                }
                onStarted()
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to start live location sharing"
            }
        }
    }

    fun updateLiveLocationCoordinates(
        requestId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double
    ) {
        val currentSession = (activeLiveSessionState.value as? UiState.Success)?.data ?: return
        if (!currentSession.isActive || currentSession.requestId != requestId) return

        val updated = currentSession.copy(
            latitude = latitude.coerceIn(-90.0, 90.0),
            longitude = longitude.coerceIn(-180.0, 180.0),
            accuracyMeters = accuracyMeters.coerceIn(0.0, 50000.0),
            updatedAt = Timestamp.now()
        )

        if (isGuest) {
            _guestLiveSessions.value = _guestLiveSessions.value + (requestId to updated)
            return
        }

        viewModelScope.launch {
            try {
                repository.saveLiveLocationSession(updated)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update live location coordinates", e)
            }
        }
    }

    fun stopLiveLocationSharing(
        requestId: String,
        navigateToEndedScreen: Boolean = true
    ) {
        if (isGuest) {
            _guestLiveSessions.value = _guestLiveSessions.value - requestId
            val currentList = (_guestBloodRequests.value as? UiState.Success)?.data.orEmpty()
            _guestBloodRequests.value = UiState.Success(
                currentList.map {
                    if (it.id == requestId) {
                        it.copy(
                            liveSessionActive = false,
                            liveSessionExpiresAt = 0L,
                            updatedAt = Timestamp.now()
                        )
                    } else {
                        it
                    }
                }
            )
            _statusMessage.value = if (_language.value == "bn") {
                "লোকেশন শেয়ার বন্ধ করা হয়েছে এবং লাইভ ডেটা মুছে ফেলা হয়েছে"
            } else {
                "Location sharing stopped and live coordinates deleted"
            }
            if (navigateToEndedScreen) {
                navigateTo(MbdScreen.LOCATION_SHARING_ENDED)
            }
            return
        }

        viewModelScope.launch {
            try {
                val req = ((bloodRequestsState.value as? UiState.Success)?.data.orEmpty())
                    .find { it.id == requestId }
                if (req != null && req.userId == currentUserId && req.status == "open") {
                    repository.updateBloodRequestLiveSessionMetadata(
                        requestId = requestId,
                        authorizedDonorId = req.authorizedDonorId,
                        authorizedDonorName = req.authorizedDonorName,
                        liveSessionActive = false,
                        liveSessionExpiresAt = 0L
                    )
                }
                repository.deleteLiveLocationSession(requestId)
                _statusMessage.value = if (_language.value == "bn") {
                    "লোকেশন শেয়ার বন্ধ করা হয়েছে এবং লাইভ ডেটা মুছে ফেলা হয়েছে"
                } else {
                    "Location sharing stopped and live coordinates deleted"
                }
                if (navigateToEndedScreen) {
                    navigateTo(MbdScreen.LOCATION_SHARING_ENDED)
                }
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to stop live location sharing"
            }
        }
    }

    fun acceptBloodRequest(request: BloodRequest) {
        val myDonor = (myDonorState.value as? UiState.Success)?.data
        val isDonorAvailable = myDonor?.isAvailable == true
        if (!isDonorAvailable) {
            _statusMessage.value = if (_language.value == "bn") {
                "আপনি বর্তমানে কোল্ডডাউন পিরিয়ডে আছেন (রক্তদানে প্রস্তুত নন)। এখন নতুন কোনো আবেদন গ্রহণ করা সম্ভব নয়।"
            } else {
                "You are currently in a cooldown period. You cannot accept new requests right now."
            }
            return
        }
        viewModelScope.launch {
            try {
                val donorName = myDonor?.fullName ?: "Donor"
                repository.updateBloodRequestStatus(request.id, "accepted")
                try {
                    repository.updateBloodRequestLiveSessionMetadata(
                        requestId = request.id,
                        authorizedDonorId = currentUserId,
                        authorizedDonorName = donorName,
                        liveSessionActive = false,
                        liveSessionExpiresAt = 0L
                    )
                } catch (ignored: Exception) {
                }
                _statusMessage.value = if (_language.value == "bn") {
                    "আবেদনটি সফলভাবে গ্রহণ করা হয়েছে! আবেদনকারীর সাথে যোগাযোগ করুন।"
                } else {
                    "Request accepted! Please contact the requester."
                }
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to accept request"
            }
        }
    }

    fun confirmAndFulfillRequestWithDonation(request: BloodRequest, onSuccess: () -> Unit = {}) {
        if (isCurrentUserBanned.value) {
            _statusMessage.value = if (_language.value == "bn") {
                "আপনার অ্যাকাউন্ট স্থগিত থাকায় রক্তদানের তথ্য যোগ করা সম্ভব নয়।"
            } else {
                "Your account is suspended."
            }
            return
        }

        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val reqDonationType = request.donationType.ifBlank { "whole_blood" }
        val reqBloodGroup = request.bloodGroup.ifBlank { "O+" }
        val reqHospital = request.hospitalName.ifBlank { "Hospital" }
        val reqLocation = request.location.ifBlank { "Mirpur, Dhaka" }
        val reqPatient = request.patientName.ifBlank { "Patient" }

        markRequestStatus(request.id, "completed")

        recordBloodDonation(
            donationDate = todayIso,
            donationTime = nowTime,
            donationType = reqDonationType,
            hospitalName = reqHospital,
            location = reqLocation,
            recipientName = reqPatient,
            bloodGroup = reqBloodGroup,
            notes = if (_language.value == "bn") {
                "রক্তদান সম্পন্ন - আবেদন #${request.id.takeLast(6).uppercase()}"
            } else {
                "Completed blood donation for request #${request.id.takeLast(6).uppercase()}"
            },
            verificationStatus = "verified",
            onSuccess = {
                _statusMessage.value = if (_language.value == "bn") {
                    "রক্তদান সফলভাবে সম্পন্ন হিসেবে সংরক্ষিত হয়েছে! আপনার পরবর্তী রক্তদানের সময় পর্যন্ত কাউন্টডাউন শুরু হয়েছে।"
                } else {
                    "Blood donation recorded! Countdown timer started for your next eligible date."
                }
                onSuccess()
            }
        )
    }

    fun markRequestStatus(requestId: String, newStatus: String) {
        if (isGuest) {
            _guestLiveSessions.value = _guestLiveSessions.value - requestId
            val currentList = (_guestBloodRequests.value as? UiState.Success)?.data.orEmpty()
            _guestBloodRequests.value = UiState.Success(
                currentList.map {
                    if (it.id == requestId) it.copy(status = newStatus, liveSessionActive = false) else it
                }
            )
            _statusMessage.value = if (_language.value == "bn") {
                "আবেদনের অবস্থা আপডেট করা হয়েছে"
            } else {
                "Request status updated"
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.updateBloodRequestStatus(requestId, newStatus)
                try {
                    repository.deleteLiveLocationSession(requestId)
                } catch (ignored: Exception) {
                }
                _statusMessage.value = if (_language.value == "bn") {
                    "আবেদনের অবস্থা আপডেট করা হয়েছে"
                } else {
                    "Request status updated"
                }
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to update status"
            }
        }
    }

    fun deleteRequest(requestId: String) {
        if (isGuest) {
            _guestLiveSessions.value = _guestLiveSessions.value - requestId
            val currentList = (_guestBloodRequests.value as? UiState.Success)?.data.orEmpty()
            _guestBloodRequests.value = UiState.Success(currentList.filterNot { it.id == requestId })
            _statusMessage.value = if (_language.value == "bn") "আবেদন মুছে ফেলা হয়েছে" else "Request deleted"
            return
        }
        viewModelScope.launch {
            try {
                try {
                    repository.deleteLiveLocationSession(requestId)
                } catch (ignored: Exception) {
                }
                repository.deleteBloodRequest(requestId)
                _statusMessage.value = if (_language.value == "bn") "আবেদন মুছে ফেলা হয়েছে" else "Request deleted"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to delete request"
            }
        }
    }

    fun deleteDonation(donationId: String) {
        if (isGuest) {
            val currentList = (_guestDonations.value as? UiState.Success)?.data.orEmpty()
            val updatedList = currentList.filterNot { it.id == donationId }
            _guestDonations.value = UiState.Success(updatedList)
            val myRecs = updatedList.filter { it.userId == currentUserId }
            val eval = DonationCountdownEngine.evaluateMultiRecordEligibility(myRecs, eligibilityRulesState.value)
            val currDonor = (_guestMyDonor.value as? UiState.Success)?.data
            if (currDonor != null) {
                _guestMyDonor.value = UiState.Success(
                    currDonor.copy(
                        lastDonationDate = eval.lastDonationDate,
                        lastDonationTime = eval.lastDonationTime,
                        nextEligibleDate = eval.nextEligibleDate,
                        donationType = eval.lastDonationType,
                        customDonationTypeName = eval.customDonationTypeName,
                        eligibilityIntervalDays = eval.governingIntervalDays,
                        ruleVersion = eval.governingRuleVersion,
                        totalDonations = eval.countsByType.totalConfirmed,
                        wholeBloodCount = eval.countsByType.wholeBloodCount,
                        plateletsCount = eval.countsByType.plateletsCount,
                        plasmaCount = eval.countsByType.plasmaCount,
                        otherDonationCount = eval.countsByType.otherDonationCount
                    )
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteDonationRecord(donationId)
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to delete donation record"
            }
        }
    }

    fun reportBloodRequest(requestId: String, reason: String) {
        val trimmedReason = reason.trim()
        if (isGuest) {
            val currentList = (_guestBloodRequests.value as? UiState.Success)?.data.orEmpty()
            _guestBloodRequests.value = UiState.Success(
                currentList.map {
                    if (it.id == requestId) it.copy(isReported = true, reportReason = trimmedReason) else it
                }
            )
            _statusMessage.value = if (_language.value == "bn") {
                "রিপোর্ট জমা দেওয়া হয়েছে। এডমিন দ্বারা পর্যবেক্ষণ করা হবে।"
            } else {
                "Report submitted. Admins will review this request."
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.reportBloodRequest(requestId, trimmedReason)
                _statusMessage.value = if (_language.value == "bn") {
                    "রিপোর্ট জমা দেওয়া হয়েছে। এডমিন দ্বারা পর্যবেক্ষণ করা হবে।"
                } else {
                    "Report submitted. Admins will review this request."
                }
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to report request"
            }
        }
    }

    fun reportDonorProfile(donorId: String, reason: String) {
        val trimmedReason = reason.trim()
        if (isGuest) {
            val currentList = (_guestPublicDonors.value as? UiState.Success<List<DonorProfile>>)?.data.orEmpty()
            _guestPublicDonors.value = UiState.Success(
                currentList.map {
                    if (it.id == donorId || it.userId == donorId) it.copy(isReported = true, reportReason = trimmedReason) else it
                }
            )
            _statusMessage.value = if (_language.value == "bn") {
                "রিপোর্ট জমা দেওয়া হয়েছে। এডমিন প্রোফাইলটি পর্যবেক্ষণ করবে।"
            } else {
                "Report submitted. Admins will review this donor profile."
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.reportDonorProfile(donorId, trimmedReason)
                _statusMessage.value = if (_language.value == "bn") {
                    "রিপোর্ট জমা দেওয়া হয়েছে। এডমিন প্রোফাইলটি পর্যবেক্ষণ করবে।"
                } else {
                    "Report submitted. Admins will review this donor profile."
                }
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to report donor profile"
            }
        }
    }

    // Admin Control Panel Operations
    fun assignRoleToUser(
        targetUserId: String,
        role: String,
        permissions: List<String>,
        onSuccess: () -> Unit = {}
    ) {
        if (!hasPermission("manage_users")) {
            _statusMessage.value = if (_language.value == "bn") {
                "ইউজার পারমিশন পরিবর্তনের অধিকার আপনার নেই।"
            } else {
                "You do not have permission to manage user roles."
            }
            return
        }
        if (isGuest) {
            _statusMessage.value = if (_language.value == "bn") {
                "গেস্ট মোডে ভূমিকা পরিবর্তন সংরক্ষিত হয়েছে"
            } else {
                "User role updated in guest mode"
            }
            onSuccess()
            return
        }
        viewModelScope.launch {
            try {
                repository.assignUserRole(
                    targetUserId = targetUserId,
                    role = role,
                    permissions = permissions,
                    adminUid = currentUserId,
                    adminEmail = defaultEmail
                )
                _statusMessage.value = if (_language.value == "bn") {
                    "ইউজারের ভূমিকা ও পারমিশন সফলভাবে আপডেট করা হয়েছে!"
                } else {
                    "User role and permissions updated successfully!"
                }
                onSuccess()
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to assign user role"
            }
        }
    }

    fun banUser(userId: String, reason: String) {
        val trimmed = reason.trim()
        if (isGuest) {
            val bans = (_guestBans.value as? UiState.Success)?.data.orEmpty()
            _guestBans.value = UiState.Success(
                bans + AdminUserBan(
                    userId = userId,
                    bannedByUid = currentUserId,
                    bannedByEmail = defaultEmail,
                    reason = trimmed,
                    bannedAt = Timestamp.now()
                )
            )
            _statusMessage.value = if (_language.value == "bn") "ব্যবহারকারীকে ব্যান করা হয়েছে" else "User banned successfully"
            return
        }
        viewModelScope.launch {
            try {
                repository.banUser(userId, trimmed, currentUserId, defaultEmail)
                _statusMessage.value = if (_language.value == "bn") "ব্যবহারকারীকে ব্যান করা হয়েছে" else "User banned successfully"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to ban user"
            }
        }
    }

    fun unbanUser(userId: String) {
        if (isGuest) {
            val bans = (_guestBans.value as? UiState.Success)?.data.orEmpty()
            _guestBans.value = UiState.Success(bans.filter { it.userId != userId })
            _statusMessage.value = if (_language.value == "bn") "ব্যান প্রত্যাহার করা হয়েছে" else "User unbanned successfully"
            return
        }
        viewModelScope.launch {
            try {
                repository.unbanUser(userId, currentUserId, defaultEmail)
                _statusMessage.value = if (_language.value == "bn") "ব্যান প্রত্যাহার করা হয়েছে" else "User unbanned successfully"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to unban user"
            }
        }
    }

    fun disableOrRestoreUser(userId: String, disable: Boolean) {
        if (isGuest) {
            _statusMessage.value = if (_language.value == "bn") {
                if (disable) "অ্যাকাউন্ট নিষ্ক্রিয় করা হয়েছে" else "অ্যাকাউন্ট পুনঃসক্রিয় করা হয়েছে"
            } else {
                if (disable) "Account disabled" else "Account restored"
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.disableOrRestoreUser(userId, disable, currentUserId, defaultEmail)
                _statusMessage.value = if (disable) {
                    if (_language.value == "bn") "অ্যাকাউন্ট নিষ্ক্রিয় করা হয়েছে" else "Account disabled"
                } else {
                    if (_language.value == "bn") "অ্যাকাউন্ট পুনঃসক্রিয় করা হয়েছে" else "Account restored"
                }
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to update account status"
            }
        }
    }

    fun deleteUserAccount(userId: String) {
        if (userId == currentUserId) {
            _statusMessage.value = if (_language.value == "bn") {
                "প্রধান এডমিন অ্যাকাউন্ট মোছা যাবে না"
            } else {
                "Owner-admin account cannot be deleted"
            }
            return
        }
        if (isGuest) {
            val donors = (_guestPublicDonors.value as? UiState.Success)?.data.orEmpty()
            _guestPublicDonors.value = UiState.Success(donors.filter { it.userId != userId && it.id != userId })
            _statusMessage.value = if (_language.value == "bn") "ব্যবহারকারী অ্যাকাউন্ট সরানো হয়েছে" else "User account removed"
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteUserAccount(userId, currentUserId, defaultEmail)
                _statusMessage.value = if (_language.value == "bn") "ব্যবহারকারী অ্যাকাউন্ট সরানো হয়েছে" else "User account removed"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to delete account"
            }
        }
    }

    fun submitAdminReport(category: String, description: String, reportedId: String, reportedType: String) {
        val report = AdminReport(
            reporterUid = currentUserId,
            reporterName = defaultDisplayName,
            reportedId = reportedId,
            reportedType = reportedType,
            category = category,
            description = description.trim(),
            status = "pending"
        )
        if (isGuest) {
            val current = (_guestReports.value as? UiState.Success)?.data.orEmpty()
            _guestReports.value = UiState.Success(listOf(report.copy(id = UUID.randomUUID().toString(), createdAt = Timestamp.now())) + current)
            _statusMessage.value = if (_language.value == "bn") "রিপোর্ট জমা দেওয়া হয়েছে" else "Report submitted"
            return
        }
        viewModelScope.launch {
            try {
                repository.submitReport(report)
                _statusMessage.value = if (_language.value == "bn") "রিপোর্ট জমা দেওয়া হয়েছে" else "Report submitted"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to submit report"
            }
        }
    }

    fun updateReportStatus(reportId: String, status: String, adminNotes: String) {
        val trimmedNotes = adminNotes.trim()
        if (isGuest) {
            val current = (_guestReports.value as? UiState.Success)?.data.orEmpty()
            _guestReports.value = UiState.Success(
                current.map {
                    if (it.id == reportId) it.copy(status = status, adminNotes = trimmedNotes, resolvedAt = Timestamp.now()) else it
                }
            )
            _statusMessage.value = if (_language.value == "bn") "রিপোর্টের স্ট্যাটাস আপডেট হয়েছে" else "Report status updated"
            return
        }
        viewModelScope.launch {
            try {
                repository.updateReportStatus(reportId, status, trimmedNotes, currentUserId, defaultEmail)
                _statusMessage.value = if (_language.value == "bn") "রিপোর্টের স্ট্যাটাস আপডেট হয়েছে" else "Report status updated"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to update report status"
            }
        }
    }

    fun adminModerateBloodRequest(requestId: String, newStatus: String, reason: String) {
        val trimmedReason = reason.trim()
        if (isGuest) {
            val current = (_guestBloodRequests.value as? UiState.Success)?.data.orEmpty()
            _guestBloodRequests.value = UiState.Success(
                current.map {
                    if (it.id == requestId) it.copy(status = newStatus, notes = "Moderation: $trimmedReason", isReported = (newStatus == "cancelled")) else it
                }
            )
            _statusMessage.value = if (_language.value == "bn") "আবেদন মডারেট করা হয়েছে" else "Request moderated"
            return
        }
        viewModelScope.launch {
            try {
                repository.adminModerateBloodRequest(requestId, newStatus, trimmedReason, currentUserId, defaultEmail)
                _statusMessage.value = if (_language.value == "bn") "আবেদন মডারেট করা হয়েছে" else "Request moderated"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to moderate request"
            }
        }
    }

    fun adminVerifyDonationRecord(recordId: String, status: String, notes: String) {
        val trimmed = notes.trim()
        if (isGuest) {
            val current = (_guestDonations.value as? UiState.Success)?.data.orEmpty()
            _guestDonations.value = UiState.Success(
                current.map {
                    if (it.id == recordId) it.copy(verificationStatus = status, notes = trimmed) else it
                }
            )
            _statusMessage.value = if (_language.value == "bn") "রক্তদানের রেকর্ড যাচাই আপডেট হয়েছে" else "Donation record updated"
            return
        }
        viewModelScope.launch {
            try {
                repository.adminVerifyDonationRecord(recordId, status, trimmed, currentUserId, defaultEmail)
                _statusMessage.value = if (_language.value == "bn") "রক্তদানের রেকর্ড যাচাই আপডেট হয়েছে" else "Donation record updated"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to verify donation record"
            }
        }
    }

    fun adminSetDonorVerificationStatus(donorId: String, isPhoneVerified: Boolean, isAdminReviewed: Boolean) {
        if (isGuest) {
            val current = (_guestPublicDonors.value as? UiState.Success)?.data.orEmpty()
            _guestPublicDonors.value = UiState.Success(
                current.map {
                    if (it.id == donorId || it.userId == donorId) {
                        it.copy(isPhoneVerified = isPhoneVerified, isAdminReviewed = isAdminReviewed)
                    } else it
                }
            )
            _statusMessage.value = if (_language.value == "bn") "রক্তদাতার যাচাইকরণ স্ট্যাটাস সংরক্ষিত হয়েছে" else "Donor verification updated"
            return
        }
        viewModelScope.launch {
            try {
                repository.adminSetDonorVerificationStatus(donorId, isPhoneVerified, isAdminReviewed, currentUserId, defaultEmail)
                _statusMessage.value = if (_language.value == "bn") "রক্তদাতার যাচাইকরণ স্ট্যাটাস সংরক্ষিত হয়েছে" else "Donor verification updated"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to update donor verification"
            }
        }
    }

    fun createBroadcastAnnouncement(title: String, message: String, type: String, isEmergency: Boolean) {
        val trimmedTitle = title.trim()
        val trimmedMsg = message.trim()
        val announcement = AdminAnnouncement(
            title = trimmedTitle,
            message = trimmedMsg,
            type = type,
            isEmergency = isEmergency,
            isActive = true,
            createdByEmail = defaultEmail
        )
        if (isGuest) {
            val current = (_guestAnnouncements.value as? UiState.Success)?.data.orEmpty()
            _guestAnnouncements.value = UiState.Success(
                listOf(announcement.copy(id = UUID.randomUUID().toString(), createdAt = Timestamp.now())) + current
            )
            _statusMessage.value = if (_language.value == "bn") "ঘোষণা সফলভাবে প্রচার করা হয়েছে" else "Broadcast published successfully"
            return
        }
        viewModelScope.launch {
            try {
                repository.createAnnouncement(announcement, currentUserId, defaultEmail)
                _statusMessage.value = if (_language.value == "bn") "ঘোষণা সফলভাবে প্রচার করা হয়েছে" else "Broadcast published successfully"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to publish announcement"
            }
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        if (isGuest) {
            val current = (_guestAnnouncements.value as? UiState.Success)?.data.orEmpty()
            _guestAnnouncements.value = UiState.Success(current.filter { it.id != announcementId })
            _statusMessage.value = if (_language.value == "bn") "ঘোষণা সরানো হয়েছে" else "Announcement removed"
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteAnnouncement(announcementId)
                _statusMessage.value = if (_language.value == "bn") "ঘোষণা সরানো হয়েছে" else "Announcement removed"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to delete announcement"
            }
        }
    }

    fun saveAppConfig(config: AppConfig) {
        if (isGuest) {
            _guestAppConfig.value = config.copy(updatedAt = Timestamp.now())
            _statusMessage.value = if (_language.value == "bn") "অ্যাপ কনফিগারেশন সংরক্ষিত হয়েছে" else "App config saved"
            return
        }
        viewModelScope.launch {
            try {
                repository.saveAppConfig(config, currentUserId, defaultEmail)
                _statusMessage.value = if (_language.value == "bn") "অ্যাপ কনফিগারেশন সংরক্ষিত হয়েছে" else "App config saved"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to save app configuration"
            }
        }
    }

    private companion object {
        const val TAG = "MbdViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

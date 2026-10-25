package com.example.ui.screens

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.i18n.MbdStrings
import com.example.ui.theme.MbdAvailableGreen
import com.example.ui.theme.MbdBorderGray
import com.example.ui.theme.MbdChipUnselected
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdCrimsonLight
import com.example.ui.theme.MbdPreferencesManager
import com.example.ui.theme.MbdSurface
import com.example.ui.theme.MbdTextPrimary
import com.example.ui.theme.MbdTextSecondary
import com.example.ui.theme.MbdThemeMode
import com.example.ui.theme.MbdWarningAmber
import com.example.ui.util.MbdNotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InstalledAppVersionInfo(
    val versionName: String,
    val versionCode: Long
)

fun getInstalledAppVersionInfo(context: Context): InstalledAppVersionInfo {
    return try {
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val verName = pkgInfo.versionName ?: BuildConfig.VERSION_NAME
        val verCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }
        InstalledAppVersionInfo(versionName = verName, versionCode = verCode)
    } catch (e: Exception) {
        InstalledAppVersionInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong()
        )
    }
}

/**
 * Redesigned Settings Screen for Mirpur Blood Donation (MBD 🩸)
 *
 * Displays every setting as a separate, clean, clickable row (minimum touch height >= 64.dp):
 * 1. ভাষা নির্বাচন (Language)
 * 2. থিম (Theme)
 * 3. অ্যাপ আপডেট (App Update)
 * 4. অ্যাপ সম্পর্কে (About)
 * Plus Emergency Request Notification preference toggle.
 */
@Composable
fun SettingsScreen(
    strings: MbdStrings,
    currentLanguage: String,
    currentThemeMode: MbdThemeMode = MbdPreferencesManager.themeModeFlow.collectAsStateWithLifecycle().value,
    updateInfo: com.example.util.UpdateInfo? = null,
    onSelectLanguage: (String) -> Unit,
    onSelectThemeMode: (MbdThemeMode) -> Unit = { mode -> MbdPreferencesManager.setThemeMode(null, mode) },
    onOpenAppUpdate: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val versionInfo = remember(context) { getInstalledAppVersionInfo(context) }

    val notificationsPref by MbdPreferencesManager.notificationsEnabledFlow.collectAsStateWithLifecycle()
    var notifPermGranted by remember {
        mutableStateOf(MbdNotificationHelper.hasNotificationPermission(context) && notificationsPref)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermGranted = granted
        MbdPreferencesManager.setNotificationsEnabled(context, granted)
    }

    var showLanguagePopup by remember { mutableStateOf(false) }
    var showThemePopup by remember { mutableStateOf(false) }

    val currentLanguageBadgeText = if (currentLanguage == "en") {
        "English"
    } else {
        "বাংলা (Bangla)"
    }

    val currentThemeBadgeText = when (currentThemeMode) {
        MbdThemeMode.DARK -> strings.settingsThemeOptionDark
        MbdThemeMode.LIGHT -> strings.settingsThemeOptionLight
        MbdThemeMode.SYSTEM -> strings.settingsThemeOptionSystem
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen_root")
    ) {
        // Red Header matching MBD app design
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
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = strings.settings,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header: General Settings
            Text(
                text = if (strings.isBangla) "সাধারণ সেটিংস" else "General Settings",
                color = MbdTextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // 1. ভাষা নির্বাচন (Language) Row
            SettingsClickableRowCard(
                icon = Icons.Default.Language,
                title = strings.settingsLanguageRowTitle,
                description = strings.settingsLanguageRowDesc,
                currentValueBadge = currentLanguageBadgeText,
                onClick = { showLanguagePopup = true },
                testTag = "settings_row_language"
            )

            // 2. থিম (Theme) Row
            SettingsClickableRowCard(
                icon = Icons.Default.Palette,
                title = strings.settingsThemeRowTitle,
                description = strings.settingsThemeRowDesc,
                currentValueBadge = currentThemeBadgeText,
                onClick = { showThemePopup = true },
                testTag = "settings_row_theme"
            )

            // 3. অ্যাপ আপডেট (App Update) Row
            SettingsClickableRowCard(
                icon = Icons.Default.SystemUpdate,
                title = strings.settingsAppUpdateRowTitle,
                description = if (updateInfo?.requiresUpdate == true) {
                    if (strings.isBangla) "নতুন আপডেট উপলব্ধ!" else "New update available!"
                } else {
                    strings.settingsAppUpdateRowDesc
                },
                currentValueBadge = if (updateInfo?.requiresUpdate == true) {
                    if (strings.isBangla) "নতুন" else "NEW"
                } else {
                    "v${versionInfo.versionName}"
                },
                onClick = onOpenAppUpdate,
                testTag = "settings_row_app_update"
            )

            // 4. অ্যাপ সম্পর্কে (About) Row
            SettingsClickableRowCard(
                icon = Icons.Default.Info,
                title = strings.settingsAboutRowTitle,
                description = strings.settingsAboutRowDesc,
                currentValueBadge = "MBD 🩸",
                onClick = onOpenAbout,
                testTag = "settings_row_about"
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Section Header: Notifications
            Text(
                text = if (strings.isBangla) "নোটিফিকেশন সেটিংস" else "Notifications",
                color = MbdTextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // Emergency Blood Request Notification Row (Preserved from reference screenshot)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdBorderGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val nextState = !notifPermGranted
                        if (nextState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !MbdNotificationHelper.hasNotificationPermission(context)
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notifPermGranted = nextState
                            MbdPreferencesManager.setNotificationsEnabled(context, nextState)
                        }
                    }
                    .testTag("settings_row_notifications")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MbdCrimsonLight,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MbdCrimson,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (strings.isBangla) {
                                "জরুরি রক্তের আবেদনের নোটিফিকেশন"
                            } else {
                                "Emergency Blood Request Notifications"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MbdTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (notifPermGranted) {
                                if (strings.isBangla) "নোটিফিকেশন সক্রিয় আছে" else "Notifications enabled"
                            } else {
                                if (strings.isBangla) "নোটিফিকেশন বন্ধ বা অনুমতি প্রয়োজন" else "Notifications muted or permission required"
                            },
                            fontSize = 12.sp,
                            color = MbdTextSecondary
                        )
                    }
                    Switch(
                        checked = notifPermGranted,
                        onCheckedChange = { checked ->
                            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !MbdNotificationHelper.hasNotificationPermission(context)
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notifPermGranted = checked
                                MbdPreferencesManager.setNotificationsEnabled(context, checked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MbdCrimson
                        ),
                        modifier = Modifier.testTag("settings_notifications_switch")
                    )
                }
            }
        }
    }

    // 3. Language Selection Popup ("ভাষা নির্বাচন করুন")
    if (showLanguagePopup) {
        LanguageSelectionPopup(
            strings = strings,
            currentLanguage = currentLanguage,
            onSelectAndApplyLanguage = { selectedLang ->
                MbdPreferencesManager.setLanguage(context, selectedLang, isExplicitUserChoice = true)
                onSelectLanguage(selectedLang)
                showLanguagePopup = false
            },
            onDismiss = { showLanguagePopup = false }
        )
    }

    // 4. Theme Selection Popup ("থিম নির্বাচন করুন")
    if (showThemePopup) {
        ThemeSelectionPopup(
            strings = strings,
            currentThemeMode = currentThemeMode,
            onSelectAndApplyTheme = { selectedMode ->
                MbdPreferencesManager.setThemeMode(context, selectedMode)
                onSelectThemeMode(selectedMode)
                showThemePopup = false
            },
            onDismiss = { showThemePopup = false }
        )
    }
}

@Composable
private fun SettingsClickableRowCard(
    icon: ImageVector,
    title: String,
    description: String,
    currentValueBadge: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MbdSurface),
        border = BorderStroke(1.dp, MbdBorderGray),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MbdCrimsonLight,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MbdCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MbdTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MbdTextSecondary,
                    lineHeight = 16.sp
                )
                if (currentValueBadge.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MbdCrimsonLight.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = currentValueBadge,
                            color = MbdCrimson,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MbdTextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Language Selection Popup ("ভাষা নির্বাচন করুন")
 *
 * Options:
 * - বাংলা (Bangla)
 * - English
 */
@Composable
fun LanguageSelectionPopup(
    strings: MbdStrings,
    currentLanguage: String,
    onSelectAndApplyLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingSelection by remember(currentLanguage) {
        mutableStateOf(if (currentLanguage == "en") "en" else "bn")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MbdSurface,
        titleContentColor = MbdTextPrimary,
        textContentColor = MbdTextPrimary,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("language_selection_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MbdCrimsonLight,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MbdCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (strings.isBangla) "ভাষা নির্বাচন করুন" else "Select Language",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MbdTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option 1: বাংলা (Bangla)
                val isBnSelected = pendingSelection == "bn"
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isBnSelected) MbdCrimsonLight.copy(alpha = 0.72f) else MbdChipUnselected,
                    border = BorderStroke(
                        width = if (isBnSelected) 1.5.dp else 1.dp,
                        color = if (isBnSelected) MbdCrimson else MbdBorderGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            pendingSelection = "bn"
                            onSelectAndApplyLanguage("bn")
                        }
                        .testTag("language_option_bn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isBnSelected,
                            onClick = {
                                pendingSelection = "bn"
                                onSelectAndApplyLanguage("bn")
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MbdCrimson,
                                unselectedColor = MbdTextSecondary
                            ),
                            modifier = Modifier.testTag("settings_lang_bn")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "বাংলা (Bangla)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isBnSelected) MbdCrimson else MbdTextPrimary
                            )
                            Text(
                                text = "ডিফল্ট ভাষা • সম্পূর্ণ বাংলা ইন্টারফেস",
                                fontSize = 11.sp,
                                color = MbdTextSecondary
                            )
                        }
                        if (isBnSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MbdCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Option 2: English
                val isEnSelected = pendingSelection == "en"
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isEnSelected) MbdCrimsonLight.copy(alpha = 0.72f) else MbdChipUnselected,
                    border = BorderStroke(
                        width = if (isEnSelected) 1.5.dp else 1.dp,
                        color = if (isEnSelected) MbdCrimson else MbdBorderGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            pendingSelection = "en"
                            onSelectAndApplyLanguage("en")
                        }
                        .testTag("language_option_en")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isEnSelected,
                            onClick = {
                                pendingSelection = "en"
                                onSelectAndApplyLanguage("en")
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MbdCrimson,
                                unselectedColor = MbdTextSecondary
                            ),
                            modifier = Modifier.testTag("settings_lang_en")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "English",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isEnSelected) MbdCrimson else MbdTextPrimary
                            )
                            Text(
                                text = "Full English interface across all screens",
                                fontSize = 11.sp,
                                color = MbdTextSecondary
                            )
                        }
                        if (isEnSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MbdCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectAndApplyLanguage(pendingSelection) },
                colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("language_popup_apply_button")
            ) {
                Text(
                    text = strings.settingsApplyBtn,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("language_popup_cancel_button")
            ) {
                Text(text = strings.settingsCancelBtn, color = MbdTextSecondary)
            }
        }
    )
}

/**
 * Theme Selection Popup ("থিম নির্বাচন করুন")
 *
 * Options:
 * A. Dark / Black (ডার্ক)
 * B. White / Light (হোয়াইট / লাইট) [Default]
 * C. System Default (সিস্টেম ডিফল্ট)
 */
@Composable
fun ThemeSelectionPopup(
    strings: MbdStrings,
    currentThemeMode: MbdThemeMode,
    onSelectAndApplyTheme: (MbdThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingMode by remember(currentThemeMode) { mutableStateOf(currentThemeMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MbdSurface,
        titleContentColor = MbdTextPrimary,
        textContentColor = MbdTextPrimary,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("theme_selection_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MbdCrimsonLight,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MbdCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (strings.isBangla) "থিম নির্বাচন করুন" else "Select Theme",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MbdTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option A: Dark / Black (ডার্ক)
                ThemeOptionRow(
                    mode = MbdThemeMode.DARK,
                    selected = pendingMode == MbdThemeMode.DARK,
                    icon = Icons.Default.DarkMode,
                    title = strings.settingsThemeOptionDark,
                    description = strings.settingsThemeOptionDarkDesc,
                    onClick = {
                        pendingMode = MbdThemeMode.DARK
                        onSelectAndApplyTheme(MbdThemeMode.DARK)
                    },
                    testTag = "theme_option_dark"
                )

                // Option B: White / Light (হোয়াইট / লাইট) — Default
                ThemeOptionRow(
                    mode = MbdThemeMode.LIGHT,
                    selected = pendingMode == MbdThemeMode.LIGHT,
                    icon = Icons.Default.LightMode,
                    title = strings.settingsThemeOptionLight,
                    description = strings.settingsThemeOptionLightDesc,
                    onClick = {
                        pendingMode = MbdThemeMode.LIGHT
                        onSelectAndApplyTheme(MbdThemeMode.LIGHT)
                    },
                    testTag = "theme_option_light"
                )

                // Option C: System Default (সিস্টেম ডিফল্ট)
                ThemeOptionRow(
                    mode = MbdThemeMode.SYSTEM,
                    selected = pendingMode == MbdThemeMode.SYSTEM,
                    icon = Icons.Default.SettingsBrightness,
                    title = strings.settingsThemeOptionSystem,
                    description = strings.settingsThemeOptionSystemDesc,
                    onClick = {
                        pendingMode = MbdThemeMode.SYSTEM
                        onSelectAndApplyTheme(MbdThemeMode.SYSTEM)
                    },
                    testTag = "theme_option_system"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectAndApplyTheme(pendingMode) },
                colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("theme_popup_apply_button")
            ) {
                Text(
                    text = strings.settingsApplyBtn,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("theme_popup_cancel_button")
            ) {
                Text(text = strings.settingsCancelBtn, color = MbdTextSecondary)
            }
        }
    )
}

@Composable
private fun ThemeOptionRow(
    mode: MbdThemeMode,
    selected: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MbdCrimsonLight.copy(alpha = 0.72f) else MbdChipUnselected,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MbdCrimson else MbdBorderGray
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MbdCrimson,
                    unselectedColor = MbdTextSecondary
                ),
                modifier = Modifier.testTag("${testTag}_radio")
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MbdCrimson else MbdTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (selected) MbdCrimson else MbdTextPrimary
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MbdTextSecondary,
                    lineHeight = 15.sp
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MbdCrimson,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Dedicated App Update Screen ("অ্যাপ আপডেট")
 *
 * Displays:
 * - Current installed app version
 * - Latest available version (when verified)
 * - Update availability status ("আপনার অ্যাপটি সর্বশেষ সংস্করণে আছে")
 * - "Check for Updates" button
 * - "Update Now" button only when a verified update is available from a real configured source
 * - Truthful explanation if no external update endpoint/store listing is configured yet
 * - Error + Retry handling if an update check fails
 */
@Composable
fun AppUpdateScreen(
    strings: MbdStrings,
    updateInfo: com.example.util.UpdateInfo? = null,
    downloadState: com.example.util.DownloadState = com.example.util.DownloadState.Idle,
    onCheckForUpdates: () -> Unit = {},
    onStartDownload: (String) -> Unit = {},
    onResetDownload: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val installedInfo = remember(context) { getInstalledAppVersionInfo(context) }

    var isCheckingLocally by remember { mutableStateOf(false) }
    
    val isVerifiedUpdateAvailable = updateInfo != null && updateInfo.requiresUpdate

    val resolvedLatestVersion = updateInfo?.latestVersionName ?: installedInfo.versionName

    val lastCheckedEpochMs = remember { System.currentTimeMillis() }
    val formattedCheckTime = remember(lastCheckedEpochMs) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastCheckedEpochMs))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("app_update_screen_root")
    ) {
        // Red Header with Back Button returning to Settings
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
                    modifier = Modifier.testTag("app_update_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = strings.updateScreenTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
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
            // Hero Status Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdBorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (downloadState is com.example.util.DownloadState.Error) {
                            MbdWarningAmber.copy(alpha = 0.15f)
                        } else if (isVerifiedUpdateAvailable) {
                            MbdCrimsonLight
                        } else {
                            MbdAvailableGreen.copy(alpha = 0.14f)
                        },
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCheckingLocally || downloadState is com.example.util.DownloadState.Downloading) {
                                CircularProgressIndicator(
                                    color = MbdCrimson,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = when {
                                        downloadState is com.example.util.DownloadState.Error -> Icons.Default.ErrorOutline
                                        isVerifiedUpdateAvailable -> Icons.Default.CloudDownload
                                        else -> Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        downloadState is com.example.util.DownloadState.Error -> MbdWarningAmber
                                        isVerifiedUpdateAvailable -> MbdCrimson
                                        else -> MbdAvailableGreen
                                    },
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Mirpur Blood Donation (MBD 🩸)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MbdTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    // Status Message Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isVerifiedUpdateAvailable) {
                            MbdCrimsonLight
                        } else {
                            MbdAvailableGreen.copy(alpha = 0.12f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_status_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isVerifiedUpdateAvailable) {
                                    if (strings.isBangla) {
                                        "নতুন সংস্করণ (v$resolvedLatestVersion) পাওয়া গেছে!"
                                    } else {
                                        "New verified update (v$resolvedLatestVersion) is available!"
                                    }
                                } else {
                                    "আপনার অ্যাপটি সর্বশেষ সংস্করণে আছে"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isVerifiedUpdateAvailable) MbdCrimson else MbdAvailableGreen,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("update_up_to_date_text")
                            )
                            if (!strings.isBangla && !isVerifiedUpdateAvailable) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Your app is on the latest installed version",
                                    fontSize = 12.sp,
                                    color = MbdTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Version Details Card
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
                    UpdateDetailRow(
                        label = strings.updateCurrentVersionLabel,
                        value = "v${installedInfo.versionName} (Build ${installedInfo.versionCode})",
                        valueTag = "installed_version_value"
                    )
                    HorizontalDivider(color = MbdBorderGray)
                    UpdateDetailRow(
                        label = strings.updateLatestVersionLabel,
                        value = if (updateInfo != null) {
                            "v$resolvedLatestVersion"
                        } else {
                            "v${installedInfo.versionName}"
                        },
                        valueTag = "latest_version_value"
                    )
                    HorizontalDivider(color = MbdBorderGray)
                    UpdateDetailRow(
                        label = strings.updateStatusLabel,
                        value = if (isVerifiedUpdateAvailable) {
                            if (strings.isBangla) "আপডেট উপলব্ধ (Update Available)" else "Update Available"
                        } else {
                            if (strings.isBangla) "সর্বশেষ সংস্করণ (Up to Date)" else "Up to Date (সর্বশেষ সংস্করণ)"
                        },
                        valueTag = "update_availability_status_value"
                    )
                    HorizontalDivider(color = MbdBorderGray)
                    UpdateDetailRow(
                        label = if (strings.isBangla) "সর্বশেষ যাচাইয়ের সময়" else "Last Checked",
                        value = formattedCheckTime,
                        valueTag = "update_last_checked_value"
                    )
                }
            }

            // Update Source Configuration Notice
            if (updateInfo == null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MbdSurface),
                    border = BorderStroke(1.dp, MbdBorderGray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_endpoint_info_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MbdCrimson,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (strings.isBangla) {
                                    "আপডেট চেক করা হচ্ছে..."
                                } else {
                                    "Checking for updates..."
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MbdTextPrimary
                            )
                            Text(
                                text = if (strings.isBangla) {
                                    "সার্ভার থেকে আপডেটের তথ্য সংগ্রহ করা হচ্ছে। অনুগ্রহ করে অপেক্ষা করুন।"
                                } else {
                                    "Fetching update information from GitHub. Please wait."
                                },
                                fontSize = 13.sp,
                                color = MbdTextSecondary,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            // Release Notes Card if a verified update is available
            val releaseNotes = if (strings.isBangla) updateInfo?.releaseNotesBn else updateInfo?.releaseNotesEn
            if (isVerifiedUpdateAvailable && !releaseNotes.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MbdSurface),
                    border = BorderStroke(1.dp, MbdCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (strings.isBangla) "নতুন সংস্করণের পরিবর্তনসমূহ (Release Notes)" else "Release Notes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MbdCrimson
                        )
                        Text(
                            text = releaseNotes,
                            fontSize = 13.sp,
                            color = MbdTextPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // Error & Retry Banner if an update check encountered an error
            if (downloadState is com.example.util.DownloadState.Error) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MbdCrimsonLight),
                    border = BorderStroke(1.dp, MbdCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_error_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = downloadState.message,
                            color = MbdCrimson,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = onResetDownload,
                            border = BorderStroke(1.dp, MbdCrimson),
                            modifier = Modifier.testTag("retry_update_check_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MbdCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (strings.isBangla) "আবার চেষ্টা করুন" else "Retry", color = MbdCrimson)
                        }
                    }
                }
            }

            // Action Buttons: "Check for Updates" & (when verified) "Update Now"
            if (downloadState is com.example.util.DownloadState.Downloading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { downloadState.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MbdCrimson,
                        trackColor = MbdCrimsonLight
                    )
                    Text(
                        text = if (strings.isBangla) "ডাউনলোড হচ্ছে: ${downloadState.progressPercent}%" else "Downloading: ${downloadState.progressPercent}%",
                        fontSize = 12.sp,
                        color = MbdTextSecondary
                    )
                }
            } else {
                Button(
                    onClick = onCheckForUpdates,
                    enabled = !isCheckingLocally,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("check_for_updates_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.updateCheckBtn,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (isVerifiedUpdateAvailable && updateInfo != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onStartDownload(updateInfo.downloadUrl) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MbdAvailableGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("update_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.updateNowBtn,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateDetailRow(
    label: String,
    value: String,
    valueTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MbdTextSecondary,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MbdTextPrimary,
            modifier = Modifier.testTag(valueTag)
        )
    }
}

/**
 * Full Scrollable About Page ("অ্যাপ সম্পর্কে")
 *
 * Includes:
 * - MBD 🩸 app icon and app name
 * - Full name: Mirpur Blood Donation
 * - App version
 * - Short description of the app
 * - Mission: Connecting blood donors with people who need blood
 * - Developer & Contact info (truthful; marked as unconfigured placeholder only in debug builds, never fake data)
 * - Privacy Policy
 * - Terms and Conditions
 * - Open-source licenses
 * - Copyright information
 */
@Composable
fun AboutContactPrivacyScreen(
    strings: MbdStrings,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val versionInfo = remember(context) { getInstalledAppVersionInfo(context) }

    var activeLegalDialog by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("about_screen_root")
    ) {
        // Red Header with Back Button returning to Settings
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
                    modifier = Modifier.testTag("about_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = strings.aboutScreenTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
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
            // 1. App Icon, Short Name ("MBD 🩸"), Full Name ("Mirpur Blood Donation"), and App Version Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                border = BorderStroke(1.dp, MbdBorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // MBD 🩸 App Icon Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MbdCrimson,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "MBD App Icon",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "MBD 🩸",
                        color = MbdCrimson,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )

                    Text(
                        text = "Mirpur Blood Donation",
                        color = MbdTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MbdCrimsonLight
                    ) {
                        Text(
                            text = if (strings.isBangla) {
                                "সংস্করণ (Version): v${versionInfo.versionName} (Build ${versionInfo.versionCode})"
                            } else {
                                "Version v${versionInfo.versionName} (Build ${versionInfo.versionCode})"
                            },
                            color = MbdCrimson,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                                .testTag("about_app_version_badge")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = strings.aboutShortDescription,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MbdTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Mission Card ("Connecting blood donors with people who need blood")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MbdCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.aboutMissionTitle,
                            color = MbdCrimson,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Connecting blood donors with people who need blood",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MbdTextPrimary
                    )
                    Text(
                        text = "রক্তদাতাদের সাথে রক্তের প্রয়োজন এমন মানুষের দ্রুত ও নিরাপদ সংযোগ স্থাপন করা।",
                        fontSize = 13.sp,
                        color = MbdTextSecondary,
                        lineHeight = 19.sp
                    )
                }
            }

            // 3. Developer & Contact Information (Truthful — no fake data; placeholder only in debug build)
            if (BuildConfig.DEBUG) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MbdSurface),
                    border = BorderStroke(1.dp, MbdBorderGray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_dev_contact_placeholder_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = MbdTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (strings.isBangla) {
                                    "ডেভেলপার ও যোগাযোগের তথ্য (Development Placeholder)"
                                } else {
                                    "Developer & Contact Info (Development Placeholder)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MbdTextPrimary
                            )
                        }
                        Text(
                            text = if (strings.isBangla) {
                                "ডেভেলপমেন্ট বিল্ড নোট: অফিশিয়াল ডেভেলপার প্রোফাইল, হেল্পলাইন নম্বর, ইমেইল বা ওয়েবসাইট লিংক এখনো কনফিগার করা হয়নি।"
                            } else {
                                "Development Build Placeholder: Official developer contact details (phone, email, website) have not been configured yet."
                            },
                            fontSize = 12.sp,
                            color = MbdTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 4. Privacy Policy Section & Interactive Viewer
            AboutLegalSectionCard(
                icon = Icons.Default.Security,
                title = if (strings.isBangla) "গোপনীয়তা নীতি (Privacy Policy)" else "Privacy Policy (গোপনীয়তা নীতি)",
                summary = if (strings.isBangla) {
                    "রক্তদাতার ফোন নম্বর, এলাকা এবং সাময়িক লাইভ লোকেশন শুধুমাত্র রক্তদাতা ও আবেদনকারীর অনুমতিক্রমে প্রদর্শিত হয়। কোনো তথ্য বাণিজ্যিক উদ্দেশ্যে শেয়ার করা হয় না।"
                } else {
                    "Donor phone numbers, area details, and temporary live location sessions are strictly controlled by explicit user privacy settings and never sold or shared commercially."
                },
                onViewFullClick = { activeLegalDialog = "privacy" },
                testTag = "about_privacy_policy_row"
            )

            // 5. Terms and Conditions Section & Interactive Viewer
            AboutLegalSectionCard(
                icon = Icons.Default.Description,
                title = strings.aboutTermsTitle,
                summary = if (strings.isBangla) {
                    "MBD একটি সম্পূর্ণ স্বেচ্ছাসেবী ও অলাভজনক রক্তদান প্ল্যাটফর্ম। রক্তের বিনিময়ে কোনো আর্থিক লেনদেন সম্পূর্ণ নিষিদ্ধ এবং রক্তদানের পূর্বে চিকিৎসকের পরামর্শ ও ক্রস-ম্যাচিং বাধ্যতামূলক।"
                } else {
                    "MBD is a strictly voluntary, non-commercial blood donation community. Financial transactions for blood are prohibited, and medical screening at the hospital is mandatory prior to donation."
                },
                onViewFullClick = { activeLegalDialog = "terms" },
                testTag = "about_terms_conditions_row"
            )

            // 6. Open-Source Licenses Section & Interactive Viewer
            AboutLegalSectionCard(
                icon = Icons.Default.Code,
                title = strings.aboutLicensesTitle,
                summary = "AndroidX Jetpack Compose, Material Design 3, Kotlin Coroutines, Firebase Android SDK, Google Play Services Location & AndroidX Credentials (Apache License 2.0).",
                onViewFullClick = { activeLegalDialog = "licenses" },
                testTag = "about_open_source_licenses_row"
            )

            // 7. Copyright Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.aboutCopyrightText,
                    fontSize = 12.sp,
                    color = MbdTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("about_copyright_text")
                )
            }
        }
    }

    if (activeLegalDialog != null) {
        val (dialogTitle, dialogBody) = when (activeLegalDialog) {
            "privacy" -> {
                (if (strings.isBangla) "গোপনীয়তা নীতি (Privacy Policy)" else "Privacy Policy") to
                    (if (strings.isBangla) {
                        "১. ব্যক্তিগত তথ্যের সুরক্ষা: রক্তদাতা নিজের প্রোফাইল থেকে ফোন নম্বর, এলাকা এবং রক্তদানের ইতিহাস পাবলিক বা প্রাইভেট রাখতে পারেন।\n\n" +
                            "২. লাইভ লোকেশন গোপনীয়তা: লাইভ লোকেশন শুধুমাত্র আবেদনকারীর স্পষ্ট সম্মতিতে নির্দিষ্ট রক্তদাতার সাথে সাময়িক সময়ের জন্য শেয়ার হয় এবং মেয়াদ শেষে স্বয়ংক্রিয়ভাবে বন্ধ হয়ে যায়।\n\n" +
                            "৩. ডেটা নিরাপত্তা: সকল প্রোফাইল ও রক্তদানের রেকর্ড ফায়ারবেস সিকিউরিটি রুলস দ্বারা সুরক্ষিত থাকে।"
                    } else {
                        "1. Personal Data Control: Donors control whether their phone number, area, and donation history are visible on their public profile.\n\n" +
                            "2. Live Location Privacy: Real-time location is shared only with explicit consent for a bounded session and automatically expires.\n\n" +
                            "3. Data Protection: All donor and request records are protected by strict role-based security rules."
                    })
            }
            "terms" -> {
                strings.aboutTermsTitle to
                    (if (strings.isBangla) {
                        "১. স্বেচ্ছাসেবী রক্তদান: MBD প্ল্যাটফর্মে সকল রক্তদান সম্পূর্ণ স্বেচ্ছাসেবী ও বিনামূল্যে পরিচালিত হয়।\n\n" +
                            "২. চিকিৎসাগত যোগ্যতা: কাউন্টডাউন টাইমার শুধুমাত্র সম্ভাব্য সময়ের হিসাব দেয়। চূড়ান্ত রক্তদানের পূর্বে হাসপাতালের চিকিৎসক কর্তৃক হিমোগ্লোবিন ও স্বাস্থ্য পরীক্ষা বাধ্যতামূলক।\n\n" +
                            "৩. সঠিক তথ্য প্রদান: ভুয়া রক্তের আবেদন বা ভুল রক্তদানের রেকর্ড প্রদান করলে অ্যাকাউন্ট সীমিত করা হতে পারে।"
                    } else {
                        "1. Voluntary Service: All blood donations coordinated through MBD are strictly voluntary and free of charge.\n\n" +
                            "2. Medical Assessment: The countdown timer provides an estimated interval only. Final eligibility is determined by medical professionals at the donation center.\n\n" +
                            "3. Authentic Records: Users must provide accurate request and donation details."
                    })
            }
            else -> {
                strings.aboutLicensesTitle to
                    "This application uses the following open-source software licensed under the Apache License, Version 2.0:\n\n" +
                    "• AndroidX Jetpack Compose & Material 3 (Copyright © Google LLC)\n" +
                    "• Kotlin Standard Library & Coroutines (Copyright © JetBrains s.r.o.)\n" +
                    "• Firebase Firestore & Authentication Android SDK (Copyright © Google LLC)\n" +
                    "• AndroidX Credentials & Google Identity Services (Copyright © Google LLC)\n" +
                    "• Google Play Services Location (Copyright © Google LLC)"
            }
        }

        AlertDialog(
            onDismissRequest = { activeLegalDialog = null },
            containerColor = MbdSurface,
            titleContentColor = MbdTextPrimary,
            textContentColor = MbdTextPrimary,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MbdCrimson
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = dialogBody,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MbdTextPrimary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeLegalDialog = null }) {
                    Text(
                        text = strings.settingsCancelBtn,
                        color = MbdCrimson,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun AboutLegalSectionCard(
    icon: ImageVector,
    title: String,
    summary: String,
    onViewFullClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MbdSurface),
        border = BorderStroke(1.dp, MbdBorderGray),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onViewFullClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MbdCrimsonLight,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MbdCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MbdTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MbdTextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MbdTextSecondary
            )
        }
    }
}

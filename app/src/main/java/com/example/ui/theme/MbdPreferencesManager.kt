package com.example.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported MBD App Theme Modes:
 * - [DARK]: Dark / Black (ডার্ক) — Dark background, light text, readable contrast, red primary accent.
 * - [LIGHT]: White / Light — White background, dark text, existing MBD red-and-white design (Default).
 * - [SYSTEM]: System Default — Automatically follows Android device's light or dark appearance setting.
 */
enum class MbdThemeMode(val storageKey: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system");

    companion object {
        fun fromStorageKey(key: String?): MbdThemeMode {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: LIGHT
        }
    }
}

/**
 * Snapshot-backed theme state used by theme-aware MBD color tokens in [Color.kt] and [Theme.kt].
 */
object MbdThemeState {
    var isDarkEffective by mutableStateOf(false)
}

/**
 * Persists and exposes user preferences for Language ("bn" default, "en") and Theme (LIGHT default, DARK, SYSTEM)
 * across app restarts using SharedPreferences.
 */
object MbdPreferencesManager {
    private const val PREFS_NAME = "mbd_settings_prefs"
    private const val KEY_LANGUAGE = "pref_language"
    private const val KEY_LANGUAGE_EXPLICIT = "pref_language_explicit"
    private const val KEY_THEME_MODE = "pref_theme_mode"
    private const val KEY_NOTIFICATIONS_ENABLED = "pref_notifications_enabled"

    private val _languageFlow = MutableStateFlow("bn")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(MbdThemeMode.LIGHT)
    val themeModeFlow: StateFlow<MbdThemeMode> = _themeModeFlow.asStateFlow()

    private val _notificationsEnabledFlow = MutableStateFlow(true)
    val notificationsEnabledFlow: StateFlow<Boolean> = _notificationsEnabledFlow.asStateFlow()

    @Volatile
    private var isInitialized = false

    @Volatile
    var hasExplicitLanguagePreference: Boolean = false
        private set

    fun init(context: Context) {
        val appContext = context.applicationContext ?: context
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs.getString(KEY_LANGUAGE, "bn") ?: "bn"
        val normalizedLang = if (savedLang == "en") "en" else "bn"
        hasExplicitLanguagePreference = prefs.getBoolean(KEY_LANGUAGE_EXPLICIT, false)

        val savedThemeKey = prefs.getString(KEY_THEME_MODE, MbdThemeMode.LIGHT.storageKey)
        val resolvedTheme = MbdThemeMode.fromStorageKey(savedThemeKey)

        val savedNotif = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

        _languageFlow.value = normalizedLang
        _themeModeFlow.value = resolvedTheme
        _notificationsEnabledFlow.value = savedNotif

        val isSystemDark = (appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        MbdThemeState.isDarkEffective = when (resolvedTheme) {
            MbdThemeMode.DARK -> true
            MbdThemeMode.LIGHT -> false
            MbdThemeMode.SYSTEM -> isSystemDark
        }
        isInitialized = true
    }

    fun ensureInitialized(context: Context) {
        if (!isInitialized) {
            init(context)
        }
    }

    fun getLanguage(context: Context? = null): String {
        if (context != null) {
            val prefs = (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_LANGUAGE, _languageFlow.value) ?: "bn"
            val normalized = if (saved == "en") "en" else "bn"
            _languageFlow.value = normalized
            return normalized
        }
        return _languageFlow.value
    }

    fun setLanguage(context: Context?, language: String, isExplicitUserChoice: Boolean = true) {
        val normalized = if (language == "en") "en" else "bn"
        _languageFlow.value = normalized
        if (isExplicitUserChoice) {
            hasExplicitLanguagePreference = true
        }
        if (context != null) {
            val prefs = (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_LANGUAGE, normalized)
                .putBoolean(KEY_LANGUAGE_EXPLICIT, hasExplicitLanguagePreference)
                .apply()
        }
    }

    fun getThemeMode(context: Context? = null): MbdThemeMode {
        if (context != null) {
            val prefs = (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = prefs.getString(KEY_THEME_MODE, _themeModeFlow.value.storageKey)
            val mode = MbdThemeMode.fromStorageKey(key)
            _themeModeFlow.value = mode
            return mode
        }
        return _themeModeFlow.value
    }

    fun setThemeMode(context: Context?, mode: MbdThemeMode) {
        _themeModeFlow.value = mode
        val isSystemDark = if (context != null) {
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        } else {
            false
        }
        MbdThemeState.isDarkEffective = when (mode) {
            MbdThemeMode.DARK -> true
            MbdThemeMode.LIGHT -> false
            MbdThemeMode.SYSTEM -> isSystemDark
        }
        if (context != null) {
            val prefs = (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_THEME_MODE, mode.storageKey)
                .apply()
        }
    }

    fun isNotificationsEnabled(context: Context? = null): Boolean {
        if (context != null) {
            val prefs = (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, _notificationsEnabledFlow.value)
            _notificationsEnabledFlow.value = saved
            return saved
        }
        return _notificationsEnabledFlow.value
    }

    fun setNotificationsEnabled(context: Context?, enabled: Boolean) {
        _notificationsEnabledFlow.value = enabled
        if (context != null) {
            val prefs = (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
                .apply()
        }
    }

    fun resetToDefaults(context: Context? = null) {
        hasExplicitLanguagePreference = false
        _languageFlow.value = "bn"
        _themeModeFlow.value = MbdThemeMode.LIGHT
        _notificationsEnabledFlow.value = true
        MbdThemeState.isDarkEffective = false
        if (context != null) {
            val prefs = (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        }
    }
}

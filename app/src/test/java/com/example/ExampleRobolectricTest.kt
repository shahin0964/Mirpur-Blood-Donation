package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.ui.i18n.getMbdStrings
import com.example.ui.theme.MbdBackground
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdPreferencesManager
import com.example.ui.theme.MbdSurface
import com.example.ui.theme.MbdTextPrimary
import com.example.ui.theme.MbdThemeMode
import com.example.ui.theme.MbdThemeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and settings strings from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("MBD", context.getString(R.string.app_name))
        assertEquals("Mirpur Blood Donation", context.getString(R.string.app_full_name))
        assertEquals("MBD 🩸", context.getString(R.string.app_short_name))
    }

    @Test
    fun `preferences manager persists language theme and notifications with Bangla and Light defaults`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MbdPreferencesManager.resetToDefaults(context)

        assertEquals("bn", MbdPreferencesManager.getLanguage(context))
        assertEquals(MbdThemeMode.LIGHT, MbdPreferencesManager.getThemeMode(context))
        assertTrue(MbdPreferencesManager.notificationsEnabledFlow.value)

        // Switch language to English and verify persistence
        MbdPreferencesManager.setLanguage(context, "en")
        assertEquals("en", MbdPreferencesManager.getLanguage(context))
        assertEquals("en", MbdPreferencesManager.languageFlow.value)

        // Switch theme to Dark and verify persistence
        MbdPreferencesManager.setThemeMode(context, MbdThemeMode.DARK)
        assertEquals(MbdThemeMode.DARK, MbdPreferencesManager.getThemeMode(context))
        assertEquals(MbdThemeMode.DARK, MbdPreferencesManager.themeModeFlow.value)

        // Switch theme to System Default and verify persistence
        MbdPreferencesManager.setThemeMode(context, MbdThemeMode.SYSTEM)
        assertEquals(MbdThemeMode.SYSTEM, MbdPreferencesManager.getThemeMode(context))

        // Toggle notifications off and verify persistence
        MbdPreferencesManager.setNotificationsEnabled(context, false)
        assertFalse(MbdPreferencesManager.notificationsEnabledFlow.value)

        // Restore defaults
        MbdPreferencesManager.resetToDefaults(context)
    }

    @Test
    fun `theme state dynamically switches surface and text colors while preserving red accent`() {
        MbdThemeState.isDarkEffective = false
        assertEquals(Color(0xFFFFFFFF), MbdSurface)
        assertEquals(Color(0xFFF6F7FB), MbdBackground)
        assertEquals(Color(0xFF181C24), MbdTextPrimary)
        assertEquals(Color(0xFFD31018), MbdCrimson)

        MbdThemeState.isDarkEffective = true
        assertEquals(Color(0xFF1A1D24), MbdSurface)
        assertEquals(Color(0xFF0F1115), MbdBackground)
        assertEquals(Color(0xFFF3F4F6), MbdTextPrimary)
        assertEquals(Color(0xFFD31018), MbdCrimson)

        MbdThemeState.isDarkEffective = false
    }

    @Test
    fun `settings language theme app update and about strings match Bangla and English specifications`() {
        val bn = getMbdStrings("bn")
        val en = getMbdStrings("en")

        // 4 required settings rows in Bangla
        assertEquals("ভাষা নির্বাচন", bn.settingsLanguageRowTitle)
        assertEquals("থিম", bn.settingsThemeRowTitle)
        assertEquals("অ্যাপ আপডেট", bn.settingsAppUpdateRowTitle)
        assertEquals("অ্যাপ সম্পর্কে", bn.settingsAboutRowTitle)

        // Popup titles and options
        assertEquals("ভাষা নির্বাচন করুন", bn.settingsLanguageDialogTitle)
        assertEquals("থিম নির্বাচন করুন", bn.settingsThemeDialogTitle)
        assertTrue(bn.settingsThemeOptionDark.contains("ডার্ক"))
        assertTrue(en.settingsThemeOptionDark.contains("Dark / Black"))

        // App Update screen status message
        assertEquals("আপনার অ্যাপটি সর্বশেষ সংস্করণে আছে", bn.updateUpToDateMessage)
        assertTrue(en.updateUpToDateMessage.contains("আপনার অ্যাপটি সর্বশেষ সংস্করণে আছে"))

        // About screen mission and copyright
        assertTrue(bn.aboutMissionStatement.contains("Connecting blood donors with people who need blood"))
        assertTrue(bn.aboutCopyrightText.contains("Mirpur Blood Donation"))
    }
}

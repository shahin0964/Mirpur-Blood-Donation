package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Crimson Palette matching Reference Design (Preserved as primary accent across Light & Dark themes)
val MbdCrimson = Color(0xFFD31018)
val MbdCrimsonDark = Color(0xFFA50A10)

val MbdCrimsonLight: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF3E1418) else Color(0xFFFFE8E9)

val MbdBannerPinkStart: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF2B1215) else Color(0xFFFFF0F0)

val MbdBannerPinkEnd: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF38161B) else Color(0xFFFFE2E4)

val MbdSoftPinkAlert: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF2E1518) else Color(0xFFFDECEC)

// Action & Status Colors
val MbdCallGreen = Color(0xFF1B9E4B)
val MbdAvailableGreen = Color(0xFF16A34A)
val MbdWarningAmber = Color(0xFFD97706)

// Neutral Surfaces & Typography Colors (Automatically adapt to Light and Dark themes)
val MbdBackground: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF0F1115) else Color(0xFFF6F7FB)

val MbdSurface: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF1A1D24) else Color(0xFFFFFFFF)

val MbdChipUnselected: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF262B36) else Color(0xFFEEF1F6)

val MbdBorderGray: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF2E3440) else Color(0xFFE2E6EE)

val MbdTextPrimary: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFFF3F4F6) else Color(0xFF181C24)

val MbdTextSecondary: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFFA3AAB8) else Color(0xFF5E6573)

val MbdTextHint: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF727A8A) else Color(0xFF949BA8)

// Dark-mode aware surface and container helpers
val MbdSubtleSurface: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF232832) else Color(0xFFF8FAFC)

val MbdWarningCardBg: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF332B12) else Color(0xFFFFF8E1)

val MbdWarningCardBorder: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF8A6B00) else Color(0xFFFFB300)

val MbdWarningCardText: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFFFFD54F) else Color(0xFFBF360C)

val MbdWarningCardIcon: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFFFFB74D) else Color(0xFFE65100)

val MbdInfoCardBg: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF3B181E) else Color(0xFFFDF2F4)

val MbdPendingCardBg: Color
    get() = if (MbdThemeState.isDarkEffective) Color(0xFF332A15) else Color(0xFFFFFBF0)


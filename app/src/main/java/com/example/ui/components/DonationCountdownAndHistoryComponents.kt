package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DEFAULT_ELIGIBILITY_RULES
import com.example.data.model.DONATION_TYPE_OPTIONS
import com.example.data.model.DonationRecord
import com.example.data.model.DonorProfile
import com.example.data.model.EligibilityRule
import com.example.ui.i18n.MbdStrings
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DonationTypeCountsSummary(
    val wholeBloodCount: Long = 0L,
    val plateletsCount: Long = 0L,
    val plasmaCount: Long = 0L,
    val otherDonationCount: Long = 0L,
    val totalConfirmed: Long = 0L
)

data class MultiRecordEligibilityResult(
    val hasVerifiedRecords: Boolean,
    val isRuleUndecided: Boolean,
    val lastDonationDate: String,
    val lastDonationTime: String,
    val lastDonationType: String,
    val customDonationTypeName: String,
    val governingIntervalDays: Long,
    val governingRuleVersion: String,
    val nextEligibleDate: String,
    val governingExplanationBn: String,
    val governingExplanationEn: String,
    val countsByType: DonationTypeCountsSummary
)

data class CountdownBreakdown(
    val hasDonationHistory: Boolean,
    val isRuleUndecided: Boolean,
    val isZeroReached: Boolean,
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val lastDonationFormatted: String,
    val lastDonationTimeFormatted: String = "",
    val targetDateFormatted: String,
    val intervalDays: Long,
    val donationTypeCode: String,
    val customDonationTypeName: String = "",
    val ruleVersion: String = "v1.0"
) {
    /**
     * Exact Bangla format requested:
     * "45 দিন : 12 ঘণ্টা : 35 মিনিট : 20 সেকেন্ড"
     */
    fun formattedHeadline(strings: MbdStrings): String {
        val d = days.toString().padStart(2, '0')
        val h = hours.toString().padStart(2, '0')
        val m = minutes.toString().padStart(2, '0')
        val s = seconds.toString().padStart(2, '0')
        return if (strings.isBangla) {
            "$days দিন : $h ঘণ্টা : $m মিনিট : $s সেকেন্ড"
        } else {
            "$d Days : $h Hours : $m Min : $s Sec"
        }
    }
}

/**
 * Pure time-based & Donation-Type-aware eligibility & countdown engine.
 * - Evaluates donation type rules (`whole_blood`, `platelets`, `plasma`, `other`)
 * - Never guesses an interval for unknown/unverified `"other"` donation types (`isRuleUndecided = true`)
 * - Evaluates cross-donation restrictions across multiple recent verified records and picks the most restrictive target date.
 */
object DonationCountdownEngine {

    private val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    fun todayIsoDate(nowEpochMs: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(nowEpochMs))
    }

    fun currentIsoTime(nowEpochMs: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("HH:mm", Locale.US)
        return fmt.format(Date(nowEpochMs))
    }

    fun daysAgoIsoDate(daysAgo: Int, nowEpochMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowEpochMs
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(cal.time)
    }

    fun isValidIsoDate(dateStr: String): Boolean {
        val trimmed = dateStr.trim()
        if (!trimmed.matches(dateRegex)) return false
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
            }
            parser.parse(trimmed) != null
        } catch (e: Exception) {
            false
        }
    }

    fun isValidTime(timeStr: String): Boolean {
        val trimmed = timeStr.trim()
        return trimmed.isEmpty() || trimmed.matches(timeRegex)
    }

    fun bumpRuleVersion(currentVersion: String): String {
        val clean = currentVersion.trim().removePrefix("v")
        val parts = clean.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = (parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0) + 1
        return "v$major.$minor"
    }

    fun computeNextEligibleDateString(
        lastDonationDate: String,
        intervalDays: Long = 90L
    ): String {
        val trimmed = lastDonationDate.trim()
        if (!isValidIsoDate(trimmed)) return ""
        if (intervalDays <= 0L) return "UNDECIDED"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
            }
            val parsed = parser.parse(trimmed) ?: return ""
            val safeInterval = intervalDays.coerceIn(1L, 365L).toInt()
            val cal = Calendar.getInstance().apply {
                time = parsed
                add(Calendar.DAY_OF_YEAR, safeInterval)
            }
            parser.format(cal.time)
        } catch (e: Exception) {
            ""
        }
    }

    fun parseIsoDateStartMillis(dateStr: String, timeStr: String = ""): Long? {
        val trimmedDate = dateStr.trim()
        if (!isValidIsoDate(trimmedDate)) return null
        val trimmedTime = timeStr.trim()
        return try {
            if (trimmedTime.matches(timeRegex)) {
                val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                    isLenient = false
                }
                parser.parse("$trimmedDate $trimmedTime")?.time
            } else {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    isLenient = false
                }
                parser.parse(trimmedDate)?.time
            }
        } catch (e: Exception) {
            null
        }
    }

    fun computeCountsByType(
        records: List<DonationRecord>,
        fallbackDonor: DonorProfile? = null
    ): DonationTypeCountsSummary {
        val verified = records.filter { it.verificationStatus == "verified" }
        if (verified.isEmpty() && fallbackDonor != null && fallbackDonor.totalDonations > 0L) {
            val wb = fallbackDonor.wholeBloodCount
            val pl = fallbackDonor.plateletsCount
            val pm = fallbackDonor.plasmaCount
            val ot = fallbackDonor.otherDonationCount
            val sumSpecific = wb + pl + pm + ot
            return if (sumSpecific > 0L) {
                DonationTypeCountsSummary(
                    wholeBloodCount = wb,
                    plateletsCount = pl,
                    plasmaCount = pm,
                    otherDonationCount = ot,
                    totalConfirmed = fallbackDonor.totalDonations
                )
            } else {
                // Attribution based on donor's recorded donationType
                when (fallbackDonor.donationType) {
                    "platelets" -> DonationTypeCountsSummary(plateletsCount = fallbackDonor.totalDonations, totalConfirmed = fallbackDonor.totalDonations)
                    "plasma" -> DonationTypeCountsSummary(plasmaCount = fallbackDonor.totalDonations, totalConfirmed = fallbackDonor.totalDonations)
                    "other" -> DonationTypeCountsSummary(otherDonationCount = fallbackDonor.totalDonations, totalConfirmed = fallbackDonor.totalDonations)
                    else -> DonationTypeCountsSummary(wholeBloodCount = fallbackDonor.totalDonations, totalConfirmed = fallbackDonor.totalDonations)
                }
            }
        }

        var wb = 0L
        var pl = 0L
        var pm = 0L
        var ot = 0L
        for (r in verified) {
            when (r.donationType) {
                "whole_blood", "double_red_cell" -> wb++
                "platelets" -> pl++
                "plasma" -> pm++
                "other" -> ot++
                else -> wb++
            }
        }
        return DonationTypeCountsSummary(
            wholeBloodCount = wb,
            plateletsCount = pl,
            plasmaCount = pm,
            otherDonationCount = ot,
            totalConfirmed = verified.size.toLong()
        )
    }

    /**
     * Multi-Record & Cross-Donation Smart Eligibility Evaluator (Sections 4, 5 & 6 of Prompt).
     * - Considers all verified donation records and applicable eligibility rules.
     * - If the latest donation is of type 'other' and no verified rule/interval exists, returns isRuleUndecided = true
     *   and nextEligibleDate = "UNDECIDED" without guessing any interval.
     * - If multiple verified donations exist across different types, calculates both same-type and cross-donation
     *   recovery windows and selects the most restrictive (latest) target date.
     */
    fun evaluateMultiRecordEligibility(
        records: List<DonationRecord>,
        rules: Map<String, EligibilityRule> = DEFAULT_ELIGIBILITY_RULES
    ): MultiRecordEligibilityResult {
        val counts = computeCountsByType(records)
        val verified = records
            .filter { it.verificationStatus == "verified" && isValidIsoDate(it.donationDate) }
            .sortedWith(
                compareByDescending<DonationRecord> { it.donationDate }
                    .thenByDescending { it.donationTime }
            )

        if (verified.isEmpty()) {
            return MultiRecordEligibilityResult(
                hasVerifiedRecords = false,
                isRuleUndecided = false,
                lastDonationDate = "",
                lastDonationTime = "",
                lastDonationType = "whole_blood",
                customDonationTypeName = "",
                governingIntervalDays = 90L,
                governingRuleVersion = "v1.0",
                nextEligibleDate = "",
                governingExplanationBn = "",
                governingExplanationEn = "",
                countsByType = counts
            )
        }

        val latest = verified.first()
        val latestRule = rules[latest.donationType]
            ?: DEFAULT_ELIGIBILITY_RULES[latest.donationType]
            ?: DEFAULT_ELIGIBILITY_RULES["whole_blood"]!!

        // Check if the latest record is an unconfigured 'other' donation type
        val latestHasConfiguredInterval = if (latest.donationType == "other") {
            (latestRule.isVerifiedRule && latestRule.intervalDays >= 14L) ||
                (latest.eligibilityIntervalDays >= 14L && isValidIsoDate(latest.nextEligibleDate))
        } else {
            true
        }

        if (!latestHasConfiguredInterval) {
            return MultiRecordEligibilityResult(
                hasVerifiedRecords = true,
                isRuleUndecided = true,
                lastDonationDate = latest.donationDate,
                lastDonationTime = latest.donationTime,
                lastDonationType = latest.donationType,
                customDonationTypeName = latest.customDonationTypeName,
                governingIntervalDays = 0L,
                governingRuleVersion = latestRule.ruleVersion,
                nextEligibleDate = "UNDECIDED",
                governingExplanationBn = "অন্যান্য বা কাস্টম রক্তদানের ধরনের জন্য অনুমোদিত নিয়ম নির্ধারিত না থাকায় পরবর্তী তারিখ নির্ধারণ করা যায়নি।",
                governingExplanationEn = "Next eligible date could not be determined because no verified rule is configured for this custom donation type.",
                countsByType = counts
            )
        }

        // Evaluate all verified records to find the most restrictive target date across same-type & cross-donation rules
        var mostRestrictiveTargetMs = 0L
        var mostRestrictiveDateStr = ""
        var governingRecord = latest
        var governingInterval = latestRule.intervalDays.takeIf { it > 0L } ?: latest.eligibilityIntervalDays.coerceAtLeast(14L)
        var governingRuleVersion = latestRule.ruleVersion.ifBlank { latest.ruleVersion.ifBlank { "v1.0" } }

        for (record in verified) {
            val rule = rules[record.donationType]
                ?: DEFAULT_ELIGIBILITY_RULES[record.donationType]
                ?: DEFAULT_ELIGIBILITY_RULES["whole_blood"]!!

            val intervalForRecord = when {
                record.donationType == "other" -> {
                    if (rule.isVerifiedRule && rule.intervalDays > 0L) rule.intervalDays
                    else if (record.eligibilityIntervalDays > 0L) record.eligibilityIntervalDays
                    else 0L
                }
                rule.intervalDays > 0L -> rule.intervalDays
                record.eligibilityIntervalDays > 0L -> record.eligibilityIntervalDays
                else -> 90L
            }

            if (intervalForRecord <= 0L) continue

            val targetDateStr = computeNextEligibleDateString(record.donationDate, intervalForRecord)
            val targetMs = parseIsoDateStartMillis(targetDateStr, record.donationTime) ?: 0L
            if (targetMs > mostRestrictiveTargetMs) {
                mostRestrictiveTargetMs = targetMs
                mostRestrictiveDateStr = targetDateStr
                governingRecord = record
                governingInterval = intervalForRecord
                governingRuleVersion = rule.ruleVersion.ifBlank { record.ruleVersion.ifBlank { "v1.0" } }
            }
        }

        val governingTypeLabelBn = donationTypeLabel(
            governingRecord.donationType,
            isBangla = true,
            customTypeName = governingRecord.customDonationTypeName
        )
        val governingTypeLabelEn = donationTypeLabel(
            governingRecord.donationType,
            isBangla = false,
            customTypeName = governingRecord.customDonationTypeName
        )

        val explanationBn = if (governingRecord.id != latest.id) {
            "একাধিক সাম্প্রতিক রক্তদানের কারণে পূর্ববর্তী $governingTypeLabelBn (${governingRecord.donationDate})-এর $governingInterval দিনের কঠোরতর চিকিৎসাগত বিরতি প্রযোজ্য হয়েছে (রুল $governingRuleVersion)।"
        } else {
            "প্রযোজ্য নিয়ম অনুযায়ী নির্ধারিত • $governingTypeLabelBn ($governingInterval দিন বিরতি, রুল $governingRuleVersion)"
        }

        val explanationEn = if (governingRecord.id != latest.id) {
            "Governed by more restrictive prior $governingTypeLabelEn (${governingRecord.donationDate}) requiring $governingInterval days recovery (Rule $governingRuleVersion)."
        } else {
            "Determined by applicable rule • $governingTypeLabelEn ($governingInterval days interval, Rule $governingRuleVersion)"
        }

        return MultiRecordEligibilityResult(
            hasVerifiedRecords = true,
            isRuleUndecided = false,
            lastDonationDate = latest.donationDate,
            lastDonationTime = latest.donationTime,
            lastDonationType = latest.donationType,
            customDonationTypeName = latest.customDonationTypeName,
            governingIntervalDays = governingInterval,
            governingRuleVersion = governingRuleVersion,
            nextEligibleDate = mostRestrictiveDateStr,
            governingExplanationBn = explanationBn,
            governingExplanationEn = explanationEn,
            countsByType = counts
        )
    }

    fun computeCountdown(
        lastDonationDate: String,
        explicitNextEligibleDate: String = "",
        intervalDays: Long = 90L,
        donationType: String = "whole_blood",
        lastDonationTime: String = "",
        customDonationTypeName: String = "",
        ruleVersion: String = "v1.0",
        nowEpochMs: Long = System.currentTimeMillis()
    ): CountdownBreakdown {
        val cleanLast = lastDonationDate.trim()
        val cleanNext = explicitNextEligibleDate.trim()
        val cleanType = donationType.ifBlank { "whole_blood" }

        // If no donation date is recorded at all
        if (!isValidIsoDate(cleanLast) && !isValidIsoDate(cleanNext)) {
            return CountdownBreakdown(
                hasDonationHistory = false,
                isRuleUndecided = false,
                isZeroReached = false,
                days = 0L,
                hours = 0L,
                minutes = 0L,
                seconds = 0L,
                lastDonationFormatted = "",
                lastDonationTimeFormatted = lastDonationTime.trim(),
                targetDateFormatted = "",
                intervalDays = intervalDays.coerceAtLeast(0L),
                donationTypeCode = cleanType,
                customDonationTypeName = customDonationTypeName,
                ruleVersion = ruleVersion.ifBlank { "v1.0" }
            )
        }

        // Critical Rule: Do not guess an interval for 'other' or when nextEligibleDate is UNDECIDED / intervalDays <= 0
        val isUndecided = cleanNext.equals("UNDECIDED", ignoreCase = true) ||
            (cleanType == "other" && intervalDays <= 0L && !isValidIsoDate(cleanNext)) ||
            (intervalDays <= 0L && !isValidIsoDate(cleanNext))

        if (isUndecided) {
            return CountdownBreakdown(
                hasDonationHistory = true,
                isRuleUndecided = true,
                isZeroReached = false,
                days = 0L,
                hours = 0L,
                minutes = 0L,
                seconds = 0L,
                lastDonationFormatted = cleanLast,
                lastDonationTimeFormatted = lastDonationTime.trim(),
                targetDateFormatted = "UNDECIDED",
                intervalDays = 0L,
                donationTypeCode = cleanType,
                customDonationTypeName = customDonationTypeName,
                ruleVersion = ruleVersion.ifBlank { "v1.0-unconfigured" }
            )
        }

        val safeInterval = intervalDays.coerceIn(14L, 365L)
        val resolvedNextStr = when {
            isValidIsoDate(cleanNext) -> cleanNext
            isValidIsoDate(cleanLast) -> computeNextEligibleDateString(cleanLast, safeInterval)
            else -> ""
        }

        val targetMillis = parseIsoDateStartMillis(resolvedNextStr, lastDonationTime)
        val lastMillis = parseIsoDateStartMillis(cleanLast, lastDonationTime)
        val finalTargetMs = targetMillis ?: ((lastMillis ?: 0L) + safeInterval * 86_400_000L)
        val diffMs = finalTargetMs - nowEpochMs

        if (diffMs <= 0L) {
            return CountdownBreakdown(
                hasDonationHistory = true,
                isRuleUndecided = false,
                isZeroReached = true,
                days = 0L,
                hours = 0L,
                minutes = 0L,
                seconds = 0L,
                lastDonationFormatted = cleanLast,
                lastDonationTimeFormatted = lastDonationTime.trim(),
                targetDateFormatted = resolvedNextStr,
                intervalDays = safeInterval,
                donationTypeCode = cleanType,
                customDonationTypeName = customDonationTypeName,
                ruleVersion = ruleVersion.ifBlank { "v1.0" }
            )
        }

        val totalSeconds = diffMs / 1000L
        val days = totalSeconds / 86_400L
        val hours = (totalSeconds % 86_400L) / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L

        return CountdownBreakdown(
            hasDonationHistory = true,
            isRuleUndecided = false,
            isZeroReached = false,
            days = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            lastDonationFormatted = cleanLast,
            lastDonationTimeFormatted = lastDonationTime.trim(),
            targetDateFormatted = resolvedNextStr,
            intervalDays = safeInterval,
            donationTypeCode = cleanType,
            customDonationTypeName = customDonationTypeName,
            ruleVersion = ruleVersion.ifBlank { "v1.0" }
        )
    }

    fun donationTypeLabel(
        code: String,
        isBangla: Boolean,
        customTypeName: String = ""
    ): String {
        if (code == "other" && customTypeName.isNotBlank()) {
            return if (isBangla) {
                "Other — অন্যান্য (${customTypeName.trim()})"
            } else {
                "Other (${customTypeName.trim()})"
            }
        }
        val option = DONATION_TYPE_OPTIONS.find { it.code == code }
        return if (option != null) {
            if (isBangla) option.labelBn else option.labelEn
        } else if (code == "double_red_cell") {
            if (isBangla) "Double Red Cell — ডাবল রেড সেল" else "Double Red Cell"
        } else {
            if (isBangla) "Whole Blood — সম্পূর্ণ রক্ত" else "Whole Blood — সম্পূর্ণ রক্ত"
        }
    }
}

/**
 * Live Countdown Timer Widget for Donor Profile, Public Profile, and My Donations screen.
 * - Supports Donation-Type-specific rules (`whole_blood`, `platelets`, `plasma`, `other`).
 * - Stops the 1-second timer automatically when the rule is undecided (`isRuleUndecided`) or when zero is reached.
 * - Displays per-type donation counts and rule version metadata.
 */
@Composable
fun LiveDonationCountdownCard(
    strings: MbdStrings,
    lastDonationDate: String,
    nextEligibleDate: String,
    eligibilityIntervalDays: Long,
    donationType: String,
    totalConfirmedDonations: Long,
    lastDonationTime: String = "",
    customDonationTypeName: String = "",
    ruleVersion: String = "v1.0",
    countsByType: DonationTypeCountsSummary? = null,
    governingRuleExplanation: String = "",
    pendingDonationsCount: Int = 0,
    isPublicProfileHiddenByPrivacy: Boolean = false,
    onRecordDonationClick: (() -> Unit)? = null,
    onAdminCorrectDatesClick: (() -> Unit)? = null,
    onAdminConfigureRulesClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val breakdown = remember(
        lastDonationDate,
        lastDonationTime,
        nextEligibleDate,
        eligibilityIntervalDays,
        donationType,
        customDonationTypeName,
        ruleVersion,
        nowEpochMs
    ) {
        DonationCountdownEngine.computeCountdown(
            lastDonationDate = lastDonationDate,
            explicitNextEligibleDate = nextEligibleDate,
            intervalDays = eligibilityIntervalDays,
            donationType = donationType,
            lastDonationTime = lastDonationTime,
            customDonationTypeName = customDonationTypeName,
            ruleVersion = ruleVersion,
            nowEpochMs = nowEpochMs
        )
    }

    // Only run the 1-second live ticker when countdown is actively running (not undecided, not zero reached, and has history)
    LaunchedEffect(
        breakdown.hasDonationHistory,
        breakdown.isRuleUndecided,
        breakdown.isZeroReached,
        lastDonationDate,
        nextEligibleDate,
        eligibilityIntervalDays
    ) {
        if (breakdown.hasDonationHistory && !breakdown.isRuleUndecided && !breakdown.isZeroReached) {
            while (true) {
                delay(1000L)
                nowEpochMs = System.currentTimeMillis()
            }
        }
    }

    val resolvedTypeLabel = DonationCountdownEngine.donationTypeLabel(
        code = breakdown.donationTypeCode,
        isBangla = strings.isBangla,
        customTypeName = customDonationTypeName
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MbdSurface),
        border = BorderStroke(1.2.dp, MbdCrimson.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_donation_countdown_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Crimson Gradient Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(MbdCrimson, MbdCrimsonDark)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.countdownTitle,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (breakdown.isRuleUndecided) {
                                    "$resolvedTypeLabel • ${strings.nextDateUndecidedText}"
                                } else {
                                    "$resolvedTypeLabel • " + (if (strings.isBangla) "বিরতি: ${breakdown.intervalDays} দিন (${breakdown.ruleVersion})" else "${breakdown.intervalDays}d (${breakdown.ruleVersion})")
                                },
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Status badge in header (LIVE vs PAUSED)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (breakdown.isRuleUndecided) Color(0xFFFFD54F) else Color(0xFF69F0AE)
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (breakdown.isRuleUndecided) {
                                    if (strings.isBangla) "স্থগিত" else "PAUSED"
                                } else {
                                    if (strings.isBangla) "লাইভ" else "LIVE"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isPublicProfileHiddenByPrivacy) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MbdSoftPinkAlert.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MbdCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (strings.isBangla) {
                                    "রক্তদাতা তাঁর রক্তদানের ইতিহাস ও কাউন্টডাউন প্রাইভেসি সেটিংসের মাধ্যমে গোপন রেখেছেন।"
                                } else {
                                    "This donor has chosen to keep their donation history and countdown private."
                                },
                                fontSize = 13.sp,
                                color = MbdTextPrimary
                            )
                        }
                    }
                } else if (!breakdown.hasDonationHistory) {
                    // Empty state when no donation history is recorded
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MbdCrimsonLight.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MbdCrimson.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("countdown_no_history_banner")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = MbdCrimson,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.noDonationHistoryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MbdCrimson,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (strings.isBangla) {
                                    "\"রক্তদান যোগ করুন\" বাটনে ক্লিক করে রক্তদানের ধরন ও তারিখ সংরক্ষণ করলে স্মার্ট কাউন্টডাউন শুরু হবে।"
                                } else {
                                    "Tap \"Record Donation\" to select your donation type and start the smart countdown timer."
                                },
                                fontSize = 12.sp,
                                color = MbdTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Required Bangla-First Structured Summary Block (Section 4 of Prompt):
                    // শেষ রক্তদানের ধরন: ...
                    // শেষ রক্তদানের তারিখ: ...
                    // পরবর্তী সম্ভাব্য তারিখ: প্রযোজ্য নিয়ম অনুযায়ী নির্ধারিত (...) OR পরবর্তী তারিখ নির্ধারণ করা যায়নি
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MbdSubtleSurface,
                        border = BorderStroke(1.dp, MbdBorderGray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("smart_countdown_summary_box")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${strings.lastDonationTypePrefix} $resolvedTypeLabel",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MbdTextPrimary
                            )
                            val dateAndTimeText = buildString {
                                append(breakdown.lastDonationFormatted.ifBlank { "-" })
                                if (breakdown.lastDonationTimeFormatted.isNotBlank()) {
                                    append(" (${breakdown.lastDonationTimeFormatted})")
                                }
                            }
                            Text(
                                text = "${strings.lastDonationDatePrefix} $dateAndTimeText",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MbdTextPrimary
                            )
                            Text(
                                text = if (breakdown.isRuleUndecided) {
                                    "${strings.nextEligibleDateLabel}: ${strings.nextDateUndecidedText}"
                                } else {
                                    "${strings.nextEligibleByRulePrefix} (${breakdown.targetDateFormatted})"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (breakdown.isRuleUndecided) Color(0xFFE65100) else MbdAvailableGreen
                            )
                            if (governingRuleExplanation.isNotBlank()) {
                                Text(
                                    text = governingRuleExplanation,
                                    fontSize = 11.sp,
                                    color = MbdTextSecondary
                                )
                            }
                        }
                    }

                    if (breakdown.isRuleUndecided) {
                        // Unconfigured / Unknown Donation Type ("other") State -> Stop Countdown!
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MbdWarningCardBg,
                            border = BorderStroke(1.5.dp, MbdWarningCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("countdown_undecided_banner")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = strings.nextDateUndecidedText,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFBF360C),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (strings.isBangla) {
                                        "এই রক্তদানের ধরনের জন্য অনুমোদিত চিকিৎসাগত বিরতি নির্ধারিত নেই। অনুমানভিত্তিক বিরতি গণনা না করে কাউন্টডাউন বন্ধ রাখা হয়েছে।"
                                    } else {
                                        "No verified medical eligibility interval is configured for this donation type. The countdown timer is stopped."
                                    },
                                    fontSize = 12.sp,
                                    color = Color(0xFFBF360C),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (breakdown.isZeroReached) {
                        // Countdown Reached Zero State: "সম্ভাব্য পরবর্তী রক্তদানের সময় হয়েছে"
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MbdAvailableGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.5.dp, MbdAvailableGreen.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("countdown_zero_reached_banner")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MbdAvailableGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = strings.countdownZeroReachedText,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MbdAvailableGreen,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (strings.isBangla) {
                                        "সর্বশেষ রক্তদান: ${breakdown.lastDonationFormatted} • নির্ধারিত বিরতি (${breakdown.intervalDays} দিন) পূর্ণ হয়েছে"
                                    } else {
                                        "Last donation: ${breakdown.lastDonationFormatted} • ${breakdown.intervalDays}-day interval reached"
                                    },
                                    fontSize = 12.sp,
                                    color = MbdTextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Active Live Countdown: "45 দিন : 12 ঘণ্টা : 35 মিনিট : 20 সেকেন্ড"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MbdCrimsonLight.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MbdCrimson.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("countdown_active_headline")
                        ) {
                            Text(
                                text = breakdown.formattedHeadline(strings),
                                color = MbdCrimsonDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }

                        // 4 Digital Unit Boxes (Days, Hours, Minutes, Seconds)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CountdownUnitTile(
                                value = breakdown.days.toString().padStart(2, '0'),
                                unitLabel = strings.daysUnit,
                                modifier = Modifier.weight(1f)
                            )
                            CountdownUnitTile(
                                value = breakdown.hours.toString().padStart(2, '0'),
                                unitLabel = strings.hoursUnit,
                                modifier = Modifier.weight(1f)
                            )
                            CountdownUnitTile(
                                value = breakdown.minutes.toString().padStart(2, '0'),
                                unitLabel = strings.minutesUnit,
                                modifier = Modifier.weight(1f)
                            )
                            CountdownUnitTile(
                                value = breakdown.seconds.toString().padStart(2, '0'),
                                unitLabel = strings.secondsUnit,
                                isHighlight = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Per-Type Donation Records Breakdown (Section 5 of Prompt)
                    val resolvedCounts = countsByType ?: DonationTypeCountsSummary(
                        wholeBloodCount = if (donationType == "whole_blood") totalConfirmedDonations else 0L,
                        plateletsCount = if (donationType == "platelets") totalConfirmedDonations else 0L,
                        plasmaCount = if (donationType == "plasma") totalConfirmedDonations else 0L,
                        otherDonationCount = if (donationType == "other") totalConfirmedDonations else 0L,
                        totalConfirmed = totalConfirmedDonations
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MbdInfoCardBg,
                        border = BorderStroke(1.dp, MbdCrimson.copy(alpha = 0.18f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("donation_counts_by_type_row")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (strings.isBangla) "ধরন অনুযায়ী মোট রক্তদান" else "Donations Separated by Type",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MbdCrimsonDark
                                )
                                Text(
                                    text = if (strings.isBangla) "মোট: $totalConfirmedDonations বার" else "Total: $totalConfirmedDonations",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MbdCrimson
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TypeCountMiniPill(
                                    label = if (strings.isBangla) "সম্পূর্ণ রক্ত" else "Whole",
                                    count = resolvedCounts.wholeBloodCount,
                                    modifier = Modifier.weight(1f)
                                )
                                TypeCountMiniPill(
                                    label = if (strings.isBangla) "প্লেটলেট" else "Platelets",
                                    count = resolvedCounts.plateletsCount,
                                    modifier = Modifier.weight(1f)
                                )
                                TypeCountMiniPill(
                                    label = if (strings.isBangla) "প্লাজমা" else "Plasma",
                                    count = resolvedCounts.plasmaCount,
                                    modifier = Modifier.weight(1f)
                                )
                                TypeCountMiniPill(
                                    label = if (strings.isBangla) "অন্যান্য" else "Other",
                                    count = resolvedCounts.otherDonationCount,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Pending Verification Notice if there are unverified records
                if (pendingDonationsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MbdWarningCardBg,
                        border = BorderStroke(1.dp, MbdWarningCardBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (strings.isBangla) {
                                    "$pendingDonationsCount টি রক্তদানের রেকর্ড যাচাইকরণ অপেক্ষমাণ (Pending Verification)। অনুমোদিত না হওয়া পর্যন্ত এটি কাউন্টডাউনে গণ্য হবে না।"
                                } else {
                                    "$pendingDonationsCount donation record(s) pending verification. Unverified records do not alter the confirmed countdown."
                                },
                                fontSize = 11.sp,
                                color = Color(0xFFBF360C),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Medical Disclaimer (Required by Section 7 of prompt)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MbdSubtleSurface,
                    border = BorderStroke(1.dp, MbdBorderGray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("countdown_medical_disclaimer")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = MbdCrimson,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.medicalDisclaimerText,
                            fontSize = 11.sp,
                            color = MbdTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Action Buttons Row (Record Blood Donation for Owner / Correct Dates or Configure Rules for Admin)
                if (onRecordDonationClick != null || onAdminCorrectDatesClick != null || onAdminConfigureRulesClick != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onRecordDonationClick != null) {
                                Button(
                                    onClick = onRecordDonationClick,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MbdCrimson,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("record_blood_donation_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.recordNewDonationBtn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (onAdminCorrectDatesClick != null) {
                                OutlinedButton(
                                    onClick = onAdminCorrectDatesClick,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MbdCrimson),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MbdCrimson),
                                    modifier = Modifier
                                        .height(46.dp)
                                        .testTag("admin_correct_dates_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditCalendar,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (strings.isBangla) "তারিখ সংশোধন" else "Correct Date",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        if (onAdminConfigureRulesClick != null) {
                            OutlinedButton(
                                onClick = onAdminConfigureRulesClick,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MbdCrimsonDark.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MbdCrimsonDark),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("admin_configure_rules_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Rule,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.adminConfigureRulesBtn,
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
}

@Composable
private fun TypeCountMiniPill(
    label: String,
    count: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MbdSurface,
        border = BorderStroke(0.8.dp, MbdCrimson.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = MbdCrimson
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MbdTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CountdownUnitTile(
    value: String,
    unitLabel: String,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlight) MbdCrimson else MbdInfoCardBg,
        border = BorderStroke(
            1.dp,
            if (isHighlight) MbdCrimsonDark else MbdCrimson.copy(alpha = 0.25f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = if (isHighlight) Color.White else MbdCrimson,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = unitLabel,
                color = if (isHighlight) Color.White.copy(alpha = 0.9f) else MbdTextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Compact Live Countdown Badge for Donor Cards in FindDonorsScreen.
 * Updates every second while visible on screen; stops if undecided or zero reached.
 */
@Composable
fun CompactDonorCardCountdownBadge(
    lastDonationDate: String,
    nextEligibleDate: String,
    eligibilityIntervalDays: Long,
    showDonationHistoryPublicly: Boolean,
    strings: MbdStrings,
    donationType: String = "whole_blood",
    modifier: Modifier = Modifier
) {
    if (!showDonationHistoryPublicly) return

    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val breakdown = remember(lastDonationDate, nextEligibleDate, eligibilityIntervalDays, donationType, nowEpochMs) {
        DonationCountdownEngine.computeCountdown(
            lastDonationDate = lastDonationDate,
            explicitNextEligibleDate = nextEligibleDate,
            intervalDays = eligibilityIntervalDays,
            donationType = donationType,
            nowEpochMs = nowEpochMs
        )
    }

    LaunchedEffect(
        breakdown.hasDonationHistory,
        breakdown.isRuleUndecided,
        breakdown.isZeroReached,
        lastDonationDate,
        nextEligibleDate,
        eligibilityIntervalDays
    ) {
        if (breakdown.hasDonationHistory && !breakdown.isRuleUndecided && !breakdown.isZeroReached) {
            while (true) {
                delay(1000L)
                nowEpochMs = System.currentTimeMillis()
            }
        }
    }

    if (!breakdown.hasDonationHistory) return

    val (bgColor, borderColor, textColor) = when {
        breakdown.isRuleUndecided -> Triple(
            MbdWarningCardBg,
            MbdWarningCardBorder.copy(alpha = 0.5f),
            Color(0xFFBF360C)
        )
        breakdown.isZeroReached -> Triple(
            MbdAvailableGreen.copy(alpha = 0.1f),
            MbdAvailableGreen.copy(alpha = 0.35f),
            MbdAvailableGreen
        )
        else -> Triple(
            MbdCrimsonLight.copy(alpha = 0.65f),
            MbdCrimson.copy(alpha = 0.25f),
            MbdCrimsonDark
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(0.8.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    breakdown.isRuleUndecided -> Icons.Default.PauseCircle
                    breakdown.isZeroReached -> Icons.Default.EventAvailable
                    else -> Icons.Default.Schedule
                },
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when {
                    breakdown.isRuleUndecided -> strings.nextDateUndecidedText
                    breakdown.isZeroReached -> strings.countdownZeroReachedText
                    strings.isBangla -> "পরবর্তী রক্তদান: ${breakdown.days} দিন ${breakdown.hours} ঘঃ ${breakdown.minutes} মিঃ ${breakdown.seconds} সেঃ"
                    else -> "Next eligible: ${breakdown.days}d ${breakdown.hours}h ${breakdown.minutes}m ${breakdown.seconds}s"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 3-State Donor Availability Status Badge (Available to be contacted / Temporarily unavailable / Not currently accepting requests).
 */
@Composable
fun AvailabilityStatusBadge(
    availabilityStatus: String,
    isAvailableFallback: Boolean,
    strings: MbdStrings,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val resolved = when (availabilityStatus) {
        "available", "temporarily_unavailable", "not_accepting" -> availabilityStatus
        else -> if (isAvailableFallback) "available" else "temporarily_unavailable"
    }

    val (bgColor, textColor, labelText) = when (resolved) {
        "available" -> Triple(
            MbdAvailableGreen.copy(alpha = 0.14f),
            MbdAvailableGreen,
            if (compact) strings.availableBadge else strings.availabilityAvailableToContact
        )
        "not_accepting" -> Triple(
            MbdCrimsonLight.copy(alpha = 0.7f),
            MbdCrimsonDark,
            if (compact) {
                if (strings.isBangla) "অনুরোধ গ্রহণ করছেন না" else "Not accepting requests"
            } else {
                strings.availabilityNotAcceptingRequests
            }
        )
        else -> Triple(
            MbdWarningCardBg,
            MbdWarningCardIcon,
            if (compact) strings.unavailableBadge else strings.availabilityTemporarilyUnavailable
        )
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = BorderStroke(0.8.dp, textColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 2.dp else 5.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 6.dp else 8.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = labelText,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 10.sp else 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 3-Option Manual Donor Availability Selector.
 */
@Composable
fun DonorAvailabilityStatusSelector(
    selectedStatus: String,
    onSelectStatus: (String) -> Unit,
    strings: MbdStrings,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        Triple(
            "available",
            strings.availabilityAvailableToContact,
            Icons.Default.CheckCircle to MbdAvailableGreen
        ),
        Triple(
            "temporarily_unavailable",
            strings.availabilityTemporarilyUnavailable,
            Icons.Default.PauseCircle to Color(0xFFE65100)
        ),
        Triple(
            "not_accepting",
            strings.availabilityNotAcceptingRequests,
            Icons.Default.RemoveCircleOutline to MbdCrimson
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (code, label, iconAndTint) ->
            val isSelected = selectedStatus == code
            val (icon, tint) = iconAndTint
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) tint.copy(alpha = 0.22f) else MbdSubtleSurface,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) tint else MbdBorderGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectStatus(code) }
                    .testTag("availability_option_$code")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectStatus(code) },
                        colors = RadioButtonDefaults.colors(selectedColor = tint)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MbdTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Donation History Timeline Section (Sections 4, 5 & 6 of Prompt).
 * Displays each donation record with its specific donation type, date & time, rule version,
 * procedure confirmation, and admin verification/correction actions.
 */
@Composable
fun DonationHistoryTimelineSection(
    records: List<DonationRecord>,
    strings: MbdStrings,
    canEditOrDeleteOwn: Boolean,
    canAdminVerifyOrCorrect: Boolean,
    onRecordDonationClick: (() -> Unit)? = null,
    onApproveRecord: ((DonationRecord) -> Unit)? = null,
    onMarkPendingRecord: ((DonationRecord) -> Unit)? = null,
    onDeleteRecord: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MbdSurface),
        border = BorderStroke(1.dp, MbdBorderGray),
        modifier = modifier
            .fillMaxWidth()
            .testTag("donation_history_timeline_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MbdCrimsonLight,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MbdCrimson,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.donationHistoryTimelineTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MbdTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val confirmedCount = records.count { it.verificationStatus == "verified" }
                        Text(
                            text = if (strings.isBangla) {
                                "মোট নিশ্চিতকৃত রক্তদান: $confirmedCount বার"
                            } else {
                                "Total Confirmed Donations: $confirmedCount"
                            },
                            fontSize = 11.sp,
                            color = MbdTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onRecordDonationClick != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MbdCrimsonLight,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onRecordDonationClick() }
                            .testTag("timeline_add_donation_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = MbdCrimson,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.recordNewDonationBtn,
                                color = MbdCrimson,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MbdBorderGray.copy(alpha = 0.6f))

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = MbdCrimson.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.noDonationHistoryText,
                            color = MbdTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                records.forEachIndexed { index, record ->
                    val isVerified = record.verificationStatus == "verified"
                    val isPending = record.verificationStatus == "pending"
                    val isUndecidedRecord = record.nextEligibleDate.equals("UNDECIDED", ignoreCase = true) ||
                        (record.donationType == "other" && record.eligibilityIntervalDays <= 0L)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPending) MbdPendingCardBg else MbdSubtleSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isPending) Color(0xFFFFB300).copy(alpha = 0.45f) else MbdBorderGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("timeline_item_${record.id.ifBlank { index.toString() }}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MbdCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = buildString {
                                            append(record.donationDate)
                                            if (record.donationTime.isNotBlank()) {
                                                append(" • ${record.donationTime}")
                                            }
                                        },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = MbdTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MbdCrimsonLight
                                    ) {
                                        Text(
                                            text = record.bloodGroup,
                                            color = MbdCrimson,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Verification Badge (Confirmed vs Pending Verification)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = when {
                                        isVerified -> MbdAvailableGreen.copy(alpha = 0.14f)
                                        isPending -> MbdWarningCardBg
                                        else -> MbdCrimsonLight
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                isVerified -> Icons.Default.Verified
                                                isPending -> Icons.Default.HourglassTop
                                                else -> Icons.Default.Info
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                isVerified -> MbdAvailableGreen
                                                isPending -> Color(0xFFE65100)
                                                else -> MbdCrimson
                                            },
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = when {
                                                isVerified -> strings.verificationConfirmedLabel
                                                isPending -> strings.verificationPendingLabel
                                                else -> strings.verificationRejectedLabel
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isVerified -> MbdAvailableGreen
                                                isPending -> Color(0xFFE65100)
                                                else -> MbdCrimson
                                            }
                                        )
                                    }
                                }
                            }

                            // Donation Type, Rule Version & Next Eligible Date
                            Text(
                                text = buildString {
                                    append(
                                        DonationCountdownEngine.donationTypeLabel(
                                            record.donationType,
                                            strings.isBangla,
                                            record.customDonationTypeName
                                        )
                                    )
                                    append(" • ")
                                    if (isUndecidedRecord) {
                                        append(strings.nextDateUndecidedText)
                                    } else if (record.nextEligibleDate.isNotBlank()) {
                                        append(if (strings.isBangla) "পরবর্তী সম্ভাব্য তারিখ: " else "Next eligible: ")
                                        append(record.nextEligibleDate)
                                        if (record.ruleVersion.isNotBlank()) {
                                            append(" (${record.ruleVersion})")
                                        }
                                    }
                                },
                                fontSize = 12.sp,
                                color = if (isUndecidedRecord) Color(0xFFE65100) else MbdTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Hospital & Location
                            if (record.hospitalName.isNotBlank() || record.location.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalHospital,
                                        contentDescription = null,
                                        tint = MbdTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = listOf(record.hospitalName, record.location)
                                            .filter { it.isNotBlank() }
                                            .joinToString(", "),
                                        fontSize = 12.sp,
                                        color = MbdTextPrimary
                                    )
                                }
                            }

                            if (record.procedureConfirmation.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FactCheck,
                                        contentDescription = null,
                                        tint = MbdAvailableGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (strings.isBangla) {
                                            "প্রক্রিয়া নিশ্চিতকরণ: ${record.procedureConfirmation}"
                                        } else {
                                            "Procedure Confirmation: ${record.procedureConfirmation}"
                                        },
                                        fontSize = 11.sp,
                                        color = MbdTextPrimary
                                    )
                                }
                            }

                            if (record.recipientName.isNotBlank() || record.notes.isNotBlank()) {
                                Text(
                                    text = listOfNotNull(
                                        record.recipientName.takeIf { it.isNotBlank() }?.let {
                                            if (strings.isBangla) "রোগী: $it" else "Recipient: $it"
                                        },
                                        record.notes.takeIf { it.isNotBlank() }
                                    ).joinToString(" • "),
                                    fontSize = 11.sp,
                                    color = MbdTextSecondary
                                )
                            }

                            // Admin Verification / Owner Delete Actions
                            if (canAdminVerifyOrCorrect || canEditOrDeleteOwn) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (canAdminVerifyOrCorrect && isPending && onApproveRecord != null) {
                                        TextButton(
                                            onClick = { onApproveRecord(record) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MbdAvailableGreen,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (strings.isBangla) "অনুমোদন করুন (Verify)" else "Approve & Verify",
                                                color = MbdAvailableGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    } else if (canAdminVerifyOrCorrect && isVerified && onMarkPendingRecord != null) {
                                        TextButton(
                                            onClick = { onMarkPendingRecord(record) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (strings.isBangla) "অপেক্ষমাণ রাখুন" else "Mark Pending",
                                                color = Color(0xFFE65100),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    if (canEditOrDeleteOwn && onDeleteRecord != null && record.id.isNotBlank()) {
                                        IconButton(
                                            onClick = { onDeleteRecord(record.id) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete record",
                                                tint = MbdTextSecondary,
                                                modifier = Modifier.size(17.dp)
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
    }
}

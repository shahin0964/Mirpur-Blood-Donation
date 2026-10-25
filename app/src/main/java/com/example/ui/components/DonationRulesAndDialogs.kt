package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DEFAULT_ELIGIBILITY_RULES
import com.example.data.model.DONATION_TYPE_OPTIONS
import com.example.data.model.DonationRecord
import com.example.data.model.EligibilityRule
import com.example.ui.i18n.MbdStrings
import com.example.ui.theme.MbdAvailableGreen
import com.example.ui.theme.MbdBorderGray
import com.example.ui.theme.MbdChipUnselected
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdCrimsonLight
import com.example.ui.theme.MbdTextPrimary
import com.example.ui.theme.MbdTextSecondary

/**
 * Bangla-First Donation Record Form Dialog (Sections 2, 3, 4 & 8 of Prompt).
 * - Mandatory Donation Type selection (Whole Blood, Platelets, Plasma, Other / Custom).
 * - Never guesses an interval for 'other' unless an authorized admin has configured & verified the rule.
 * - Required Donation Date (YYYY-MM-DD) and Time (HH:mm).
 * - Optional Hospital/Donation Center, Procedure Confirmation, Recipient, and Additional Notes.
 * - Duplicate donation prevention (same date + same donation type).
 */
@Composable
fun RecordBloodDonationDialog(
    strings: MbdStrings,
    defaultBloodGroup: String,
    defaultLocation: String = "Mirpur-10, Dhaka",
    isSaving: Boolean,
    isAdminUser: Boolean = false,
    eligibilityRules: Map<String, EligibilityRule> = DEFAULT_ELIGIBILITY_RULES,
    existingRecords: List<DonationRecord> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmRecord: (
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
    ) -> Unit
) {
    var selectedTypeCode by remember { mutableStateOf("whole_blood") }
    var customDonationTypeName by remember { mutableStateOf("") }
    var donationDate by remember { mutableStateOf(DonationCountdownEngine.todayIsoDate()) }
    var donationTime by remember { mutableStateOf(DonationCountdownEngine.currentIsoTime()) }
    var hospitalName by remember { mutableStateOf("") }
    var procedureConfirmation by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(defaultLocation.ifBlank { "Mirpur-10, Dhaka" }) }
    var recipientName by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf(defaultBloodGroup.ifBlank { "O+" }) }
    var notes by remember { mutableStateOf("") }
    var submitAsVerified by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val activeRule = remember(selectedTypeCode, eligibilityRules) {
        eligibilityRules[selectedTypeCode]
            ?: DEFAULT_ELIGIBILITY_RULES[selectedTypeCode]
            ?: DEFAULT_ELIGIBILITY_RULES["whole_blood"]!!
    }

    var adminCustomIntervalText by remember(selectedTypeCode, activeRule) {
        mutableStateOf(
            if (activeRule.isVerifiedRule && activeRule.intervalDays > 0L) {
                activeRule.intervalDays.toString()
            } else {
                ""
            }
        )
    }

    val isRuleVerifiedForType = remember(selectedTypeCode, activeRule, isAdminUser, adminCustomIntervalText) {
        if (selectedTypeCode == "other") {
            if (activeRule.isVerifiedRule && activeRule.intervalDays >= 14L) {
                true
            } else {
                isAdminUser && (adminCustomIntervalText.toLongOrNull() ?: 0L) >= 14L
            }
        } else {
            activeRule.isVerifiedRule && activeRule.intervalDays >= 14L
        }
    }

    val effectiveIntervalDays = remember(selectedTypeCode, activeRule, isRuleVerifiedForType, isAdminUser, adminCustomIntervalText) {
        if (!isRuleVerifiedForType) {
            0L
        } else if (isAdminUser && adminCustomIntervalText.toLongOrNull() != null) {
            adminCustomIntervalText.toLongOrNull()!!.coerceIn(14L, 365L)
        } else {
            activeRule.intervalDays.coerceIn(14L, 365L)
        }
    }

    val previewNextEligible = remember(donationDate, effectiveIntervalDays, isRuleVerifiedForType) {
        if (!isRuleVerifiedForType || effectiveIntervalDays <= 0L) {
            "UNDECIDED"
        } else {
            DonationCountdownEngine.computeNextEligibleDateString(donationDate, effectiveIntervalDays)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = MbdCrimson,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.recordNewDonationBtn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Mandatory Donation Type Selection (4 Required Options)
                Text(
                    text = strings.donationTypeLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MbdTextPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DONATION_TYPE_OPTIONS.forEach { option ->
                        val isSelected = selectedTypeCode == option.code
                        val ruleForOpt = eligibilityRules[option.code] ?: DEFAULT_ELIGIBILITY_RULES[option.code]
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MbdCrimsonLight.copy(alpha = 0.72f) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MbdCrimson else MbdBorderGray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedTypeCode = option.code
                                    validationError = null
                                }
                                .testTag("donation_type_option_${option.code}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTypeCode = option.code
                                        validationError = null
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = MbdCrimson),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (strings.isBangla) option.labelBn else option.labelEn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MbdTextPrimary
                                    )
                                    val ruleSub = if (option.code == "other" && (ruleForOpt == null || !ruleForOpt.isVerifiedRule)) {
                                        if (strings.isBangla) {
                                            "অ্যাডমিন নিয়ম নির্ধারণ না করা পর্যন্ত কাউন্টডাউন বন্ধ থাকবে"
                                        } else {
                                            "No interval guessed until an admin configures a rule"
                                        }
                                    } else {
                                        val iv = ruleForOpt?.intervalDays ?: option.defaultIntervalDays.toLong()
                                        val rv = ruleForOpt?.ruleVersion ?: "v1.0"
                                        if (strings.isBangla) {
                                            "প্রযোজ্য বিরতি: $iv দিন • রুল সংস্করণ: $rv"
                                        } else {
                                            "Applicable interval: $iv days • Rule: $rv"
                                        }
                                    }
                                    Text(
                                        text = ruleSub,
                                        fontSize = 10.sp,
                                        color = MbdTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedTypeCode == "other") {
                    OutlinedTextField(
                        value = customDonationTypeName,
                        onValueChange = {
                            customDonationTypeName = it
                            validationError = null
                        },
                        label = {
                            Text(
                                if (strings.isBangla) {
                                    "অন্যান্য রক্তদানের নাম / বিবরণ *"
                                } else {
                                    "Custom Donation Type Name *"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_custom_donation_type_input")
                    )

                    if (isAdminUser) {
                        OutlinedTextField(
                            value = adminCustomIntervalText,
                            onValueChange = { adminCustomIntervalText = it.filter { ch -> ch.isDigit() } },
                            label = {
                                Text(
                                    if (strings.isBangla) {
                                        "অ্যাডমিন নির্ধারিত বিরতি (দিন) — খালি রাখলে অনির্ণীত থাকবে"
                                    } else {
                                        "Admin Configured Interval (Days) — Leave blank if undecided"
                                    }
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Rule Preview Banner (Verified Rule vs Undecided Rule)
                if (!isRuleVerifiedForType || previewNextEligible == "UNDECIDED") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF8E1),
                        border = BorderStroke(1.dp, Color(0xFFFFB300)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_undecided_rule_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = strings.nextDateUndecidedText,
                                    color = Color(0xFFBF360C),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (strings.isBangla) {
                                        "অজানা বা কাস্টম রক্তদানের জন্য কোনো অনুমানভিত্তিক বিরতি ধরা হয়নি। কাউন্টডাউন বন্ধ থাকবে।"
                                    } else {
                                        "No interval is guessed for an unverified custom donation type. Countdown will remain stopped."
                                    },
                                    color = Color(0xFFBF360C),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                } else if (previewNextEligible.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MbdAvailableGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MbdAvailableGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${strings.nextEligibleByRulePrefix}: $previewNextEligible",
                                color = MbdAvailableGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (strings.isBangla) {
                                    "বিরতি: $effectiveIntervalDays দিন • রুল সংস্করণ: ${activeRule.ruleVersion}"
                                } else {
                                    "Interval: $effectiveIntervalDays days • Rule version: ${activeRule.ruleVersion}"
                                },
                                color = MbdTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // 2. Required Donation Date & Time
                Text(
                    text = if (strings.isBangla) "রক্তদানের তারিখ ও সময় নির্বাচন করুন *" else "Donation Date & Time *",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MbdTextPrimary
                )

                val datePresets = remember {
                    listOf(
                        (if (strings.isBangla) "আজ" else "Today") to DonationCountdownEngine.todayIsoDate(),
                        (if (strings.isBangla) "৭ দিন আগে" else "7d ago") to DonationCountdownEngine.daysAgoIsoDate(7),
                        (if (strings.isBangla) "১৫ দিন আগে" else "15d ago") to DonationCountdownEngine.daysAgoIsoDate(15),
                        (if (strings.isBangla) "৪৫ দিন আগে" else "45d ago") to DonationCountdownEngine.daysAgoIsoDate(45),
                        (if (strings.isBangla) "৯০ দিন আগে" else "90d ago") to DonationCountdownEngine.daysAgoIsoDate(90)
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(datePresets) { (label, presetDate) ->
                        val isSelected = donationDate == presetDate
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) MbdCrimson else MbdChipUnselected,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    donationDate = presetDate
                                    validationError = null
                                }
                        ) {
                            Text(
                                text = "$label ($presetDate)",
                                color = if (isSelected) Color.White else MbdTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = donationDate,
                        onValueChange = {
                            donationDate = it
                            validationError = null
                        },
                        label = { Text(if (strings.isBangla) "তারিখ (YYYY-MM-DD) *" else "Date (YYYY-MM-DD) *") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("dialog_donation_date_input")
                    )

                    OutlinedTextField(
                        value = donationTime,
                        onValueChange = {
                            donationTime = it
                            validationError = null
                        },
                        label = { Text(if (strings.isBangla) "সময় (HH:mm) *" else "Time (HH:mm) *") },
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("dialog_donation_time_input")
                    )
                }

                // 3. Optional Hospital, Procedure Confirmation, Location, Recipient & Notes
                OutlinedTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = {
                        Text(
                            if (strings.isBangla) "হাসপাতাল বা ডোনেশন সেন্টার (ঐচ্ছিক)" else "Hospital or Donation Center (Optional)"
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.LocalHospital, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_hospital_input")
                )

                OutlinedTextField(
                    value = procedureConfirmation,
                    onValueChange = { procedureConfirmation = it },
                    label = { Text(strings.procedureConfirmationLabel) },
                    leadingIcon = { Icon(Icons.Default.FactCheck, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_procedure_confirmation_input")
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(strings.locationLabel) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = {
                        Text(
                            if (strings.isBangla) "রোগীর নাম (ঐচ্ছিক)" else "Recipient Name (Optional)"
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_notes_input")
                )

                // Verification Status Option (Confirmed vs Pending Admin Verification)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, MbdBorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (strings.isBangla) "যাচাইকরণ অবস্থা (Verification)" else "Verification Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MbdTextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = submitAsVerified,
                                onClick = { submitAsVerified = true },
                                colors = RadioButtonDefaults.colors(selectedColor = MbdAvailableGreen)
                            )
                            Text(
                                text = if (strings.isBangla) {
                                    "নিশ্চিত রক্তদান (সাথে সাথে কাউন্টডাউন আপডেট হবে)"
                                } else {
                                    "Confirmed Donation (Updates countdown immediately)"
                                },
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { submitAsVerified = true }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !submitAsVerified,
                                onClick = { submitAsVerified = false },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFE65100))
                            )
                            Text(
                                text = if (strings.isBangla) {
                                    "যাচাইকরণ অপেক্ষমাণ (অ্যাডমিন অনুমোদনের জন্য)"
                                } else {
                                    "Pending Verification (Requires Admin Approval)"
                                },
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { submitAsVerified = false }
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MbdCrimson,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("dialog_validation_error")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanDate = donationDate.trim()
                    val cleanTime = donationTime.trim().ifBlank { "12:00" }
                    if (!DonationCountdownEngine.isValidIsoDate(cleanDate)) {
                        validationError = if (strings.isBangla) {
                            "সঠিক ফরম্যাটে তারিখ লিখুন (YYYY-MM-DD)"
                        } else {
                            "Please enter a valid date in YYYY-MM-DD format"
                        }
                        return@Button
                    }
                    if (!DonationCountdownEngine.isValidTime(cleanTime)) {
                        validationError = if (strings.isBangla) {
                            "সঠিক ফরম্যাটে সময় লিখুন (HH:mm, যেমন: 14:30)"
                        } else {
                            "Please enter a valid time in HH:mm format (e.g., 14:30)"
                        }
                        return@Button
                    }
                    if (selectedTypeCode == "other" && customDonationTypeName.isBlank()) {
                        validationError = if (strings.isBangla) {
                            "অন্যান্য রক্তদানের ক্ষেত্রে রক্তদানের ধরন বা নাম উল্লেখ করুন"
                        } else {
                            "Please specify the custom donation type name for 'Other'"
                        }
                        return@Button
                    }
                    // Prevent accidental duplicate donation records
                    val isDuplicate = existingRecords.any { rec ->
                        rec.donationDate.trim() == cleanDate &&
                            rec.donationType.trim() == selectedTypeCode &&
                            rec.verificationStatus != "rejected"
                    }
                    if (isDuplicate) {
                        validationError = if (strings.isBangla) {
                            "এই তারিখে ($cleanDate) একই ধরনের রক্তদানের রেকর্ড ইতিমধ্যে যুক্ত রয়েছে।"
                        } else {
                            "A donation record for this date ($cleanDate) and donation type already exists."
                        }
                        return@Button
                    }

                    onConfirmRecord(
                        cleanDate,
                        cleanTime,
                        selectedTypeCode,
                        customDonationTypeName.trim(),
                        procedureConfirmation.trim(),
                        effectiveIntervalDays,
                        activeRule.ruleVersion.ifBlank { "v1.0" },
                        hospitalName.trim(),
                        location.trim().ifBlank { "Mirpur, Dhaka" },
                        recipientName.trim(),
                        bloodGroup,
                        notes.trim(),
                        if (submitAsVerified) "verified" else "pending"
                    )
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                modifier = Modifier.testTag("confirm_record_donation_button")
            ) {
                Text(if (strings.isBangla) "রক্তদান যোগ করুন" else "Submit Donation Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (strings.isBangla) "বাতিল" else "Cancel")
            }
        }
    )
}

/**
 * Admin Date & Eligibility Correction Dialog (Section 7 of Prompt).
 * Allows an authorized administrator to correct an incorrect donation date, donation type, or eligibility date.
 */
@Composable
fun AdminCorrectDonorDatesDialog(
    strings: MbdStrings,
    donorName: String,
    initialLastDonationDate: String,
    initialNextEligibleDate: String,
    initialIntervalDays: Long,
    initialTotalDonations: Long,
    onDismiss: () -> Unit,
    onConfirmCorrection: (
        correctedLastDonationDate: String,
        correctedNextEligibleDate: String,
        correctedIntervalDays: Long,
        correctedTotalDonations: Long
    ) -> Unit
) {
    var lastDate by remember { mutableStateOf(initialLastDonationDate.ifBlank { DonationCountdownEngine.todayIsoDate() }) }
    var intervalText by remember { mutableStateOf(initialIntervalDays.coerceIn(0L, 365L).toString()) }
    var customNextDate by remember {
        mutableStateOf(
            initialNextEligibleDate.ifBlank {
                if (initialIntervalDays <= 0L) "UNDECIDED"
                else DonationCountdownEngine.computeNextEligibleDateString(lastDate, initialIntervalDays)
            }
        )
    }
    var totalDonationsText by remember { mutableStateOf(initialTotalDonations.coerceAtLeast(0L).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MbdCrimson,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.adminCorrectDatesBtn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (strings.isBangla) {
                        "রক্তদাতা: $donorName • ভুল রক্তদানের তারিখ বা পরবর্তী সম্ভাব্য তারিখ সংশোধন করুন"
                    } else {
                        "Donor: $donorName • Correct donation date or next eligible date"
                    },
                    fontSize = 12.sp,
                    color = MbdTextSecondary
                )

                OutlinedTextField(
                    value = lastDate,
                    onValueChange = {
                        lastDate = it
                        val iv = intervalText.toLongOrNull()?.coerceIn(0L, 365L) ?: 90L
                        val autoNext = if (iv <= 0L) "UNDECIDED" else DonationCountdownEngine.computeNextEligibleDateString(it, iv)
                        if (autoNext.isNotBlank()) {
                            customNextDate = autoNext
                        }
                    },
                    label = { Text(strings.lastDonationDateLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = intervalText,
                    onValueChange = {
                        intervalText = it.filter { ch -> ch.isDigit() }
                        val iv = intervalText.toLongOrNull()?.coerceIn(0L, 365L) ?: 0L
                        customNextDate = if (iv <= 0L) {
                            "UNDECIDED"
                        } else {
                            DonationCountdownEngine.computeNextEligibleDateString(lastDate, iv)
                        }
                    },
                    label = { Text("${strings.eligibilityIntervalLabel} (0 = অনির্ণীত)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customNextDate,
                    onValueChange = { customNextDate = it },
                    label = { Text("${strings.nextEligibleDateLabel} (YYYY-MM-DD বা UNDECIDED)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalDonationsText,
                    onValueChange = { totalDonationsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.totalDonationsLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val iv = intervalText.toLongOrNull()?.coerceIn(0L, 365L) ?: 0L
                    val resolvedNext = customNextDate.trim().ifBlank {
                        if (iv <= 0L) "UNDECIDED" else DonationCountdownEngine.computeNextEligibleDateString(lastDate, iv)
                    }
                    val total = totalDonationsText.toLongOrNull()?.coerceAtLeast(0L) ?: initialTotalDonations
                    onConfirmCorrection(lastDate.trim(), resolvedNext, iv, total)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson)
            ) {
                Text(if (strings.isBangla) "সংশোধন সংরক্ষণ করুন" else "Save Correction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (strings.isBangla) "বাতিল" else "Cancel")
            }
        }
    )
}

/**
 * Configurable Medical Eligibility Rules Engine Dialog for Authorized Administrators (Sections 4, 5 & 8).
 * Allows admins to configure intervals, cross-donation gaps, verification state, and rule versions
 * for Whole Blood, Platelets, Plasma, and Other/Custom donations, and automatically recalculates
 * target dates for affected records.
 */
@Composable
fun AdminEligibilityRulesDialog(
    strings: MbdStrings,
    currentRules: Map<String, EligibilityRule>,
    onDismiss: () -> Unit,
    onSaveRuleAndRecalculate: (EligibilityRule) -> Unit
) {
    var selectedType by remember { mutableStateOf("whole_blood") }
    val activeRule = remember(selectedType, currentRules) {
        currentRules[selectedType]
            ?: DEFAULT_ELIGIBILITY_RULES[selectedType]
            ?: DEFAULT_ELIGIBILITY_RULES["whole_blood"]!!
    }

    var intervalText by remember(activeRule) { mutableStateOf(activeRule.intervalDays.toString()) }
    var crossGapText by remember(activeRule) { mutableStateOf(activeRule.crossTypeMinGapDays.toString()) }
    var isVerifiedRule by remember(activeRule) { mutableStateOf(activeRule.isVerifiedRule) }
    var ruleVersionText by remember(activeRule) {
        mutableStateOf(DonationCountdownEngine.bumpRuleVersion(activeRule.ruleVersion))
    }
    var guidanceBnText by remember(activeRule) { mutableStateOf(activeRule.medicalGuidanceBn) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Rule,
                    contentDescription = null,
                    tint = MbdCrimson,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.adminConfigureRulesBtn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (strings.isBangla) {
                        "রক্তদানের ধরন অনুযায়ী চিকিৎসাগত বিরতি ও নিয়ম পরিবর্তন করলে সংশ্লিষ্ট ডোনারদের পরবর্তী সম্ভাব্য তারিখ স্বয়ংক্রিয়ভাবে পুনঃগণনা (Recalculate) হবে।"
                    } else {
                        "Updating a medical eligibility rule will increment its rule version and automatically recalculate target dates."
                    },
                    fontSize = 11.sp,
                    color = MbdTextSecondary
                )

                // Type selector tabs
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DONATION_TYPE_OPTIONS.forEach { opt ->
                        val isSelected = selectedType == opt.code
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MbdCrimsonLight.copy(alpha = 0.7f) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isSelected) MbdCrimson else MbdBorderGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedType = opt.code }
                                .testTag("admin_rule_type_${opt.code}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedType = opt.code },
                                    colors = RadioButtonDefaults.colors(selectedColor = MbdCrimson),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = opt.labelBn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MbdTextPrimary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MbdBorderGray)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { intervalText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (strings.isBangla) "বিরতি (দিন)" else "Interval (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_rule_interval_input")
                    )

                    OutlinedTextField(
                        value = crossGapText,
                        onValueChange = { crossGapText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (strings.isBangla) "ক্রস-ডোনেশন বিরতি" else "Cross-Type Gap") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = ruleVersionText,
                    onValueChange = { ruleVersionText = it },
                    label = { Text(if (strings.isBangla) "রুল সংস্করণ (Rule Version)" else "Rule Version") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isVerifiedRule = !isVerifiedRule }
                ) {
                    Checkbox(
                        checked = isVerifiedRule,
                        onCheckedChange = { isVerifiedRule = it },
                        colors = CheckboxDefaults.colors(checkedColor = MbdAvailableGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (strings.isBangla) {
                            "অনুমোদিত নিয়ম সক্রিয় করুন (কাউন্টডাউন চালু করার জন্য)"
                        } else {
                            "Mark rule as verified & active for countdown calculation"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MbdTextPrimary
                    )
                }

                OutlinedTextField(
                    value = guidanceBnText,
                    onValueChange = { guidanceBnText = it },
                    label = { Text(if (strings.isBangla) "চিকিৎসাগত নির্দেশনা (বাংলা)" else "Medical Guidance (Bangla)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val iv = intervalText.toLongOrNull()?.coerceIn(0L, 365L) ?: 0L
                    val crossGap = crossGapText.toLongOrNull()?.coerceIn(0L, 180L) ?: 0L
                    val updatedRule = activeRule.copy(
                        donationType = selectedType,
                        intervalDays = iv,
                        crossTypeMinGapDays = crossGap,
                        isVerifiedRule = isVerifiedRule && iv > 0L,
                        ruleVersion = ruleVersionText.trim().ifBlank { "v1.1" },
                        medicalGuidanceBn = guidanceBnText.trim().ifBlank { activeRule.medicalGuidanceBn }
                    )
                    onSaveRuleAndRecalculate(updatedRule)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                modifier = Modifier.testTag("admin_save_rule_button")
            ) {
                Text(if (strings.isBangla) "নিয়ম সংরক্ষণ ও পুনঃগণনা করুন" else "Save Rule & Recalculate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (strings.isBangla) "বন্ধ করুন" else "Close")
            }
        }
    )
}

package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.LaunchedEffect
import com.example.data.model.BLOOD_GROUPS
import com.example.data.model.DonorProfile
import com.example.data.model.MIRPUR_LOCATIONS
import com.example.ui.i18n.MbdStrings
import com.example.ui.theme.MbdAvailableGreen
import com.example.ui.theme.MbdBorderGray
import com.example.ui.theme.MbdChipUnselected
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdCrimsonLight
import com.example.ui.theme.MbdSoftPinkAlert
import com.example.ui.theme.MbdTextHint
import com.example.ui.theme.MbdTextPrimary
import com.example.ui.theme.MbdTextSecondary
import com.example.ui.util.MbdNotificationHelper

@Composable
fun RequestBloodScreen(
    strings: MbdStrings,
    defaultPhoneNumber: String,
    isSaving: Boolean,
    preselectedDonor: DonorProfile? = null,
    onClearPreselectedDonor: () -> Unit = {},
    onBack: () -> Unit,
    onSubmitRequest: (
        bloodGroup: String,
        hospitalName: String,
        location: String,
        patientName: String,
        contactNumber: String,
        neededDateTime: String,
        bagsNeeded: Long,
        urgency: String,
        notes: String,
        donationType: String
    ) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current

    var selectedBloodGroup by remember(preselectedDonor?.id) {
        mutableStateOf(preselectedDonor?.bloodGroup ?: "")
    }
    var showBloodGroupMenu by remember { mutableStateOf(false) }

    var selectedDonationType by remember { mutableStateOf("whole_blood") }

    var hospitalName by remember { mutableStateOf("") }
    var location by remember(preselectedDonor?.id) {
        mutableStateOf(preselectedDonor?.area ?: "")
    }
    var showLocationMenu by remember { mutableStateOf(false) }

    var patientName by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf(defaultPhoneNumber) }
    var neededDateTime by remember { mutableStateOf("") }
    var bagsNeeded by remember { mutableLongStateOf(1L) }
    var urgency by remember { mutableStateOf("Emergency") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(preselectedDonor) {
        if (preselectedDonor != null) {
            if (selectedBloodGroup.isBlank()) {
                selectedBloodGroup = preselectedDonor.bloodGroup
            }
            if (location.isBlank() && preselectedDonor.area.isNotBlank()) {
                location = preselectedDonor.area
            }
        }
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && selectedBloodGroup.isNotBlank() && hospitalName.isNotBlank()) {
            MbdNotificationHelper.showBloodRequestNotification(
                context = context,
                bloodGroup = selectedBloodGroup,
                hospitalName = hospitalName,
                location = location,
                isBangla = strings.isBangla
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Red Top App Bar matching Reference Screen 4
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
                    modifier = Modifier.testTag("request_blood_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = if (preselectedDonor != null) {
                        strings.sendBloodRequestBtn
                    } else {
                        strings.requestBlood
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Associated Donor Card when opened from a Donor Profile Details Page
            if (preselectedDonor != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MbdAvailableGreen.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MbdAvailableGreen.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("associated_donor_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MbdCrimson,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = preselectedDonor.bloodGroup,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = MbdAvailableGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (strings.isBangla) {
                                        "নির্বাচিত রক্তদাতা (যুক্ত করা হয়েছে)"
                                    } else {
                                        "Associated Blood Donor"
                                    },
                                    fontSize = 11.sp,
                                    color = MbdAvailableGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = preselectedDonor.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MbdTextPrimary
                            )
                            Text(
                                text = preselectedDonor.area,
                                fontSize = 12.sp,
                                color = MbdTextSecondary
                            )
                        }
                        IconButton(onClick = onClearPreselectedDonor) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear selected donor",
                                tint = MbdTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Soft Pink Banner matching Reference Screen 4
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MbdSoftPinkAlert,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MbdCrimson,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = strings.requestBannerText,
                        color = MbdCrimson,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (validationError != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MbdCrimson.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError!!,
                        color = MbdCrimson,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. Blood Group * (Dropdown)
            Column {
                RequiredFieldLabel(text = strings.bloodGroupLabel)
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedTextField(
                        value = selectedBloodGroup,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text(
                                text = strings.selectBloodGroup,
                                color = MbdTextHint,
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showBloodGroupMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MbdTextSecondary
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MbdCrimson,
                            unfocusedBorderColor = MbdBorderGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBloodGroupMenu = true }
                            .testTag("request_blood_group_dropdown")
                    )
                    // Transparent overlay to trigger dropdown reliably on click
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showBloodGroupMenu = true }
                    )
                    DropdownMenu(
                        expanded = showBloodGroupMenu,
                        onDismissRequest = { showBloodGroupMenu = false }
                    ) {
                        BLOOD_GROUPS.forEach { bg ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = bg,
                                        fontWeight = FontWeight.Bold,
                                        color = MbdCrimson
                                    )
                                },
                                onClick = {
                                    selectedBloodGroup = bg
                                    showBloodGroupMenu = false
                                    validationError = null
                                }
                            )
                        }
                    }
                }
            }

            // 1b. Required Donation Type *
            Column {
                RequiredFieldLabel(text = strings.requiredDonationTypeLabel)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        "whole_blood" to if (strings.isBangla) "সম্পূর্ণ রক্ত" else "Whole Blood",
                        "platelets" to if (strings.isBangla) "প্লেটলেট" else "Platelets",
                        "plasma" to if (strings.isBangla) "প্লাজমা" else "Plasma"
                    )
                    types.forEach { (typeKey, label) ->
                        val isSelected = selectedDonationType == typeKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MbdCrimson else MbdChipUnselected,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MbdCrimson else MbdBorderGray
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDonationType = typeKey }
                                .testTag("request_donation_type_$typeKey")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MbdTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 2. Hospital Name *
            Column {
                RequiredFieldLabel(text = strings.hospitalNameLabel)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = hospitalName,
                    onValueChange = {
                        hospitalName = it
                        validationError = null
                    },
                    placeholder = {
                        Text(
                            text = strings.enterHospitalName,
                            color = MbdTextHint,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MbdCrimson,
                        unfocusedBorderColor = MbdBorderGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_hospital_input")
                )
            }

            // 3. Location * (with leading Location pin & Mirpur suggestions dropdown)
            Column {
                RequiredFieldLabel(text = strings.locationLabel)
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            validationError = null
                        },
                        placeholder = {
                            Text(
                                text = strings.selectLocation,
                                color = MbdTextHint,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MbdTextSecondary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showLocationMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Choose Mirpur area",
                                    tint = MbdTextSecondary
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MbdCrimson,
                            unfocusedBorderColor = MbdBorderGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_location_input")
                    )

                    DropdownMenu(
                        expanded = showLocationMenu,
                        onDismissRequest = { showLocationMenu = false }
                    ) {
                        MIRPUR_LOCATIONS.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc) },
                                onClick = {
                                    location = loc
                                    showLocationMenu = false
                                    validationError = null
                                }
                            )
                        }
                    }
                }
            }

            // 4. Patient Name *
            Column {
                RequiredFieldLabel(text = strings.patientNameLabel)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = patientName,
                    onValueChange = {
                        patientName = it
                        validationError = null
                    },
                    placeholder = {
                        Text(
                            text = strings.enterPatientName,
                            color = MbdTextHint,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MbdCrimson,
                        unfocusedBorderColor = MbdBorderGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_patient_input")
                )
            }

            // 5. Contact Number * (with leading Call icon)
            Column {
                RequiredFieldLabel(text = strings.contactNumberLabel)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = {
                        contactNumber = it
                        validationError = null
                    },
                    placeholder = {
                        Text(
                            text = strings.enterPhoneNumber,
                            color = MbdTextHint,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = MbdTextSecondary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MbdCrimson,
                        unfocusedBorderColor = MbdBorderGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_phone_input")
                )
            }

            // 6. Blood Needed Date & Time
            Column {
                Text(
                    text = strings.neededDateTimeLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MbdTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = neededDateTime,
                    onValueChange = { neededDateTime = it },
                    placeholder = {
                        Text(
                            text = strings.enterNeededDateTime,
                            color = MbdTextHint,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MbdCrimson,
                        unfocusedBorderColor = MbdBorderGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_needed_datetime_input")
                )
            }

            // 7. Bags Needed & Urgency Level
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        text = strings.bagsNeededLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MbdTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1L, 2L, 3L, 4L, 5L).forEach { count ->
                            val selected = bagsNeeded == count
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) MbdCrimson else MbdChipUnselected,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { bagsNeeded = count }
                            ) {
                                Text(
                                    text = count.toString(),
                                    color = if (selected) Color.White else MbdTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = strings.urgencyLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MbdTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Emergency", "Urgent", "Standard").forEach { level ->
                            val selected = urgency == level
                            val label = if (strings.isBangla) {
                                when (level) {
                                    "Emergency" -> "অতি জরুরি"
                                    "Urgent" -> "জরুরি"
                                    else -> "সাধারণ"
                                }
                            } else {
                                level
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) MbdCrimson else MbdChipUnselected,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { urgency = level }
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) Color.White else MbdTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 8. Additional Information / Notes
            Column {
                Text(
                    text = strings.notesLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MbdTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = {
                        Text(
                            text = strings.enterNotes,
                            color = MbdTextHint,
                            fontSize = 14.sp
                        )
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MbdCrimson,
                        unfocusedBorderColor = MbdBorderGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_notes_input")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Submit Request Button matching Reference Screen 4
            Button(
                onClick = {
                    val bg = selectedBloodGroup.trim()
                    if (bg.isEmpty() || hospitalName.isBlank() || location.isBlank() ||
                        patientName.isBlank() || contactNumber.trim().length < 6
                    ) {
                        validationError = if (strings.isBangla) {
                            "অনুগ্রহ করে রক্তের গ্রুপ, হাসপাতাল, এলাকা, রোগীর নাম এবং সঠিক মোবাইল নম্বর পূরণ করুন।"
                        } else {
                            "Please select a blood group and fill in all required (*) fields."
                        }
                        return@Button
                    }

                    onSubmitRequest(
                        bg,
                        hospitalName,
                        location,
                        patientName,
                        contactNumber,
                        neededDateTime,
                        bagsNeeded,
                        urgency,
                        notes,
                        selectedDonationType
                    )

                    // Trigger or request notification permission for emergency blood request alert
                    if (MbdNotificationHelper.hasNotificationPermission(context)) {
                        MbdNotificationHelper.showBloodRequestNotification(
                            context = context,
                            bloodGroup = bg,
                            hospitalName = hospitalName,
                            location = location,
                            isBangla = strings.isBangla
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MbdCrimson,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_blood_request_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.submitRequest,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RequiredFieldLabel(text: String) {
    Text(
        text = buildAnnotatedString {
            append(text)
            append(" ")
            withStyle(style = SpanStyle(color = MbdCrimson, fontWeight = FontWeight.Bold)) {
                append("*")
            }
        },
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = MbdTextPrimary
    )
}

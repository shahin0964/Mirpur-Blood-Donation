package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BLOOD_GROUPS
import com.example.data.model.DonorProfile
import com.example.data.model.MIRPUR_LOCATIONS
import com.example.ui.components.AvailabilityStatusBadge
import com.example.ui.components.CompactDonorCardCountdownBadge
import com.example.ui.i18n.MbdStrings
import com.example.ui.theme.MbdAvailableGreen
import com.example.ui.theme.MbdBorderGray
import com.example.ui.theme.MbdCallGreen
import com.example.ui.theme.MbdChipUnselected
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdCrimsonLight
import com.example.ui.theme.MbdSurface
import com.example.ui.theme.MbdTextHint
import com.example.ui.theme.MbdTextPrimary
import com.example.ui.theme.MbdTextSecondary
import com.example.ui.util.dialPhoneNumber
import com.example.ui.viewmodel.UiState

@Composable
fun FindDonorsScreen(
    strings: MbdStrings,
    donorsState: UiState<List<DonorProfile>>,
    searchQuery: String,
    selectedBloodGroup: String,
    availableOnly: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectBloodGroup: (String) -> Unit,
    onToggleAvailableOnly: (Boolean) -> Unit,
    onBack: () -> Unit,
    onRegisterDonorClick: () -> Unit,
    onDonorClick: (DonorProfile) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var showLocationMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Red Top App Bar matching Reference Screen 3
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
                    modifier = Modifier.testTag("find_donors_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = strings.findDonors,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = onRegisterDonorClick,
                    modifier = Modifier.testTag("find_donors_register_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = strings.registerAsDonorBtn,
                        tint = Color.White
                    )
                }
            }
        }

        // Search & Blood Group Filter Container matching Reference Screen 3
        Surface(
            color = MbdSurface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = strings.searchPlaceholder,
                                color = MbdTextHint,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MbdTextSecondary
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MbdTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { showLocationMenu = true },
                                    modifier = Modifier.testTag("mirpur_location_filter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Select Mirpur location",
                                        tint = MbdCrimson
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MbdCrimson,
                            unfocusedBorderColor = MbdBorderGray,
                            focusedContainerColor = MbdSurface,
                            unfocusedContainerColor = MbdSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("donor_search_input")
                    )

                    DropdownMenu(
                        expanded = showLocationMenu,
                        onDismissRequest = { showLocationMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (strings.isBangla) "সকল এলাকা (মিরপুর ও ঢাকা)" else "All Locations") },
                            onClick = {
                                onSearchQueryChange("")
                                showLocationMenu = false
                            }
                        )
                        MIRPUR_LOCATIONS.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc) },
                                onClick = {
                                    onSearchQueryChange(loc.substringBefore(","))
                                    showLocationMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Blood Group Filter Chips (All, A+, A-, B+, B-, AB+, AB-, O+, O-) matching Reference Screen 3
                val filterGroups = remember { listOf("All") + BLOOD_GROUPS }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(filterGroups) { group ->
                        val isSelected = selectedBloodGroup == group
                        val label = if (group == "All") strings.allFilter else group
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) MbdCrimson else MbdChipUnselected,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onSelectBloodGroup(group) }
                                .testTag("blood_group_chip_$group")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else MbdTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Available Only Filter Chip
                    item {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (availableOnly) MbdAvailableGreen else MbdChipUnselected,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onToggleAvailableOnly(!availableOnly) }
                                .testTag("available_only_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = if (availableOnly) Color.White else MbdAvailableGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.availableOnly,
                                    color = if (availableOnly) Color.White else MbdTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Donors List
        when (donorsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MbdCrimson)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = donorsState.message,
                        color = MbdCrimson
                    )
                }
            }
            is UiState.Success -> {
                val filteredDonors = remember(donorsState.data, searchQuery, selectedBloodGroup, availableOnly) {
                    val q = searchQuery.trim().lowercase()
                    donorsState.data.filter { donor ->
                        val matchesGroup = selectedBloodGroup == "All" ||
                            donor.bloodGroup.equals(selectedBloodGroup, ignoreCase = true)
                        val matchesAvailability = !availableOnly || donor.isAvailable
                        val matchesQuery = q.isEmpty() ||
                            donor.fullName.lowercase().contains(q) ||
                            donor.bloodGroup.lowercase().contains(q) ||
                            donor.area.lowercase().contains(q) ||
                            donor.address.lowercase().contains(q)
                        matchesGroup && matchesAvailability && matchesQuery
                    }
                }

                if (filteredDonors.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MbdSurface),
                            border = BorderStroke(1.dp, MbdBorderGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                BloodDropBadgeIcon(modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = strings.noDonorsFound,
                                    color = MbdTextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onRegisterDonorClick,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson),
                                    modifier = Modifier.testTag("empty_state_register_donor_button")
                                ) {
                                    Text(text = strings.registerAsDonorBtn)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("donors_lazy_list"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredDonors, key = { it.id.ifBlank { it.userId } }) { donor ->
                            DonorListItemCard(
                                donor = donor,
                                strings = strings,
                                onCardClick = { onDonorClick(donor) },
                                onCallClick = { dialPhoneNumber(context, donor.phoneNumber) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonorListItemCard(
    donor: DonorProfile,
    strings: MbdStrings,
    onCardClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val donorKey = donor.id.ifBlank { donor.userId }
    // Using Material 3 clickable Card overload for built-in ripple & subtle elevation animation
    Card(
        onClick = onCardClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MbdSurface),
        border = BorderStroke(1.dp, MbdBorderGray.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.5.dp,
            pressedElevation = 4.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("donor_card_$donorKey")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Donor Avatar with Availability Indicator Dot
            Box(modifier = Modifier.size(58.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MbdCrimsonLight,
                    border = BorderStroke(1.5.dp, MbdCrimson.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val initials = donor.fullName
                            .trim()
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercase() }
                            .ifEmpty { "D" }
                        Text(
                            text = initials,
                            color = MbdCrimson,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(if (donor.isAvailable) MbdAvailableGreen else Color.Gray)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Donor Details Column matching Reference Screen 3 (Avatar, Name, Blood Group, Availability, Location, History)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = donor.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MbdTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Blood Drop + Group + Availability Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MbdCrimson,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = donor.bloodGroup,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MbdTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AvailabilityStatusBadge(
                        availabilityStatus = donor.resolvedAvailabilityStatus(),
                        isAvailableFallback = donor.isAvailable,
                        strings = strings,
                        compact = true
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Location Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MbdTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = donor.area.ifBlank { donor.address },
                        fontSize = 12.sp,
                        color = MbdTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (donor.lastDonationDate.isNotBlank() || donor.totalDonations > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            if (donor.totalDonations > 0) {
                                append(if (strings.isBangla) "${donor.totalDonations} বার রক্তদান" else "${donor.totalDonations} donations")
                            }
                            if (donor.lastDonationDate.isNotBlank() && donor.showDonationHistoryPublicly) {
                                if (isNotEmpty()) append(" • ")
                                append(donor.lastDonationDate)
                            }
                        },
                        fontSize = 11.sp,
                        color = MbdTextSecondary.copy(alpha = 0.85f)
                    )
                }

                if (donor.lastDonationDate.isNotBlank() && donor.showDonationHistoryPublicly) {
                    Spacer(modifier = Modifier.height(4.dp))
                    CompactDonorCardCountdownBadge(
                        lastDonationDate = donor.lastDonationDate,
                        nextEligibleDate = donor.nextEligibleDate,
                        eligibilityIntervalDays = donor.eligibilityIntervalDays,
                        donationType = donor.donationType,
                        showDonationHistoryPublicly = donor.showDonationHistoryPublicly,
                        strings = strings
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Green Call Button matching Reference Screen 3 — initiates phone call separately without opening profile page
            Button(
                onClick = onCallClick,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MbdCallGreen,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("call_donor_button_$donorKey")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = strings.callBtn,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

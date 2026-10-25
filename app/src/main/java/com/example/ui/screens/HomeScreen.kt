package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BloodRequest
import com.example.data.model.DonorProfile
import com.example.ui.i18n.MbdStrings
import com.example.ui.theme.MbdAvailableGreen
import com.example.ui.theme.MbdBannerPinkEnd
import com.example.ui.theme.MbdBannerPinkStart
import com.example.ui.theme.MbdBorderGray
import com.example.ui.theme.MbdCallGreen
import com.example.ui.theme.MbdCrimson
import com.example.ui.theme.MbdCrimsonDark
import com.example.ui.theme.MbdCrimsonLight
import com.example.ui.theme.MbdSurface
import com.example.ui.theme.MbdTextPrimary
import com.example.ui.theme.MbdTextSecondary
import com.example.ui.util.dialPhoneNumber
import com.example.ui.util.formatRelativeTime
import com.example.ui.viewmodel.MbdScreen
import com.example.ui.viewmodel.UiState

@Composable
fun HomeScreen(
    strings: MbdStrings,
    currentUserId: String,
    myDonorState: UiState<DonorProfile?>,
    bloodRequestsState: UiState<List<BloodRequest>>,
    onOpenDrawer: () -> Unit,
    onToggleLanguage: () -> Unit,
    onNavigate: (MbdScreen) -> Unit,
    onOpenRequestDetails: (BloodRequest) -> Unit,
    onOpenDonorLiveMap: (BloodRequest) -> Unit,
    onMarkRequestStatus: (String, String) -> Unit,
    onDeleteRequest: (String) -> Unit
) {
    val openRequestsCount = when (bloodRequestsState) {
        is UiState.Success -> bloodRequestsState.data.count { it.status == "open" }
        else -> 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Bar matching Reference Screen 2
        Surface(
            color = MbdSurface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("home_drawer_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open navigation menu",
                        tint = MbdTextPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = strings.appTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MbdTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = strings.appSubtitle,
                        fontSize = 12.sp,
                        color = MbdTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Language Toggle Chip (বাংলা / EN)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MbdCrimsonLight,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onToggleLanguage() }
                        .testTag("home_language_toggle")
                ) {
                    Text(
                        text = if (strings.isBangla) "EN" else "বাংলা",
                        color = MbdCrimson,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { onNavigate(MbdScreen.NOTIFICATIONS) },
                    modifier = Modifier.testTag("home_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (openRequestsCount > 0) {
                                Badge(
                                    containerColor = MbdCrimson,
                                    contentColor = Color.White
                                ) {
                                    Text(text = openRequestsCount.coerceAtMost(99).toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = strings.notifications,
                            tint = MbdCrimson
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Banner Card ("Be a Hero / Donate Blood") matching Reference Screen 2
            item {
                HeroDonateBannerCard(
                    strings = strings,
                    myDonorProfile = (myDonorState as? UiState.Success)?.data,
                    onDonateNowClick = { onNavigate(MbdScreen.PROFILE) }
                )
            }

            // 2. 2x2 Quick Action Cards Grid matching Reference Screen 2
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            title = strings.findDonors,
                            subtitle = strings.findDonorsSub,
                            icon = Icons.Default.WaterDrop,
                            onClick = { onNavigate(MbdScreen.FIND_DONORS) },
                            testTag = "quick_action_find_donors",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            title = strings.requestBlood,
                            subtitle = strings.requestBloodSub,
                            icon = Icons.Default.CalendarToday,
                            onClick = { onNavigate(MbdScreen.REQUEST_BLOOD) },
                            testTag = "quick_action_request_blood",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            title = strings.myDonations,
                            subtitle = strings.myDonationsSub,
                            icon = Icons.Default.Favorite,
                            isHeartPulse = true,
                            onClick = { onNavigate(MbdScreen.MY_DONATIONS) },
                            testTag = "quick_action_my_donations",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            title = strings.profile,
                            subtitle = strings.profileSub,
                            icon = Icons.Default.Person,
                            onClick = { onNavigate(MbdScreen.PROFILE) },
                            testTag = "quick_action_profile",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Blood Donation Awareness & Eligibility Banner
            item {
                Card(
                    onClick = { onNavigate(MbdScreen.AWARENESS) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MbdSurface),
                    border = BorderStroke(1.dp, MbdBorderGray),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_awareness_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MbdCrimsonLight,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = MbdCrimson,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.awarenessEligibility,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MbdTextPrimary
                            )
                            Text(
                                text = if (strings.isBangla) {
                                    "রক্তদানের শর্তাবলী, বিরতি ও রক্তের গ্রুপ সামঞ্জস্যতা জানুন"
                                } else {
                                    "Learn donor eligibility criteria, intervals & blood compatibility"
                                },
                                fontSize = 12.sp,
                                color = MbdTextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MbdCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 4. Recent Requests Header Row matching Reference Screen 2
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentRequests,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MbdTextPrimary
                    )
                    Text(
                        text = strings.viewAll,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MbdTextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigate(MbdScreen.REQUESTS_LIST) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("view_all_requests_button")
                    )
                }
            }

            // 5. Recent Requests List (Real Firestore Data)
            when (bloodRequestsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MbdCrimson)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MbdSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = bloodRequestsState.message,
                                color = MbdCrimson,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val activeRequests = bloodRequestsState.data.take(5)
                    if (activeRequests.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MbdSurface),
                                border = BorderStroke(1.dp, MbdBorderGray),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    BloodDropBadgeIcon(modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = strings.noRequestsYet,
                                        color = MbdTextSecondary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { onNavigate(MbdScreen.REQUEST_BLOOD) },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(containerColor = MbdCrimson)
                                    ) {
                                        Text(text = strings.createFirstRequest)
                                    }
                                }
                            }
                        }
                    } else {
                        items(activeRequests, key = { it.id }) { req ->
                            RecentRequestCard(
                                request = req,
                                strings = strings,
                                currentUserId = currentUserId,
                                onViewClick = { onOpenRequestDetails(req) },
                                onViewLiveLocationClick = { onOpenDonorLiveMap(req) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroDonateBannerCard(
    strings: MbdStrings,
    myDonorProfile: DonorProfile?,
    onDonateNowClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(MbdBannerPinkStart, MbdBannerPinkEnd)
                    )
                )
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.25f)) {
                    Text(
                        text = strings.heroTitle,
                        color = MbdCrimson,
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        lineHeight = 27.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.heroSubtitle,
                        color = MbdTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    if (myDonorProfile != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (myDonorProfile.isAvailable) {
                                MbdAvailableGreen.copy(alpha = 0.14f)
                            } else {
                                Color.Gray.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = "${myDonorProfile.bloodGroup} • ${if (myDonorProfile.isAvailable) strings.availableBadge else strings.unavailableBadge}",
                                color = if (myDonorProfile.isAvailable) MbdAvailableGreen else MbdTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onDonateNowClick,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MbdCrimson,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("hero_donate_now_button")
                    ) {
                        Text(
                            text = strings.donateNow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Hand holding glossy red blood drop illustration matching Reference Screen 2
                HeroHandBloodIllustration(
                    modifier = Modifier
                        .size(115.dp)
                        .padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
fun HeroHandBloodIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Glossy Red Blood Drop
        val dropPath = Path().apply {
            moveTo(w * 0.56f, h * 0.05f)
            cubicTo(
                w * 0.56f, h * 0.05f,
                w * 0.30f, h * 0.36f,
                w * 0.30f, h * 0.52f
            )
            cubicTo(
                w * 0.30f, h * 0.67f,
                w * 0.42f, h * 0.77f,
                w * 0.56f, h * 0.77f
            )
            cubicTo(
                w * 0.70f, h * 0.77f,
                w * 0.82f, h * 0.67f,
                w * 0.82f, h * 0.52f
            )
            cubicTo(
                w * 0.82f, h * 0.36f,
                w * 0.56f, h * 0.05f,
                w * 0.56f, h * 0.05f
            )
            close()
        }
        drawPath(
            path = dropPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFEA1D25), MbdCrimsonDark)
            )
        )

        // White glossy highlight arc on the right side of the blood drop
        val highlightArc = Path().apply {
            moveTo(w * 0.69f, h * 0.44f)
            cubicTo(
                w * 0.74f, h * 0.52f,
                w * 0.71f, h * 0.63f,
                w * 0.61f, h * 0.68f
            )
        }
        drawPath(
            path = highlightArc,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Warm Peach Hand cradling below the drop
        val handPath = Path().apply {
            moveTo(w * 0.06f, h * 0.69f)
            cubicTo(
                w * 0.22f, h * 0.67f,
                w * 0.40f, h * 0.82f,
                w * 0.66f, h * 0.82f
            )
            lineTo(w * 0.94f, h * 0.80f)
            cubicTo(
                w * 0.96f, h * 0.86f,
                w * 0.92f, h * 0.94f,
                w * 0.82f, h * 0.95f
            )
            lineTo(w * 0.36f, h * 0.95f)
            cubicTo(
                w * 0.22f, h * 0.95f,
                w * 0.10f, h * 0.84f,
                w * 0.04f, h * 0.75f
            )
            close()
        }
        drawPath(path = handPath, color = Color(0xFFF6A58E))
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    isHeartPulse: Boolean = false
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MbdSurface),
        border = BorderStroke(1.dp, MbdBorderGray.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MbdCrimson,
                    modifier = Modifier.size(28.dp)
                )
                if (isHeartPulse) {
                    // Subtle white ECG pulse line across heart icon matching Reference Screen 2
                    Canvas(modifier = Modifier.size(28.dp)) {
                        val w = size.width
                        val h = size.height
                        val pulse = Path().apply {
                            moveTo(w * 0.18f, h * 0.50f)
                            lineTo(w * 0.36f, h * 0.50f)
                            lineTo(w * 0.44f, h * 0.34f)
                            lineTo(w * 0.54f, h * 0.66f)
                            lineTo(w * 0.62f, h * 0.48f)
                            lineTo(w * 0.82f, h * 0.48f)
                        }
                        drawPath(
                            path = pulse,
                            color = Color.White,
                            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = MbdTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MbdTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecentRequestCard(
    request: BloodRequest,
    strings: MbdStrings,
    currentUserId: String = "",
    onViewClick: () -> Unit,
    onViewLiveLocationClick: () -> Unit = {}
) {
    val groupPositiveLabel = when {
        request.bloodGroup.endsWith("+") -> "${request.bloodGroup} (Positive)"
        request.bloodGroup.endsWith("-") -> "${request.bloodGroup} (Negative)"
        else -> request.bloodGroup
    }
    val isLiveForUser = request.liveSessionActive &&
        request.liveSessionExpiresAt > System.currentTimeMillis() &&
        (request.userId == currentUserId || request.authorizedDonorId == currentUserId)

    Card(
        onClick = onViewClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MbdSurface),
        border = BorderStroke(1.dp, MbdBorderGray.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_request_card_${request.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BloodDropBadgeIcon(modifier = Modifier.size(48.dp))

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = groupPositiveLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MbdTextPrimary
                        )
                        val resolvedStatus = request.resolvedStatus()
                        if (resolvedStatus != "pending") {
                            val statusLabel = when (resolvedStatus) {
                                "donor_contacted" -> strings.statusDonorContacted
                                "accepted" -> strings.statusAccepted
                                "completed" -> strings.statusCompleted
                                "cancelled" -> strings.statusCancelled
                                else -> strings.statusPending
                            }
                            val statusColor = when (resolvedStatus) {
                                "completed" -> MbdAvailableGreen
                                "cancelled" -> Color.Gray
                                "accepted" -> Color(0xFF2E7D32)
                                "donor_contacted" -> Color(0xFF0277BD)
                                else -> MbdCrimson
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = statusColor.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = statusLabel,
                                    color = statusColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${request.donationTypeLabel(strings.isBangla)} • ${request.hospitalName}, ${request.location}",
                        fontSize = 12.sp,
                        color = MbdTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatRelativeTime(request.createdAt, strings.isBangla),
                        fontSize = 11.sp,
                        color = MbdTextSecondary.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onViewClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MbdCrimson,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("view_request_button_${request.id}")
                ) {
                    Text(
                        text = strings.viewBtn,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isLiveForUser) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MbdAvailableGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MbdAvailableGreen.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onViewLiveLocationClick() }
                        .testTag("card_view_live_location_${request.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingLiveDot(color = MbdAvailableGreen, sizeDp = 10)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.viewLiveLocationBtn,
                                color = MbdAvailableGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MbdAvailableGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BloodDropBadgeIcon(modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFEDEFF3),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val drop = Path().apply {
                moveTo(w * 0.5f, h * 0.18f)
                cubicTo(w * 0.5f, h * 0.18f, w * 0.25f, h * 0.48f, w * 0.25f, h * 0.62f)
                cubicTo(w * 0.25f, h * 0.76f, w * 0.36f, h * 0.84f, w * 0.5f, h * 0.84f)
                cubicTo(w * 0.64f, h * 0.84f, w * 0.75f, h * 0.76f, w * 0.75f, h * 0.62f)
                cubicTo(w * 0.75f, h * 0.48f, w * 0.5f, h * 0.18f, w * 0.5f, h * 0.18f)
                close()
            }
            drawPath(drop, color = MbdCrimson)

            val highlight = Path().apply {
                moveTo(w * 0.62f, h * 0.55f)
                cubicTo(w * 0.66f, h * 0.62f, w * 0.62f, h * 0.72f, w * 0.53f, h * 0.75f)
            }
            drawPath(
                highlight,
                color = Color.White.copy(alpha = 0.85f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun BloodRequestDetailDialog(
    request: BloodRequest,
    strings: MbdStrings,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onCallClick: () -> Unit,
    onMarkFulfilled: () -> Unit,
    onDelete: () -> Unit
) {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BloodDropBadgeIcon(modifier = Modifier.size(38.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${request.bloodGroup} (${request.bagsNeeded} ${if (strings.isBangla) "ব্যাগ" else "Bag(s)"})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = request.urgency,
                        color = MbdCrimson,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow(
                    label = strings.patientNameLabel,
                    value = request.patientName
                )
                DetailRow(
                    label = strings.hospitalNameLabel,
                    value = request.hospitalName
                )
                DetailRow(
                    label = strings.locationLabel,
                    value = request.location
                )
                DetailRow(
                    label = strings.contactNumberLabel,
                    value = request.contactNumber
                )
                if (request.notes.isNotBlank()) {
                    DetailRow(
                        label = strings.notesLabel,
                        value = request.notes
                    )
                }

                if (isOwner) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (request.status == "open") {
                            OutlinedButton(
                                onClick = onMarkFulfilled,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MbdAvailableGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = strings.markFulfilledBtn, fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Request",
                                tint = MbdCrimson
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCallClick,
                colors = ButtonDefaults.buttonColors(containerColor = MbdCallGreen),
                shape = RoundedCornerShape(50)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "${strings.callBtn} (${request.contactNumber})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (strings.isBangla) "বন্ধ করুন" else "Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MbdTextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MbdTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

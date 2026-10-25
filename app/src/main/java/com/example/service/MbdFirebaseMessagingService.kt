package com.example.service

import android.util.Log
import com.example.ui.theme.MbdPreferencesManager
import com.example.ui.util.MbdNotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MbdFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM Message received from: ${message.from}")

        val data = message.data
        val notification = message.notification

        val isBangla = MbdPreferencesManager.getLanguage(applicationContext) == "bn"

        val type = data["type"] ?: "GENERAL"
        val requestId = data["requestId"] ?: ""
        val bloodGroup = data["bloodGroup"] ?: data["blood_group"] ?: "O+"
        val hospitalName = data["hospitalName"] ?: data["hospital_name"] ?: "Hospital"
        val location = data["location"] ?: "Dhaka"
        val requesterName = data["requesterName"] ?: data["requester_name"] ?: ""
        val title = notification?.title ?: data["title"] ?: ""
        val body = notification?.body ?: data["body"] ?: ""
        val newStatus = data["status"] ?: "accepted"
        val isEmergency = data["isEmergency"]?.toBoolean() ?: true

        when (type.uppercase()) {
            "BLOOD_REQUEST", "EMERGENCY_REQUEST" -> {
                MbdNotificationHelper.showBloodRequestNotification(
                    context = applicationContext,
                    requestId = requestId,
                    bloodGroup = bloodGroup,
                    hospitalName = hospitalName,
                    location = location,
                    requesterName = requesterName,
                    isBangla = isBangla
                )
            }
            "STATUS_UPDATE" -> {
                MbdNotificationHelper.showStatusUpdateNotification(
                    context = applicationContext,
                    requestId = requestId,
                    bloodGroup = bloodGroup,
                    hospitalName = hospitalName,
                    newStatus = newStatus,
                    isBangla = isBangla
                )
            }
            "ADMIN_ANNOUNCEMENT" -> {
                MbdNotificationHelper.showAdminAnnouncementNotification(
                    context = applicationContext,
                    announcementId = data["announcementId"] ?: requestId,
                    title = if (title.isNotBlank()) title else "Notice",
                    body = if (body.isNotBlank()) body else "New announcement from MBD Admin",
                    isEmergency = isEmergency,
                    isBangla = isBangla
                )
            }
            "LIVE_LOCATION" -> {
                MbdNotificationHelper.showLiveLocationSharedNotification(
                    context = applicationContext,
                    requestId = requestId,
                    requesterName = requesterName.ifBlank { "Requester" },
                    bloodGroup = bloodGroup,
                    hospitalName = hospitalName,
                    authorizedDonorName = data["authorizedDonorName"] ?: "Donor",
                    isBangla = isBangla
                )
            }
            else -> {
                if (title.isNotBlank() && body.isNotBlank()) {
                    MbdNotificationHelper.showAdminAnnouncementNotification(
                        context = applicationContext,
                        announcementId = requestId.ifBlank { System.currentTimeMillis().toString() },
                        title = title,
                        body = body,
                        isEmergency = false,
                        isBangla = isBangla
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "MbdFCMService"
    }
}

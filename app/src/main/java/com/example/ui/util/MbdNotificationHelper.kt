package com.example.ui.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.theme.MbdPreferencesManager
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object MbdNotificationHelper {
    const val CHANNEL_EMERGENCY = "mbd_emergency_requests"
    const val CHANNEL_STATUS = "mbd_status_updates"
    const val CHANNEL_ADMIN = "mbd_admin_alerts"

    const val EXTRA_TARGET_SCREEN = "EXTRA_TARGET_SCREEN"
    const val EXTRA_REQUEST_ID = "EXTRA_REQUEST_ID"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        return MbdPreferencesManager.isNotificationsEnabled(context)
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val isBangla = MbdPreferencesManager.getLanguage(context) == "bn"

            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                if (isBangla) "জরুরি রক্তের আবেদন" else "Emergency Blood Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for urgent blood requests in Mirpur & Dhaka"
                enableVibration(true)
                setShowBadge(true)
            }

            val statusChannel = NotificationChannel(
                CHANNEL_STATUS,
                if (isBangla) "আবেদনের স্ট্যাটাস আপডেট" else "Request Status Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for blood request updates and fulfillments"
                enableVibration(true)
                setShowBadge(true)
            }

            val adminChannel = NotificationChannel(
                CHANNEL_ADMIN,
                if (isBangla) "অ্যাডমিন অ্যালার্ট ও ঘোষণা" else "Admin Alerts & Announcements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Important system announcements and admin alerts"
                setShowBadge(true)
            }

            manager.createNotificationChannels(listOf(emergencyChannel, statusChannel, adminChannel))
        }
    }

    private fun createPendingIntent(context: Context, requestId: String?, targetScreen: String, notificationId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_SCREEN, targetScreen)
            if (!requestId.isNullOrBlank()) {
                putExtra(EXTRA_REQUEST_ID, requestId)
            }
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, notificationId, intent, flags)
    }

    fun showBloodRequestNotification(
        context: Context,
        requestId: String = "",
        bloodGroup: String,
        hospitalName: String,
        location: String,
        requesterName: String = "",
        isBangla: Boolean = MbdPreferencesManager.getLanguage(context) == "bn"
    ) {
        if (!areNotificationsEnabled(context)) return
        createNotificationChannels(context)

        val notifId = if (requestId.isNotBlank()) requestId.hashCode() else (System.currentTimeMillis() % 100000).toInt()
        val pendingIntent = createPendingIntent(context, requestId, "REQUEST_DETAILS", notifId)

        val title = if (isBangla) {
            "জরুরি $bloodGroup রক্তের আবেদন — MBD 🩸"
        } else {
            "Urgent $bloodGroup Blood Needed — MBD 🩸"
        }

        val body = if (isBangla) {
            val req = if (requesterName.isNotBlank()) "$requesterName — " else ""
            "${req}$hospitalName ($location)-এ জরুরি $bloodGroup রক্ত প্রয়োজন। বিস্তারিত দেখতে ট্যাপ করুন।"
        } else {
            val req = if (requesterName.isNotBlank()) "$requesterName: " else ""
            "${req}$bloodGroup blood needed at $hospitalName ($location). Tap to view."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_EMERGENCY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (ignored: SecurityException) {
        }
    }

    fun showStatusUpdateNotification(
        context: Context,
        requestId: String,
        bloodGroup: String,
        hospitalName: String,
        newStatus: String,
        isBangla: Boolean = MbdPreferencesManager.getLanguage(context) == "bn"
    ) {
        if (!areNotificationsEnabled(context)) return
        createNotificationChannels(context)

        val notifId = (requestId + "_status_" + newStatus).hashCode()
        val pendingIntent = createPendingIntent(context, requestId, "REQUEST_DETAILS", notifId)

        val statusText = if (isBangla) {
            when (newStatus.lowercase()) {
                "accepted" -> "রক্তদাতা পাওয়া গেছে"
                "completed", "fulfilled" -> "রক্তদান সফলভাবে সম্পন্ন হয়েছে"
                "cancelled" -> "রক্তের আবেদন বাতিল করা হয়েছে"
                else -> "স্ট্যাটাস আপডেট করা হয়েছে"
            }
        } else {
            when (newStatus.lowercase()) {
                "accepted" -> "Donor Assigned"
                "completed", "fulfilled" -> "Blood Request Fulfilled"
                "cancelled" -> "Blood Request Cancelled"
                else -> "Request Status Updated"
            }
        }

        val title = if (isBangla) {
            "রক্তের আবেদনের আপডেট ($bloodGroup) — MBD"
        } else {
            "Blood Request Update ($bloodGroup) — MBD"
        }

        val body = if (isBangla) {
            "$hospitalName-এ $bloodGroup রক্তের আবেদনের জন্য: $statusText। বিস্তারিত দেখতে ট্যাপ করুন।"
        } else {
            "Update for $bloodGroup request at $hospitalName: $statusText. Tap to view."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (ignored: SecurityException) {
        }
    }

    fun showAdminAnnouncementNotification(
        context: Context,
        announcementId: String,
        title: String,
        body: String,
        isEmergency: Boolean,
        isBangla: Boolean = MbdPreferencesManager.getLanguage(context) == "bn"
    ) {
        if (!areNotificationsEnabled(context)) return
        createNotificationChannels(context)

        val notifId = announcementId.hashCode()
        val channel = if (isEmergency) CHANNEL_EMERGENCY else CHANNEL_ADMIN
        val pendingIntent = createPendingIntent(context, null, "NOTIFICATIONS", notifId)

        val displayTitle = if (isEmergency) {
            if (isBangla) "🚨 জরুরি নোটিশ — $title" else "🚨 Emergency Notice — $title"
        } else {
            "MBD: $title"
        }

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(displayTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (isEmergency) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (ignored: SecurityException) {
        }
    }

    fun showLiveLocationSharedNotification(
        context: Context,
        requestId: String = "",
        requesterName: String,
        bloodGroup: String,
        hospitalName: String,
        authorizedDonorName: String,
        isBangla: Boolean = MbdPreferencesManager.getLanguage(context) == "bn"
    ) {
        if (!areNotificationsEnabled(context)) return
        createNotificationChannels(context)

        val notifId = if (requestId.isNotBlank()) (requestId + "_location").hashCode() else ((System.currentTimeMillis() + 7) % 100000).toInt()
        val pendingIntent = createPendingIntent(context, requestId, "DONOR_LIVE_MAP_VIEW", notifId)

        val title = if (isBangla) {
            "লাইভ লোকেশন শেয়ার করা হয়েছে ($bloodGroup) — MBD 🩸"
        } else {
            "Live Location Shared ($bloodGroup) — MBD 🩸"
        }
        val body = if (isBangla) {
            "$requesterName ($hospitalName) রক্তদাতা $authorizedDonorName-এর সাথে লাইভ লোকেশন শেয়ার করেছেন। লাইভ মানচিত্র দেখতে ট্যাপ করুন।"
        } else {
            "$requesterName ($hospitalName) shared live location with donor $authorizedDonorName. Tap to view map."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_EMERGENCY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (ignored: SecurityException) {
        }
    }
}

fun dialPhoneNumber(context: Context, phoneNumber: String) {
    val sanitized = phoneNumber.trim()
    if (sanitized.isEmpty()) return
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (ignored: Exception) {
    }
}

fun formatRelativeTime(timestamp: Timestamp?, isBangla: Boolean): String {
    if (timestamp == null) return if (isBangla) "এইমাত্র" else "Just now"
    val nowMs = System.currentTimeMillis()
    val thenMs = timestamp.toDate().time
    val diffMs = (nowMs - thenMs).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)

    return when {
        minutes < 1 -> if (isBangla) "এইমাত্র" else "Just now"
        minutes < 60 -> if (isBangla) "$minutes মিনিট আগে" else "$minutes mins ago"
        hours < 24 -> if (isBangla) "$hours ঘণ্টা আগে" else "$hours hours ago"
        days < 7 -> if (isBangla) "$days দিন আগে" else "$days days ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(thenMs))
    }
}

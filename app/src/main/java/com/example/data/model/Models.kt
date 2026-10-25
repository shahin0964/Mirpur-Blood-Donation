package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val bloodGroup: String = "A+",
    val area: String = "Mirpur-10",
    val address: String = "Mirpur, Dhaka",
    val lastDonationDate: String = "",
    val lastDonationTime: String = "",
    val nextEligibleDate: String = "",
    val donationType: String = "whole_blood",
    val customDonationTypeName: String = "",
    val eligibilityIntervalDays: Long = 90L,
    val ruleVersion: String = "v1.0",
    val wholeBloodCount: Long = 0L,
    val plateletsCount: Long = 0L,
    val plasmaCount: Long = 0L,
    val otherDonationCount: Long = 0L,
    val availabilityStatus: String = "available",
    @get:PropertyName("isAvailable")
    val isAvailable: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val language: String = "bn",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "fullName" to fullName,
        "email" to email,
        "phoneNumber" to phoneNumber,
        "bloodGroup" to bloodGroup,
        "area" to area,
        "address" to address,
        "lastDonationDate" to lastDonationDate,
        "lastDonationTime" to lastDonationTime,
        "nextEligibleDate" to nextEligibleDate,
        "donationType" to donationType.ifBlank { "whole_blood" },
        "customDonationTypeName" to customDonationTypeName,
        "eligibilityIntervalDays" to eligibilityIntervalDays.coerceIn(0L, 365L),
        "ruleVersion" to ruleVersion.ifBlank { "v1.0" },
        "wholeBloodCount" to wholeBloodCount.coerceAtLeast(0L),
        "plateletsCount" to plateletsCount.coerceAtLeast(0L),
        "plasmaCount" to plasmaCount.coerceAtLeast(0L),
        "otherDonationCount" to otherDonationCount.coerceAtLeast(0L),
        "availabilityStatus" to availabilityStatus.ifBlank { if (isAvailable) "available" else "temporarily_unavailable" },
        "isAvailable" to isAvailable,
        "notificationsEnabled" to notificationsEnabled,
        "language" to language,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(uid: String, existingCreatedAt: Timestamp): Map<String, Any> = mapOf(
        "userId" to uid,
        "fullName" to fullName,
        "email" to email,
        "phoneNumber" to phoneNumber,
        "bloodGroup" to bloodGroup,
        "area" to area,
        "address" to address,
        "lastDonationDate" to lastDonationDate,
        "lastDonationTime" to lastDonationTime,
        "nextEligibleDate" to nextEligibleDate,
        "donationType" to donationType.ifBlank { "whole_blood" },
        "customDonationTypeName" to customDonationTypeName,
        "eligibilityIntervalDays" to eligibilityIntervalDays.coerceIn(0L, 365L),
        "ruleVersion" to ruleVersion.ifBlank { "v1.0" },
        "wholeBloodCount" to wholeBloodCount.coerceAtLeast(0L),
        "plateletsCount" to plateletsCount.coerceAtLeast(0L),
        "plasmaCount" to plasmaCount.coerceAtLeast(0L),
        "otherDonationCount" to otherDonationCount.coerceAtLeast(0L),
        "availabilityStatus" to availabilityStatus.ifBlank { if (isAvailable) "available" else "temporarily_unavailable" },
        "isAvailable" to isAvailable,
        "notificationsEnabled" to notificationsEnabled,
        "language" to language,
        "createdAt" to existingCreatedAt,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class DonorProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val bloodGroup: String = "A+",
    val area: String = "Mirpur-10",
    val address: String = "Mirpur-10, Dhaka",
    val phoneNumber: String = "",
    @get:PropertyName("isAvailable")
    val isAvailable: Boolean = true,
    val availabilityStatus: String = "available",
    val lastDonationDate: String = "",
    val lastDonationTime: String = "",
    val nextEligibleDate: String = "",
    val donationType: String = "whole_blood",
    val customDonationTypeName: String = "",
    val eligibilityIntervalDays: Long = 90L,
    val ruleVersion: String = "v1.0",
    val totalDonations: Long = 0L,
    val wholeBloodCount: Long = 0L,
    val plateletsCount: Long = 0L,
    val plasmaCount: Long = 0L,
    val otherDonationCount: Long = 0L,
    val visibility: String = "public",
    val age: String = "",
    val gender: String = "",
    @get:PropertyName("showPhonePublicly")
    val showPhonePublicly: Boolean = true,
    @get:PropertyName("shareLocationArea")
    val shareLocationArea: Boolean = true,
    @get:PropertyName("showDonationHistoryPublicly")
    val showDonationHistoryPublicly: Boolean = true,
    @get:PropertyName("isPhoneVerified")
    val isPhoneVerified: Boolean = true,
    @get:PropertyName("isAdminReviewed")
    val isAdminReviewed: Boolean = true,
    @get:PropertyName("isReported")
    val isReported: Boolean = false,
    val reportReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedAvailabilityStatus(): String {
        return when (availabilityStatus) {
            "available", "temporarily_unavailable", "not_accepting" -> availabilityStatus
            else -> if (isAvailable) "available" else "temporarily_unavailable"
        }
    }

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "fullName" to fullName,
        "bloodGroup" to bloodGroup,
        "area" to area,
        "address" to address,
        "phoneNumber" to phoneNumber,
        "isAvailable" to isAvailable,
        "availabilityStatus" to resolvedAvailabilityStatus(),
        "lastDonationDate" to lastDonationDate,
        "lastDonationTime" to lastDonationTime,
        "nextEligibleDate" to nextEligibleDate,
        "donationType" to donationType.ifBlank { "whole_blood" },
        "customDonationTypeName" to customDonationTypeName,
        "eligibilityIntervalDays" to eligibilityIntervalDays.coerceIn(0L, 365L),
        "ruleVersion" to ruleVersion.ifBlank { "v1.0" },
        "totalDonations" to totalDonations.coerceAtLeast(0L),
        "wholeBloodCount" to wholeBloodCount.coerceAtLeast(0L),
        "plateletsCount" to plateletsCount.coerceAtLeast(0L),
        "plasmaCount" to plasmaCount.coerceAtLeast(0L),
        "otherDonationCount" to otherDonationCount.coerceAtLeast(0L),
        "visibility" to visibility,
        "age" to age,
        "gender" to gender,
        "showPhonePublicly" to showPhonePublicly,
        "shareLocationArea" to shareLocationArea,
        "showDonationHistoryPublicly" to showDonationHistoryPublicly,
        "isPhoneVerified" to isPhoneVerified,
        "isAdminReviewed" to isAdminReviewed,
        "isReported" to isReported,
        "reportReason" to reportReason,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(uid: String, existingCreatedAt: Timestamp): Map<String, Any> = mapOf(
        "userId" to uid,
        "fullName" to fullName,
        "bloodGroup" to bloodGroup,
        "area" to area,
        "address" to address,
        "phoneNumber" to phoneNumber,
        "isAvailable" to isAvailable,
        "availabilityStatus" to resolvedAvailabilityStatus(),
        "lastDonationDate" to lastDonationDate,
        "lastDonationTime" to lastDonationTime,
        "nextEligibleDate" to nextEligibleDate,
        "donationType" to donationType.ifBlank { "whole_blood" },
        "customDonationTypeName" to customDonationTypeName,
        "eligibilityIntervalDays" to eligibilityIntervalDays.coerceIn(0L, 365L),
        "ruleVersion" to ruleVersion.ifBlank { "v1.0" },
        "totalDonations" to totalDonations.coerceAtLeast(0L),
        "wholeBloodCount" to wholeBloodCount.coerceAtLeast(0L),
        "plateletsCount" to plateletsCount.coerceAtLeast(0L),
        "plasmaCount" to plasmaCount.coerceAtLeast(0L),
        "otherDonationCount" to otherDonationCount.coerceAtLeast(0L),
        "visibility" to visibility,
        "age" to age,
        "gender" to gender,
        "showPhonePublicly" to showPhonePublicly,
        "shareLocationArea" to shareLocationArea,
        "showDonationHistoryPublicly" to showDonationHistoryPublicly,
        "isPhoneVerified" to isPhoneVerified,
        "isAdminReviewed" to isAdminReviewed,
        "isReported" to isReported,
        "reportReason" to reportReason,
        "createdAt" to existingCreatedAt,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class BloodRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val requesterName: String = "",
    val patientName: String = "",
    val bloodGroup: String = "A+",
    val bagsNeeded: Long = 1L,
    val hospitalName: String = "",
    val location: String = "",
    val contactNumber: String = "",
    val neededDateTime: String = "",
    val donationType: String = "whole_blood",
    val urgency: String = "Emergency",
    val notes: String = "",
    val status: String = "open",
    val visibility: String = "community",
    val authorizedDonorId: String = "",
    val authorizedDonorName: String = "",
    @get:PropertyName("liveSessionActive")
    val liveSessionActive: Boolean = false,
    val liveSessionExpiresAt: Long = 0L,
    @get:PropertyName("isReported")
    val isReported: Boolean = false,
    val reportReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    /**
     * Resolves the request status across backward-compatible "open"/"fulfilled" and the
     * 5 official statuses: "pending", "donor_contacted", "accepted", "completed", "cancelled".
     */
    fun resolvedStatus(): String = when (status) {
        "open", "pending" -> "pending"
        "donor_contacted" -> "donor_contacted"
        "accepted" -> "accepted"
        "fulfilled", "completed" -> "completed"
        "cancelled" -> "cancelled"
        else -> status
    }

    fun isTerminal(): Boolean = resolvedStatus() in listOf("completed", "cancelled")

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "requesterName" to requesterName,
        "patientName" to patientName,
        "bloodGroup" to bloodGroup,
        "bagsNeeded" to bagsNeeded,
        "hospitalName" to hospitalName,
        "location" to location,
        "contactNumber" to contactNumber,
        "neededDateTime" to neededDateTime,
        "donationType" to donationType.ifBlank { "whole_blood" },
        "urgency" to urgency,
        "notes" to notes,
        "status" to if (status in listOf("open", "pending")) "pending" else status,
        "visibility" to "community",
        "authorizedDonorId" to authorizedDonorId,
        "authorizedDonorName" to authorizedDonorName,
        "liveSessionActive" to liveSessionActive,
        "liveSessionExpiresAt" to liveSessionExpiresAt,
        "isReported" to isReported,
        "reportReason" to reportReason,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun donationTypeLabel(isBangla: Boolean): String {
        return when (donationType.lowercase()) {
            "platelets" -> if (isBangla) "প্লেটলেট" else "Platelets"
            "plasma" -> if (isBangla) "প্লাজমা" else "Plasma"
            "double_red_cell" -> if (isBangla) "ডাবল রেড সেল" else "Double Red Cell"
            "other" -> if (isBangla) "অন্যান্য উপাদান" else "Other"
            else -> if (isBangla) "সম্পূর্ণ রক্ত" else "Whole Blood"
        }
    }
}

/**
 * Legitimate Achievement Badges calculated strictly from verified donation records.
 * Promotes life-saving voluntary contribution while enforcing medically safe intervals.
 */
data class DonorAchievementBadge(
    val id: String,
    val emoji: String,
    val titleBn: String,
    val titleEn: String,
    val descBn: String,
    val descEn: String,
    val isUnlocked: Boolean,
    val tier: String = "bronze"
)

fun computeDonorAchievementBadges(
    donor: DonorProfile,
    verifiedCount: Long
): List<DonorAchievementBadge> {
    return listOf(
        DonorAchievementBadge(
            id = "verified_donor",
            emoji = "🛡️",
            titleBn = "যাচাইকৃত রক্তদাতা",
            titleEn = "Verified Donor",
            descBn = "ফোন নম্বর এবং প্রাথমিক তথ্য সফলভাবে যাচাই করা হয়েছে।",
            descEn = "Phone number and donor credentials verified.",
            isUnlocked = donor.isPhoneVerified && donor.isAdminReviewed,
            tier = "verified"
        ),
        DonorAchievementBadge(
            id = "first_donation",
            emoji = "🌱",
            titleBn = "প্রথম রক্তদান হিরো",
            titleEn = "First Drop Hero",
            descBn = "১টি সফল ও যাচাইকৃত রক্তদান সম্পন্ন করেছেন।",
            descEn = "Completed 1 verified blood donation.",
            isUnlocked = verifiedCount >= 1L,
            tier = "hero"
        ),
        DonorAchievementBadge(
            id = "bronze_lifesaver",
            emoji = "🥉",
            titleBn = "ব্রোঞ্জ জীবনদাতা",
            titleEn = "Bronze Lifesaver",
            descBn = "৩টি যাচাইকৃত রক্তদান সম্পন্ন করে জীবন রক্ষায় অবদান রেখেছেন।",
            descEn = "Completed 3 verified blood donations.",
            isUnlocked = verifiedCount >= 3L,
            tier = "bronze"
        ),
        DonorAchievementBadge(
            id = "silver_lifesaver",
            emoji = "🥈",
            titleBn = "সিলভার জীবনদাতা",
            titleEn = "Silver Lifesaver",
            descBn = "৫টি যাচাইকৃত রক্তদানের গৌরবময় মাইলফলক অর্জন করেছেন।",
            descEn = "Completed 5 verified blood donations milestone.",
            isUnlocked = verifiedCount >= 5L,
            tier = "silver"
        ),
        DonorAchievementBadge(
            id = "gold_lifesaver",
            emoji = "🥇",
            titleBn = "গোল্ডেন জীবনদাতা",
            titleEn = "Gold Lifesaver",
            descBn = "১০টি যাচাইকৃত রক্তদান সম্পন্ন করে অনন্য দৃষ্টান্ত স্থাপন করেছেন।",
            descEn = "Completed 10 verified blood donations milestone.",
            isUnlocked = verifiedCount >= 10L,
            tier = "gold"
        ),
        DonorAchievementBadge(
            id = "platinum_guardian",
            emoji = "👑",
            titleBn = "প্ল্যাটিনাম অভিভাবক",
            titleEn = "Platinum Guardian",
            descBn = "২০টির বেশি রক্তদান সম্পন্ন করা কিংবদন্তী রক্তদাতা।",
            descEn = "Legendary donor with 20+ verified donations.",
            isUnlocked = verifiedCount >= 20L,
            tier = "platinum"
        )
    )
}

@IgnoreExtraProperties
data class LiveLocationSession(
    @DocumentId val id: String = "",
    val requestId: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val authorizedDonorId: String = "",
    val authorizedDonorName: String = "",
    val latitude: Double = 23.8069,
    val longitude: Double = 90.3687,
    val accuracyMeters: Double = 12.0,
    @get:PropertyName("isActive")
    val isActive: Boolean = true,
    val expiresAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "requestId" to requestId,
        "requesterId" to uid,
        "requesterName" to requesterName,
        "authorizedDonorId" to authorizedDonorId,
        "authorizedDonorName" to authorizedDonorName,
        "latitude" to latitude,
        "longitude" to longitude,
        "accuracyMeters" to accuracyMeters,
        "isActive" to isActive,
        "expiresAtEpochMs" to expiresAtEpochMs,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(uid: String, existingCreatedAt: Timestamp): Map<String, Any> = mapOf(
        "requestId" to requestId,
        "requesterId" to uid,
        "requesterName" to requesterName,
        "authorizedDonorId" to authorizedDonorId,
        "authorizedDonorName" to authorizedDonorName,
        "latitude" to latitude,
        "longitude" to longitude,
        "accuracyMeters" to accuracyMeters,
        "isActive" to isActive,
        "expiresAtEpochMs" to expiresAtEpochMs,
        "createdAt" to existingCreatedAt,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class DonationRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val donorName: String = "",
    val hospitalName: String = "",
    val location: String = "",
    val recipientName: String = "",
    val bloodGroup: String = "A+",
    val donationDate: String = "",
    val donationTime: String = "",
    val donationType: String = "whole_blood",
    val customDonationTypeName: String = "",
    val procedureConfirmation: String = "",
    val eligibilityIntervalDays: Long = 90L,
    val ruleVersion: String = "v1.0",
    val nextEligibleDate: String = "",
    val verificationStatus: String = "verified",
    val visibility: String = "public",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "donorName" to donorName,
        "hospitalName" to hospitalName,
        "location" to location,
        "recipientName" to recipientName,
        "bloodGroup" to bloodGroup,
        "donationDate" to donationDate,
        "donationTime" to donationTime,
        "donationType" to donationType.ifBlank { "whole_blood" },
        "customDonationTypeName" to customDonationTypeName,
        "procedureConfirmation" to procedureConfirmation,
        "eligibilityIntervalDays" to eligibilityIntervalDays.coerceIn(0L, 365L),
        "ruleVersion" to ruleVersion.ifBlank { "v1.0" },
        "nextEligibleDate" to nextEligibleDate,
        "verificationStatus" to verificationStatus.ifBlank { "verified" },
        "visibility" to visibility.ifBlank { "public" },
        "notes" to notes,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class EligibilityRule(
    @DocumentId val id: String = "",
    val donationType: String = "whole_blood",
    val titleBn: String = "Whole Blood — সম্পূর্ণ রক্ত",
    val titleEn: String = "Whole Blood — সম্পূর্ণ রক্ত",
    val intervalDays: Long = 90L,
    val crossTypeMinGapDays: Long = 28L,
    @get:PropertyName("isVerifiedRule")
    val isVerifiedRule: Boolean = true,
    val ruleVersion: String = "v1.0",
    val medicalGuidanceBn: String = "সম্পূর্ণ রক্তদানের পর লোহিত কণিকা পুনর্গঠনের জন্য ন্যূনতম ৯০ দিন (৩ মাস) বিরতি প্রয়োজন।",
    val medicalGuidanceEn: String = "A minimum 90-day (3-month) recovery interval is required after Whole Blood donation.",
    val updatedByEmail: String = "md.shahin0962@gmail.com",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "donationType" to donationType,
        "titleBn" to titleBn,
        "titleEn" to titleEn,
        "intervalDays" to intervalDays.coerceIn(0L, 365L),
        "crossTypeMinGapDays" to crossTypeMinGapDays.coerceIn(0L, 180L),
        "isVerifiedRule" to isVerifiedRule,
        "ruleVersion" to ruleVersion.ifBlank { "v1.0" },
        "medicalGuidanceBn" to medicalGuidanceBn,
        "medicalGuidanceEn" to medicalGuidanceEn,
        "updatedByEmail" to updatedByEmail,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(existingCreatedAt: Timestamp): Map<String, Any> = mapOf(
        "donationType" to donationType,
        "titleBn" to titleBn,
        "titleEn" to titleEn,
        "intervalDays" to intervalDays.coerceIn(0L, 365L),
        "crossTypeMinGapDays" to crossTypeMinGapDays.coerceIn(0L, 180L),
        "isVerifiedRule" to isVerifiedRule,
        "ruleVersion" to ruleVersion.ifBlank { "v1.0" },
        "medicalGuidanceBn" to medicalGuidanceBn,
        "medicalGuidanceEn" to medicalGuidanceEn,
        "updatedByEmail" to updatedByEmail,
        "createdAt" to existingCreatedAt,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class DonationTypeOption(
    val code: String,
    val defaultIntervalDays: Int,
    val crossTypeMinGapDays: Int,
    val isVerifiedByDefault: Boolean,
    val labelBn: String,
    val labelEn: String,
    val descBn: String,
    val descEn: String
)

val DONATION_TYPE_OPTIONS = listOf(
    DonationTypeOption(
        code = "whole_blood",
        defaultIntervalDays = 90,
        crossTypeMinGapDays = 28,
        isVerifiedByDefault = true,
        labelBn = "Whole Blood — সম্পূর্ণ রক্ত",
        labelEn = "Whole Blood — সম্পূর্ণ রক্ত",
        descBn = "সাধারণ সম্পূর্ণ রক্তদান • প্রযোজ্য বিরতি: ৯০ দিন (ক্রস-ডোনেশন বিরতি: ২৮ দিন)",
        descEn = "Standard Whole Blood • Configurable interval: 90 days (Cross-component gap: 28 days)"
    ),
    DonationTypeOption(
        code = "platelets",
        defaultIntervalDays = 14,
        crossTypeMinGapDays = 7,
        isVerifiedByDefault = true,
        labelBn = "Platelets — প্লেটলেট",
        labelEn = "Platelets — প্লেটলেট",
        descBn = "অ্যাফেরেসিস প্লেটলেট রক্তদান • প্রযোজ্য বিরতি: ১৪ দিন (ক্রস-ডোনেশন বিরতি: ৭ দিন)",
        descEn = "Plateletpheresis donation • Configurable interval: 14 days (Cross-component gap: 7 days)"
    ),
    DonationTypeOption(
        code = "plasma",
        defaultIntervalDays = 28,
        crossTypeMinGapDays = 14,
        isVerifiedByDefault = true,
        labelBn = "Plasma — প্লাজমা",
        labelEn = "Plasma — প্লাজমা",
        descBn = "প্লাজমা রক্তদান • প্রযোজ্য বিরতি: ২৮ দিন (ক্রস-ডোনেশন বিরতি: ১৪ দিন)",
        descEn = "Plasma donation • Configurable interval: 28 days (Cross-component gap: 14 days)"
    ),
    DonationTypeOption(
        code = "other",
        defaultIntervalDays = 0,
        crossTypeMinGapDays = 0,
        isVerifiedByDefault = false,
        labelBn = "Other / Custom Donation Type — অন্যান্য",
        labelEn = "Other / Custom Donation Type — অন্যান্য",
        descBn = "অন্যান্য বা বিশেষ রক্তদান • অ্যাডমিন কর্তৃক নিয়ম নির্ধারিত না হওয়া পর্যন্ত কাউন্টডাউন বন্ধ থাকবে",
        descEn = "Other / Custom donation • Countdown pauses until an authorized admin configures a verified rule"
    )
)

val DEFAULT_ELIGIBILITY_RULES: Map<String, EligibilityRule> = mapOf(
    "whole_blood" to EligibilityRule(
        id = "whole_blood",
        donationType = "whole_blood",
        titleBn = "Whole Blood — সম্পূর্ণ রক্ত",
        titleEn = "Whole Blood — সম্পূর্ণ রক্ত",
        intervalDays = 90L,
        crossTypeMinGapDays = 28L,
        isVerifiedRule = true,
        ruleVersion = "v1.0",
        medicalGuidanceBn = "সম্পূর্ণ রক্তদানের পর পুনরায় সম্পূর্ণ রক্তদানে ন্যূনতম ৯০ দিন এবং প্লেটলেট/প্লাজমা দানে ন্যূনতম ২৮ দিন বিরতি প্রযোজ্য।",
        medicalGuidanceEn = "Requires 90 days before next Whole Blood donation and 28 days before Platelet/Plasma donation."
    ),
    "platelets" to EligibilityRule(
        id = "platelets",
        donationType = "platelets",
        titleBn = "Platelets — প্লেটলেট",
        titleEn = "Platelets — প্লেটলেট",
        intervalDays = 14L,
        crossTypeMinGapDays = 7L,
        isVerifiedRule = true,
        ruleVersion = "v1.0",
        medicalGuidanceBn = "প্লেটলেট (অ্যাফেরেসিস) দানের পর পরবর্তী প্লেটলেট দানে ১৪ দিন এবং অন্যান্য উপাদানে ন্যূনতম ৭ দিন বিরতি প্রযোজ্য।",
        medicalGuidanceEn = "Requires 14 days before next Platelets donation and at least 7 days before other components."
    ),
    "plasma" to EligibilityRule(
        id = "plasma",
        donationType = "plasma",
        titleBn = "Plasma — প্লাজমা",
        titleEn = "Plasma — প্লাজমা",
        intervalDays = 28L,
        crossTypeMinGapDays = 14L,
        isVerifiedRule = true,
        ruleVersion = "v1.0",
        medicalGuidanceBn = "প্লাজমা দানের পর পরবর্তী প্লাজমা দানে ২৮ দিন এবং অন্যান্য রক্ত উপাদানে ন্যূনতম ১৪ দিন বিরতি প্রযোজ্য।",
        medicalGuidanceEn = "Requires 28 days before next Plasma donation and at least 14 days before other components."
    ),
    "other" to EligibilityRule(
        id = "other",
        donationType = "other",
        titleBn = "Other / Custom Donation Type — অন্যান্য",
        titleEn = "Other / Custom Donation Type — অন্যান্য",
        intervalDays = 0L,
        crossTypeMinGapDays = 0L,
        isVerifiedRule = false,
        ruleVersion = "v1.0-unconfigured",
        medicalGuidanceBn = "অজানা বা কাস্টম রক্তদানের ধরনের জন্য অনুমানভিত্তিক বিরতি গণনা করা হয় না। অনুমোদিত অ্যাডমিন নিয়ম নির্ধারণ করলে কাউন্টডাউন চালু হবে।",
        medicalGuidanceEn = "No interval is guessed for custom/unknown donation types until an authorized administrator configures a verified rule."
    ),
    "double_red_cell" to EligibilityRule(
        id = "double_red_cell",
        donationType = "double_red_cell",
        titleBn = "Double Red Cell — ডাবল রেড সেল",
        titleEn = "Double Red Cell — ডাবল রেড সেল",
        intervalDays = 180L,
        crossTypeMinGapDays = 56L,
        isVerifiedRule = true,
        ruleVersion = "v1.0",
        medicalGuidanceBn = "ডাবল রেড সেল রক্তদানের পর পরবর্তী রক্তদানে ১৮০ দিন বিরতি প্রযোজ্য।",
        medicalGuidanceEn = "Requires 180 days recovery interval after Double Red Cell donation."
    )
)

val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

val MIRPUR_LOCATIONS = listOf(
    "Mirpur-1, Dhaka",
    "Mirpur-2, Dhaka",
    "Mirpur-6, Dhaka",
    "Mirpur-10, Dhaka",
    "Mirpur-11, Dhaka",
    "Mirpur-12, Dhaka",
    "Mirpur-13, Dhaka",
    "Mirpur-14, Dhaka",
    "Pallabi, Mirpur",
    "Kazipara, Mirpur",
    "Shewrapara, Mirpur",
    "Kalshi, Mirpur",
    "Rupnagar, Mirpur",
    "Mirpur DOHS, Dhaka",
    "Agargaon, Dhaka",
    "Mohammadpur, Dhaka"
)

@IgnoreExtraProperties
data class AdminLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val actionType: String = "",
    val targetId: String = "",
    val targetType: String = "",
    val reason: String = "",
    val status: String = "success",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminReport(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val reporterName: String = "",
    val reportedId: String = "",
    val reportedType: String = "request",
    val category: String = "Fake Blood Request",
    val description: String = "",
    val status: String = "pending",
    val adminNotes: String = "",
    val resolvedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val resolvedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminAnnouncement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "info",
    @get:PropertyName("isEmergency")
    val isEmergency: Boolean = false,
    @get:PropertyName("isActive")
    val isActive: Boolean = true,
    val createdByEmail: String = "",
    val durationDays: Int = 0,
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "main_config",
    val emergencyBannerText: String = "",
    @get:PropertyName("emergencyBannerActive")
    val emergencyBannerActive: Boolean = false,
    val supportPhone: String = "01700000000",
    val supportEmail: String = "support@mirpurblooddonation.com",
    val privacyPolicyUrl: String = "https://mirpurblooddonation.org/privacy",
    val termsUrl: String = "https://mirpurblooddonation.org/terms",
    val latestAppVersion: String = "1.0",
    val minRequiredVersion: String = "1.0",
    val latestVersionCode: Long = 1L,
    val minSupportedVersionCode: Long = 1L,
    val downloadUrl: String = "",
    val releaseNotesBn: String = "",
    val releaseNotesEn: String = "",
    val updateMessage: String = "আপনার অ্যাপটি সর্বশেষ সংস্করণে আপডেট করুন",
    @get:PropertyName("isUpdateMandatory")
    val isUpdateMandatory: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminUserBan(
    @DocumentId val userId: String = "",
    val bannedByUid: String = "",
    val bannedByEmail: String = "",
    val reason: String = "",
    @ServerTimestamp val bannedAt: Timestamp? = null
)

data class UserAccountRecord(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val bloodGroup: String = "",
    val area: String = "",
    val isBanned: Boolean = false,
    val isDisabled: Boolean = false,
    val banReason: String = "",
    val bannedAt: Timestamp? = null,
    val isDonor: Boolean = false,
    val totalDonations: Long = 0L,
    val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserRoleAssignment(
    @DocumentId val userId: String = "",
    val role: String = "user",
    val permissions: List<String> = emptyList(),
    val assignedByUid: String = "",
    val assignedByEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectivePermissions(): Set<String> {
        val predefined = when (role.lowercase()) {
            "owner", "admin" -> setOf(
                "view_donors", "view_requests", "manage_requests",
                "manage_donations", "manage_users", "manage_bans", "manage_config"
            )
            "donation_manager" -> setOf(
                "view_donors", "view_requests", "manage_donations"
            )
            "request_manager" -> setOf(
                "view_donors", "view_requests", "manage_requests"
            )
            "support_manager" -> setOf(
                "view_donors", "view_requests", "manage_bans"
            )
            "content_manager" -> setOf(
                "view_donors", "view_requests", "manage_config"
            )
            else -> emptySet()
        }
        return predefined + permissions.toSet()
    }

    fun hasPermission(perm: String): Boolean {
        if (role.lowercase() in listOf("owner", "admin")) return true
        return perm in effectivePermissions()
    }

    fun canAccessAdminPanel(): Boolean {
        return role.lowercase() in listOf("owner", "admin", "donation_manager", "request_manager", "support_manager", "content_manager", "custom") || permissions.isNotEmpty()
    }

    fun toMap(): Map<String, Any> = mapOf(
        "role" to role,
        "permissions" to permissions,
        "assignedByUid" to assignedByUid,
        "assignedByEmail" to assignedByEmail,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}



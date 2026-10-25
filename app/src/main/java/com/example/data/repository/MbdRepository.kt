package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminAnnouncement
import com.example.data.model.AdminLog
import com.example.data.model.AdminReport
import com.example.data.model.AdminUserBan
import com.example.data.model.AppConfig
import com.example.data.model.BloodRequest
import com.example.data.model.DEFAULT_ELIGIBILITY_RULES
import com.example.data.model.DonationRecord
import com.example.data.model.DonorProfile
import com.example.data.model.EligibilityRule
import com.example.data.model.LiveLocationSession
import com.example.data.model.UserProfile
import com.example.data.model.UserRoleAssignment
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MbdRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // 1. User Profile Operations
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                docRef.set(profile.toCreateMap(uid)).await()
            } else {
                val existingCreatedAt = existingSnap.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: profile.createdAt ?: Timestamp.now()
                docRef.set(profile.toUpdateMap(uid, existingCreatedAt)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // 2. Donor Directory Operations
    fun observePublicDonors(): Flow<List<DonorProfile>> {
        val path = "donors"
        return db.collection("donors")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DonorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMyDonorProfile(userId: String): Flow<DonorProfile?> {
        val path = "donors/$userId"
        return db.collection("donors").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(DonorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeDonorById(donorId: String): Flow<DonorProfile?> {
        val path = "donors/$donorId"
        return db.collection("donors").document(donorId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(DonorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun getDonorById(donorId: String): DonorProfile? {
        val docRef = db.collection("donors").document(donorId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(DonorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveDonorProfile(donor: DonorProfile) {
        val uid = requireUserId()
        val docRef = db.collection("donors").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                docRef.set(donor.toCreateMap(uid)).await()
            } else {
                val existingCreatedAt = existingSnap.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: donor.createdAt ?: Timestamp.now()
                docRef.set(donor.toUpdateMap(uid, existingCreatedAt)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun adminCorrectDonorDates(
        donorId: String,
        lastDonationDate: String,
        lastDonationTime: String = "",
        nextEligibleDate: String,
        donationType: String,
        customDonationTypeName: String = "",
        eligibilityIntervalDays: Long,
        ruleVersion: String = "v1.0",
        totalDonations: Long,
        wholeBloodCount: Long? = null,
        plateletsCount: Long? = null,
        plasmaCount: Long? = null,
        otherDonationCount: Long? = null
    ) {
        requireUserId()
        val docRef = db.collection("donors").document(donorId)
        try {
            val updates = mutableMapOf<String, Any>(
                "lastDonationDate" to lastDonationDate,
                "lastDonationTime" to lastDonationTime,
                "nextEligibleDate" to nextEligibleDate,
                "donationType" to donationType.ifBlank { "whole_blood" },
                "customDonationTypeName" to customDonationTypeName,
                "eligibilityIntervalDays" to eligibilityIntervalDays.coerceIn(0L, 365L),
                "ruleVersion" to ruleVersion.ifBlank { "v1.0" },
                "totalDonations" to totalDonations.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            wholeBloodCount?.let { updates["wholeBloodCount"] = it.coerceAtLeast(0L) }
            plateletsCount?.let { updates["plateletsCount"] = it.coerceAtLeast(0L) }
            plasmaCount?.let { updates["plasmaCount"] = it.coerceAtLeast(0L) }
            otherDonationCount?.let { updates["otherDonationCount"] = it.coerceAtLeast(0L) }
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // 3. Blood Requests Operations
    fun observeCommunityBloodRequests(): Flow<List<BloodRequest>> {
        val path = "blood_requests"
        return db.collection("blood_requests")
            .whereEqualTo("visibility", "community")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BloodRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createBloodRequest(request: BloodRequest): String {
        val uid = requireUserId()
        val colRef = db.collection("blood_requests")
        val docRef = colRef.document()
        return try {
            docRef.set(request.toCreateMap(uid)).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBloodRequestStatus(requestId: String, newStatus: String) {
        requireUserId()
        val docRef = db.collection("blood_requests").document(requestId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "liveSessionActive" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBloodRequestLiveSessionMetadata(
        requestId: String,
        authorizedDonorId: String,
        authorizedDonorName: String,
        liveSessionActive: Boolean,
        liveSessionExpiresAt: Long
    ) {
        requireUserId()
        val docRef = db.collection("blood_requests").document(requestId)
        try {
            docRef.update(
                mapOf(
                    "authorizedDonorId" to authorizedDonorId,
                    "authorizedDonorName" to authorizedDonorName,
                    "liveSessionActive" to liveSessionActive,
                    "liveSessionExpiresAt" to liveSessionExpiresAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBloodRequest(requestId: String) {
        requireUserId()
        val docRef = db.collection("blood_requests").document(requestId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun reportBloodRequest(requestId: String, reason: String) {
        requireUserId()
        val docRef = db.collection("blood_requests").document(requestId)
        try {
            docRef.update(
                mapOf(
                    "isReported" to true,
                    "reportReason" to reason.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun reportDonorProfile(donorId: String, reason: String) {
        requireUserId()
        val docRef = db.collection("donors").document(donorId)
        try {
            docRef.update(
                mapOf(
                    "isReported" to true,
                    "reportReason" to reason.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // 4. Restricted Live Location Sessions (/live_locations/{requestId})
    fun observeLiveLocationSession(requestId: String): Flow<LiveLocationSession?> {
        val path = "live_locations/$requestId"
        return db.collection("live_locations").document(requestId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(LiveLocationSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun getLiveLocationSession(requestId: String): LiveLocationSession? {
        val docRef = db.collection("live_locations").document(requestId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(LiveLocationSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveLiveLocationSession(session: LiveLocationSession) {
        val uid = requireUserId()
        val docRef = db.collection("live_locations").document(session.requestId)
        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                docRef.set(session.toCreateMap(uid)).await()
            } else {
                val existingCreatedAt = existingSnap.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: session.createdAt ?: Timestamp.now()
                docRef.set(session.toUpdateMap(uid, existingCreatedAt)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteLiveLocationSession(requestId: String) {
        requireUserId()
        val docRef = db.collection("live_locations").document(requestId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // 5. Synchronized Donation History Operations (/donation_history/{recordId})
    fun observeDonations(userId: String): Flow<List<DonationRecord>> {
        val path = "donation_history"
        return db.collection("donation_history")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DonationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<DonationRecord> { it.donationDate }
                            .thenByDescending { it.donationTime }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observePublicDonorDonations(donorId: String): Flow<List<DonationRecord>> {
        val path = "donation_history"
        return db.collection("donation_history")
            .whereEqualTo("userId", donorId)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DonationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.verificationStatus != "rejected" }
                    .sortedWith(
                        compareByDescending<DonationRecord> { it.donationDate }
                            .thenByDescending { it.donationTime }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addDonationRecord(record: DonationRecord): String {
        val uid = requireUserId()
        val colRef = db.collection("donation_history")
        val docRef = colRef.document()
        return try {
            docRef.set(record.toCreateMap(uid)).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateDonationRecordVerificationAndDates(
        recordId: String,
        verificationStatus: String,
        donationDate: String,
        donationTime: String = "",
        donationType: String,
        customDonationTypeName: String = "",
        procedureConfirmation: String = "",
        eligibilityIntervalDays: Long,
        ruleVersion: String = "v1.0",
        nextEligibleDate: String,
        notes: String
    ) {
        requireUserId()
        val docRef = db.collection("donation_history").document(recordId)
        try {
            docRef.update(
                mapOf(
                    "verificationStatus" to verificationStatus,
                    "donationDate" to donationDate,
                    "donationTime" to donationTime,
                    "donationType" to donationType.ifBlank { "whole_blood" },
                    "customDonationTypeName" to customDonationTypeName,
                    "procedureConfirmation" to procedureConfirmation,
                    "eligibilityIntervalDays" to eligibilityIntervalDays.coerceIn(0L, 365L),
                    "ruleVersion" to ruleVersion.ifBlank { "v1.0" },
                    "nextEligibleDate" to nextEligibleDate,
                    "notes" to notes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDonationRecord(donationId: String) {
        requireUserId()
        val docRef = db.collection("donation_history").document(donationId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // 6. Configurable Medical Eligibility Rules Operations (/eligibility_rules/{ruleId})
    fun observeEligibilityRules(): Flow<Map<String, EligibilityRule>> {
        val path = "eligibility_rules"
        return db.collection("eligibility_rules")
            .snapshots()
            .map { snapshot ->
                val merged = DEFAULT_ELIGIBILITY_RULES.toMutableMap()
                val remoteRules = snapshot.toObjects(
                    EligibilityRule::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                for (rule in remoteRules) {
                    val key = rule.donationType.ifBlank { rule.id }
                    if (key.isNotBlank()) {
                        merged[key] = rule
                    }
                }
                merged.toMap()
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun saveEligibilityRule(rule: EligibilityRule) {
        requireUserId()
        val ruleId = rule.donationType.ifBlank { rule.id }.ifBlank { "whole_blood" }
        val docRef = db.collection("eligibility_rules").document(ruleId)
        try {
            val existingSnap = docRef.get().await()
            val normalizedRule = rule.copy(donationType = ruleId)
            if (!existingSnap.exists()) {
                docRef.set(normalizedRule.toCreateMap()).await()
            } else {
                val existingCreatedAt = existingSnap.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: rule.createdAt ?: Timestamp.now()
                docRef.set(normalizedRule.toUpdateMap(existingCreatedAt)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // 7. Secure Admin Control Panel Operations
    fun observeIsAdmin(userId: String): Flow<Boolean> {
        val path = "admins/$userId"
        return db.collection("admins").document(userId)
            .snapshots()
            .map { snap ->
                val isDocAdmin = snap.exists()
                val isOwnerEmail = auth.currentUser?.email?.equals("md.shahin0962@gmail.com", ignoreCase = true) == true
                isDocAdmin || isOwnerEmail
            }
            .catch { emit(auth.currentUser?.email?.equals("md.shahin0962@gmail.com", ignoreCase = true) == true) }
    }

    suspend fun checkIsAdmin(userId: String): Boolean {
        return try {
            val isOwnerEmail = auth.currentUser?.email?.equals("md.shahin0962@gmail.com", ignoreCase = true) == true
            val snap = db.collection("admins").document(userId).get().await()
            snap.exists() || isOwnerEmail
        } catch (_: Exception) {
            auth.currentUser?.email?.equals("md.shahin0962@gmail.com", ignoreCase = true) == true
        }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { emit(emptyList()) }
    }

    fun observeUserRoleAssignment(userId: String): Flow<UserRoleAssignment?> {
        val path = "user_roles/$userId"
        return db.collection("user_roles").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserRoleAssignment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { emit(null) }
    }

    fun observeAllUserRoles(): Flow<List<UserRoleAssignment>> {
        return db.collection("user_roles")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserRoleAssignment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { emit(emptyList()) }
    }

    suspend fun assignUserRole(
        targetUserId: String,
        role: String,
        permissions: List<String>,
        adminUid: String,
        adminEmail: String
    ) {
        requireUserId()
        val docRef = db.collection("user_roles").document(targetUserId)
        val data = mapOf(
            "role" to role,
            "permissions" to permissions,
            "assignedByUid" to adminUid,
            "assignedByEmail" to adminEmail,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        logAdminAction(
            AdminLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "ROLE_ASSIGNED",
                targetId = targetUserId,
                targetType = "user",
                reason = "Assigned role=$role with permissions=$permissions",
                status = "success"
            )
        )
    }

    fun observeAllBans(): Flow<List<AdminUserBan>> {
        return db.collection("bans")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AdminUserBan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { emit(emptyList()) }
    }

    suspend fun banUser(userId: String, reason: String, adminUid: String, adminEmail: String) {
        requireUserId()
        val banDoc = db.collection("bans").document(userId)
        val banData = mapOf(
            "userId" to userId,
            "bannedByUid" to adminUid,
            "bannedByEmail" to adminEmail,
            "reason" to reason,
            "bannedAt" to FieldValue.serverTimestamp()
        )
        banDoc.set(banData).await()
        logAdminAction(
            AdminLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "USER_BANNED",
                targetId = userId,
                targetType = "user",
                reason = reason,
                status = "success"
            )
        )
    }

    suspend fun unbanUser(userId: String, adminUid: String, adminEmail: String) {
        requireUserId()
        val banDoc = db.collection("bans").document(userId)
        banDoc.delete().await()
        logAdminAction(
            AdminLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "USER_UNBANNED",
                targetId = userId,
                targetType = "user",
                reason = "Ban revoked by administrator",
                status = "success"
            )
        )
    }

    suspend fun disableOrRestoreUser(userId: String, disable: Boolean, adminUid: String, adminEmail: String) {
        requireUserId()
        val userDoc = db.collection("users").document(userId)
        userDoc.update(
            mapOf(
                "availabilityStatus" to if (disable) "not_accepting" else "available",
                "isAvailable" to !disable,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        logAdminAction(
            AdminLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = if (disable) "USER_DISABLED" else "USER_RESTORED",
                targetId = userId,
                targetType = "user",
                reason = if (disable) "Account disabled by admin" else "Account restored by admin",
                status = "success"
            )
        )
    }

    suspend fun deleteUserAccount(userId: String, adminUid: String, adminEmail: String) {
        requireUserId()
        if (userId == adminUid) {
            throw IllegalStateException("The owner-admin account cannot be deleted.")
        }
        val userDoc = db.collection("users").document(userId)
        userDoc.delete().await()
        try {
            db.collection("donors").document(userId).delete().await()
        } catch (_: Exception) {}
        logAdminAction(
            AdminLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "USER_DELETED",
                targetId = userId,
                targetType = "user",
                reason = "User profile removed by admin",
                status = "success"
            )
        )
    }

    fun observeAllReports(): Flow<List<AdminReport>> {
        return db.collection("reports")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AdminReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { emit(emptyList()) }
    }

    suspend fun submitReport(report: AdminReport): String {
        requireUserId()
        val docRef = db.collection("reports").document()
        val data = mapOf(
            "reporterUid" to report.reporterUid.ifBlank { requireUserId() },
            "reporterName" to report.reporterName,
            "reportedId" to report.reportedId,
            "reportedType" to report.reportedType,
            "category" to report.category,
            "description" to report.description,
            "status" to "pending",
            "adminNotes" to "",
            "resolvedBy" to "",
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        return docRef.id
    }

    suspend fun updateReportStatus(
        reportId: String,
        status: String,
        adminNotes: String,
        adminUid: String,
        adminEmail: String
    ) {
        requireUserId()
        val docRef = db.collection("reports").document(reportId)
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "adminNotes" to adminNotes,
            "resolvedBy" to adminUid,
            "resolvedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updates).await()
        logAdminAction(
            AdminLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "REPORT_" + status.uppercase(),
                targetId = reportId,
                targetType = "report",
                reason = adminNotes,
                status = "success"
            )
        )
    }

    suspend fun adminModerateBloodRequest(
        requestId: String,
        newStatus: String,
        moderationReason: String,
        adminUid: String,
        adminEmail: String
    ) {
        requireUserId()
        val docRef = db.collection("blood_requests").document(requestId)
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "liveSessionActive" to false,
            "notes" to if (moderationReason.isNotBlank()) "Moderation: $moderationReason" else "",
            "isReported" to (newStatus == "cancelled"),
            "reportReason" to moderationReason,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updates).await()
        logAdminAction(
            AdminLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "REQUEST_MODERATED",
                targetId = requestId,
                targetType = "request",
                reason = "Status set to $newStatus. Reason: $moderationReason",
                status = "success"
            )
        )
    }

    fun observeAllDonationHistory(): Flow<List<DonationRecord>> {
        return db.collection("donation_history")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DonationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { emit(emptyList()) }
    }

    suspend fun adminVerifyDonationRecord(
        recordId: String,
        newStatus: String,
        notes: String,
        adminUid: String,
        adminEmail: String
    ) {
        requireUserId()
        val docRef = db.collection("donation_history").document(recordId)
        val updates = mutableMapOf<String, Any>(
            "verificationStatus" to newStatus,
            "notes" to notes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updates).await()
        logAdminAction(
            AdminLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "DONATION_VERIFIED",
                targetId = recordId,
                targetType = "donation",
                reason = "Verification set to $newStatus. Notes: $notes",
                status = "success"
            )
        )
    }

    suspend fun adminSetDonorVerificationStatus(
        donorId: String,
        isPhoneVerified: Boolean,
        isAdminReviewed: Boolean,
        adminUid: String,
        adminEmail: String
    ) {
        requireUserId()
        val docRef = db.collection("donors").document(donorId)
        val updates = mapOf<String, Any>(
            "isPhoneVerified" to isPhoneVerified,
            "isAdminReviewed" to isAdminReviewed,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updates).await()
        logAdminAction(
            AdminLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "DONOR_VERIFIED",
                targetId = donorId,
                targetType = "donor",
                reason = "PhoneVerified=$isPhoneVerified, AdminReviewed=$isAdminReviewed",
                status = "success"
            )
        )
    }

    suspend fun logAdminAction(action: AdminLog) {
        try {
            val docRef = db.collection("admin_logs").document()
            val data = mapOf(
                "adminUid" to action.adminUid,
                "adminEmail" to action.adminEmail,
                "actionType" to action.actionType,
                "targetId" to action.targetId,
                "targetType" to action.targetType,
                "reason" to action.reason,
                "status" to action.status,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
        } catch (_: Exception) {}
    }

    fun observeAdminLogs(): Flow<List<AdminLog>> {
        return db.collection("admin_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AdminLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { emit(emptyList()) }
    }

    fun observeAnnouncements(): Flow<List<AdminAnnouncement>> {
        return db.collection("admin_announcements")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AdminAnnouncement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { emit(emptyList()) }
    }

    suspend fun createAnnouncement(
        announcement: AdminAnnouncement,
        adminUid: String,
        adminEmail: String
    ): String {
        requireUserId()
        val docRef = db.collection("admin_announcements").document()
        val data = mapOf(
            "title" to announcement.title,
            "message" to announcement.message,
            "type" to announcement.type,
            "isEmergency" to announcement.isEmergency,
            "isActive" to announcement.isActive,
            "createdByEmail" to adminEmail,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        logAdminAction(
            AdminLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "NOTIFICATION_SENT",
                targetId = docRef.id,
                targetType = "broadcast",
                reason = announcement.title,
                status = "success"
            )
        )
        return docRef.id
    }

    suspend fun deleteAnnouncement(announcementId: String) {
        requireUserId()
        db.collection("admin_announcements").document(announcementId).delete().await()
    }

    fun observeAppConfig(): Flow<AppConfig> {
        return db.collection("app_config").document("main_config")
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: AppConfig()
                } else {
                    AppConfig()
                }
            }
            .catch { emit(AppConfig()) }
    }

    suspend fun saveAppConfig(config: AppConfig, adminUid: String, adminEmail: String) {
        requireUserId()
        val docRef = db.collection("app_config").document("main_config")
        val data = mapOf(
            "emergencyBannerText" to config.emergencyBannerText,
            "emergencyBannerActive" to config.emergencyBannerActive,
            "supportPhone" to config.supportPhone,
            "supportEmail" to config.supportEmail,
            "privacyPolicyUrl" to config.privacyPolicyUrl,
            "termsUrl" to config.termsUrl,
            "latestAppVersion" to config.latestAppVersion,
            "minRequiredVersion" to config.minRequiredVersion,
            "updateMessage" to config.updateMessage,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        logAdminAction(
            AdminLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "CONFIG_UPDATED",
                targetId = "main_config",
                targetType = "config",
                reason = "App configuration saved",
                status = "success"
            )
        )
    }
}

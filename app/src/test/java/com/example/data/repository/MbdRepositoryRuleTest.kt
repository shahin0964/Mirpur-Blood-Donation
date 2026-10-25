package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BloodRequest
import com.example.data.model.DonationRecord
import com.example.data.model.DonorProfile
import com.example.data.model.LiveLocationSession
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MbdRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndGetUserProfile_authenticatedOwner_succeedsAndIsolatesCrossUser() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = MbdRepository(firestore)

        val profile = UserProfile(
            userId = aliceUid,
            fullName = "Md Shahin",
            email = ALICE_EMAIL,
            phoneNumber = "01711223344",
            bloodGroup = "O+",
            area = "Mirpur-10",
            address = "Mirpur-10, Dhaka",
            lastDonationDate = "2026-05-10",
            isAvailable = true,
            notificationsEnabled = true,
            language = "bn"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(profile)
        }

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile(aliceUid)
        }
        assertNotNull(fetched)
        assertEquals("Md Shahin", fetched?.fullName)

        // Cross-user read must fail with PERMISSION_DENIED
        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.getUserProfile(aliceUid)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user profile read")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun saveDonorAndObservePublicDonors_authenticatedUser_emitsMatchingDonor() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = MbdRepository(firestore)

        val donor = DonorProfile(
            userId = aliceUid,
            fullName = "Rafiqul Islam",
            bloodGroup = "A+",
            area = "Mirpur-10, Dhaka",
            address = "Block C, Mirpur-10, Dhaka",
            phoneNumber = "01712345678",
            isAvailable = true,
            lastDonationDate = "2026-06-15",
            totalDonations = 4L,
            visibility = "public"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveDonorProfile(donor)
        }

        val emittedDonors = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublicDonors().first { list -> list.any { it.id == aliceUid } }
        }
        assertTrue(emittedDonors.any { it.fullName == "Rafiqul Islam" && it.bloodGroup == "A+" })
    }

    @Test
    fun createBloodRequestAndLiveLocation_authorizedParticipantsOnly_succeedsAndBlocksStranger() = runBlocking {
        val bobUid = signInTestUser(BOB_EMAIL)
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = MbdRepository(firestore)

        val requestId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBloodRequest(
                BloodRequest(
                    userId = aliceUid,
                    requesterName = "Tanvir Hasan",
                    patientName = "Rahima Begum",
                    bloodGroup = "B+",
                    bagsNeeded = 2L,
                    hospitalName = "National Heart Foundation Hospital",
                    location = "Mirpur-2, Dhaka",
                    contactNumber = "01819001122",
                    neededDateTime = "2026-10-25 10:00 AM",
                    urgency = "Emergency",
                    notes = "Open heart surgery tomorrow morning"
                )
            )
        }
        assertTrue(requestId.isNotEmpty())

        val expiresAt = System.currentTimeMillis() + 30 * 60 * 1000L
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveLiveLocationSession(
                LiveLocationSession(
                    requestId = requestId,
                    requesterId = aliceUid,
                    requesterName = "Tanvir Hasan",
                    authorizedDonorId = bobUid,
                    authorizedDonorName = "Rafiqul Islam",
                    latitude = 23.8069,
                    longitude = 90.3687,
                    accuracyMeters = 10.5,
                    isActive = true,
                    expiresAtEpochMs = expiresAt
                )
            )
            repository.updateBloodRequestLiveSessionMetadata(
                requestId = requestId,
                authorizedDonorId = bobUid,
                authorizedDonorName = "Rafiqul Islam",
                liveSessionActive = true,
                liveSessionExpiresAt = expiresAt
            )
        }

        // Authorized donor Bob can read the live location session
        signInTestUser(BOB_EMAIL)
        val bobReadSession = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getLiveLocationSession(requestId)
        }
        assertNotNull(bobReadSession)
        assertEquals(23.8069, bobReadSession?.latitude ?: 0.0, 0.001)

        // Unauthorized third-party Charlie is blocked from reading the live location session
        signInTestUser(CHARLIE_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.getLiveLocationSession(requestId)
            }
            fail("Expected PERMISSION_DENIED for unauthorized third-party reading live_locations")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }

        // Requester Alice updates status to fulfilled and deletes the live location session
        signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateBloodRequestStatus(requestId, "fulfilled")
            repository.deleteLiveLocationSession(requestId)
        }

        // Synchronized donation history test (including donationTime, procedureConfirmation, ruleVersion & public timeline query)
        val donationId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addDonationRecord(
                DonationRecord(
                    userId = aliceUid,
                    donorName = "Tanvir Hasan",
                    hospitalName = "Delta Medical College Hospital",
                    location = "Mirpur-1, Dhaka",
                    recipientName = "Karim Uddin",
                    bloodGroup = "O+",
                    donationDate = "2026-09-12",
                    donationTime = "11:45",
                    donationType = "platelets",
                    procedureConfirmation = "Apheresis Slip #402",
                    eligibilityIntervalDays = 14L,
                    ruleVersion = "v1.0",
                    nextEligibleDate = "2026-09-26",
                    verificationStatus = "verified",
                    visibility = "public",
                    notes = "Single donor platelets"
                )
            )
        }
        assertTrue(donationId.isNotEmpty())

        // Also test "other" donation type when rule is unconfigured (eligibilityIntervalDays = 0, nextEligibleDate = "UNDECIDED")
        val otherDonationId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addDonationRecord(
                DonationRecord(
                    userId = aliceUid,
                    donorName = "Tanvir Hasan",
                    hospitalName = "National Heart Foundation",
                    location = "Mirpur-2, Dhaka",
                    bloodGroup = "O+",
                    donationDate = "2026-10-24",
                    donationTime = "16:20",
                    donationType = "other",
                    customDonationTypeName = "Convalescent Plasma",
                    procedureConfirmation = "Lab Ref #89",
                    eligibilityIntervalDays = 0L,
                    ruleVersion = "v1.0-unconfigured",
                    nextEligibleDate = "UNDECIDED",
                    verificationStatus = "verified",
                    visibility = "public",
                    notes = "Unconfigured custom donation type"
                )
            )
        }
        assertTrue(otherDonationId.isNotEmpty())

        // Another user (Bob) can observe Alice's public donation history timeline
        signInTestUser(BOB_EMAIL)
        val publicHistory = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublicDonorDonations(aliceUid).first { list ->
                list.any { it.id == donationId } && list.any { it.id == otherDonationId }
            }
        }
        assertTrue(publicHistory.any { it.id == donationId && it.donationType == "platelets" && it.nextEligibleDate == "2026-09-26" })
        assertTrue(publicHistory.any { it.id == otherDonationId && it.donationType == "other" && it.nextEligibleDate == "UNDECIDED" })
    }

    @Test
    fun observeCommunityBloodRequests_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = MbdRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCommunityBloodRequests().first()
            }
            fail("Expected exception when observing blood requests unauthenticated")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@mbd.org"
        const val BOB_EMAIL = "bob@mbd.org"
        const val CHARLIE_EMAIL = "charlie@mbd.org"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}

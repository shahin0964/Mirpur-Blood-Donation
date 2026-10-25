# Mirpur Blood Donation (মিরপুর ব্লাড ডোনেশন)

[![Android CI & Release Workflow](https://github.com/mirpurblooddonation/mbd/actions/workflows/android_release.yml/badge.svg)](https://github.com/mirpurblooddonation/mbd/actions)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![UI Tooling](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-red.svg)
![Localization](https://img.shields.io/badge/Bilingual-Bengali%20%2F%20English-orange.svg)

---

## 📌 About Mirpur Blood Donation

**Mirpur Blood Donation (মিরপুর ব্লাড ডোনেশন)** is a specialized Android application designed to bridge the critical gap between emergency blood requesters and volunteer blood donors in Mirpur and surrounding areas of Dhaka, Bangladesh. 

By facilitating real-time donor discovery, medical eligibility calculations, live location assistance, and instantaneous request broadcasts, the app ensures rapid response during life-critical medical emergencies.

---

## 🌟 Main Features

1. **Secure Multi-Method Authentication**
   - Seamless sign-in via Google Authentication and Phone OTP verification.
   - Profile creation tailored for both active blood donors and community requesters.

2. **Emergency Blood Request System**
   - Rapid emergency blood request submission (Blood Group, Units, Hospital Location, Urgency, Contact Person).
   - Real-time notification and filtering system matching requesters with available donors of compatible blood groups.
   - Status tracking from creation to fulfillment or cancellation.

3. **Smart Donor Search & Live Map Navigation**
   - Instant donor directory search filtered by blood group, availability status, and geographic sub-areas within Mirpur.
   - Live location sharing for emergency navigation to medical centers and blood donation sites.

4. **Medical Eligibility Rules Engine**
   - Automated 120-day donation interval calculation determining real-time donor eligibility.
   - Clear eligibility badges and countdown timers notifying donors when they are next eligible to donate.

5. **Complete Bilingual Interface (বাংলা & English)**
   - Instantaneous full-app language toggling between Bengali (বাংলা) and English.
   - Zero mixed-language UI strings across all screens, dialogs, forms, toasts, and notifications.

6. **Comprehensive Admin Panel**
   - Dedicated administrative control panel for user role management, donor verification, and emergency blood request moderation.
   - System announcement broadcast tools and remote application update controls.

7. **Automated App Update & Mandatory Lock System**
   - In-app check against GitHub release version manifests.
   - Seamless background APK downloading, FileProvider installation trigger, and mandatory update enforcement for critical security releases.

---

## 🛠 How It Works

### For Requesters
1. Log in securely using Google or Phone OTP.
2. Submit an emergency blood request detailing the required blood group, hospital location, and urgency.
3. Track responses and connect directly with matching nearby donors.

### For Donors
1. Register donor details including blood group and last donation date.
2. Receive immediate alerts when emergency blood requests match your blood group.
3. Accept requests, coordinate with requesters, and track your next eligibility date.

---

## 👥 User Roles & Access

- **Standard Users (Requesters & Donors):** Search for donors, post emergency blood requests, manage personal profiles, view donation history, and receive blood match notifications.
- **Administrators:** Verify donor listings, moderate emergency blood posts, manage system announcements, and publish mandatory app update policies.

---

## 🔒 Privacy & Security

- **Strict Repository Privacy:** Source code and internal configurations remain confidential.
- **Zero Hardcoded Credentials:** All environment configs and API integrations use secure Android build mechanisms.
- **Data Protection:** Personal contact information and location data are exposed strictly for verified emergency blood coordination purposes.

---

## 🚀 App Updates & Distribution Pipeline

Mirpur Blood Donation utilizes an automated **GitHub Actions CI/CD Pipeline**:
- Every production commit triggers automated Android release builds (`gradle assembleDebug`).
- Generates a structured release `version_manifest.json` along with the installable APK.
- Cleanly manages GitHub Releases, keeping distribution points up-to-date.
- In-app `UpdateManager` periodically checks the remote version manifest to prompt or mandate updates.

---

## 🏛 Technical Architecture

- **Language:** 100% Kotlin
- **UI Framework:** Jetpack Compose with Material Design 3
- **Architecture Pattern:** MVVM (Model-View-ViewModel) + Clean Data Layer
- **Asynchronous Operations:** Kotlin Coroutines & Shared/StateFlow
- **Theme & Palette:** Custom M3 Crimson theme optimized for high legibility and emergency accessibility

---

## 📦 Download & Support

Official APK releases are made available through our automated distribution pipeline. For emergency assistance or app support, please contact the administration team.

---

## ⚠️ Disclaimer

*Mirpur Blood Donation is a non-profit voluntary community matching platform. It is not a clinical healthcare provider or blood bank. All donors must undergo standard pre-donation medical screening at licensed clinical facilities prior to transfusion.*

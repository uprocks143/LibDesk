# LibDesk - Multi-Tenant Library & Study Space SaaS Platform

LibDesk is an offline-first, enterprise-grade Android application engineered for operating and scaling modern study spaces, reading rooms, and library chains. It delivers a unified, role-governed multi-tier architecture:

1. **👑 Super Admin (SaaS Platform Governance)** — Oversees subscription tiers, plan approvals, revenue analytics (MRR), and institute lifecycles with strict, non-bypassable Supabase authentication and 2-Factor Authentication (2FA). Accessible via an edge-to-edge full-screen mobile management console.
2. **🏢 Library Owner / Manager Operations** — End-to-end administration including real-time Supabase cloud sync, live capacity & occupancy progress meters, interactive seat matrices, shift scheduling, student enrollment, fee collections, real-time attendance registers, and direct WhatsApp communication.
3. **🧑‍🎓 Student Self-Service Portal** — Real-time seat availability, dynamic 30-second rotating entrance pass, PDF reader with NCERT catalog, fee receipts, and digital ID cards.

Built natively with **Kotlin**, **Jetpack Compose (Material Design 3)**, **Room Database**, and **Supabase (PostgreSQL + Auth API)**.

---

## ✨ Key Features & Capabilities

### 🏢 Library Owner & Manager Suite
* **Real-Time Supabase Cloud Sync**: Live cloud data fetching and bi-directional synchronization with Supabase PostgreSQL database.
* **Live Occupancy & Capacity Dashboard**: 
  - Real-time seat calculation: Total Registered Capacity, Currently Occupied Seats, Vacant/Free Seats, and Live In-Hall Attendance.
  - Visual color-coded capacity meter (`<75% Normal`, `75-90% High Demand`, `>90% At Capacity`).
  - Direct "Fetch Supabase" button with animated sync indicator.
* **Interactive Seat Matrix**: Visual desk allocation with custom zones, real-time occupancy status (Occupied, Available, Reserved, Maintenance), and instant desk assignments.
* **Multi-Shift Management**: Flexible configurations for morning, evening, night, and full-day shifts with auto-calculated seat allocations.
* **Fee Collection & Payment Tracking**: Comprehensive fee ledgers, partial payment handling, dynamic UPI QR generation, and detailed payment history.
* **Direct WhatsApp Integration**:
  * **Zero Contact Picker Interruption**: Directly opens chat with the student's registered mobile number using normalized `91` country code formatting (`api.whatsapp.com` and direct WhatsApp intent package targeting).
  * **Fee Receipts & Reminders**: Instantly share itemized payment receipts and overdue fee alerts directly to the student's WhatsApp.
  * **Attendance Alerts**: One-tap attendance alerts (check-in / check-out timestamps) sent straight to the registered student or parent.
  * **Daily Shift Reports**: Share comprehensive morning, evening, and daily attendance summaries via WhatsApp.
* **Attendance Register & Scanner**: Embedded CameraX QR scanner for fast student check-in/check-out with anti-fraud rolling validation and manual punch overrides.
* **Digital Library & Resource Hub**: Manage study materials, mock tests, and PDFs with persistent Android Storage Access Framework (SAF) URI support.
* **Notice Board & Student Helpdesk**: Broadcast urgent notices and resolve student desk/facility complaints in real time.

### 👑 Super Admin (SaaS Management & Security)
* **Dedicated Full-Screen Console**: Edge-to-edge mobile-optimized interface with system bar padding and keyboard awareness.
* **Discrete Access Point**: Clean bottom console button for developers/owners, leaving the top login switcher uncluttered for Library Owners and Students.
* **Single-Slot Protected Governance**: Exactly one Super Admin account is permitted per deployment to ensure absolute platform control.
* **Strict Supabase Authentication**: Eliminates hardcoded master PINs and backdoors. Super Admin logins are validated strictly against cloud authentication credentials.
* **Mandatory 2FA Email OTP Verification**: High-security two-factor authentication enforced via Supabase Auth email tokens (`auth/v1/verify`) before granting administrative access.
* **SaaS Subscription Lifecycle**: Review institution tiers (15-Day Free Trial, Standard, Pro, Enterprise), verify UPI payment proofs, and activate licenses.
* **Network Telemetry & MRR Analytics**: Instant visibility into active libraries, total seats deployed, occupancy rates, and recurring revenue.

### 🔐 Authentication & Session Persistence
* **Zero Hardcoded Credentials**: No static user IDs or passwords stored in the app; all fields initialize empty for genuine security.
* **Dynamic "Remember Me" Support**: Optional credential caching via private SharedPreferences (`libdesk_remember_me_prefs`) for seamless recurring logins.
* **Self-Service Password Recovery**: In-app OTP email verification flow for resetting forgotten passwords via Supabase.

### ☁️ Enterprise Google Drive Cloud & Local Data Backup
* **AES-256 GCM Cloud Encryption**: Every snapshot is securely encrypted locally before upload so that only authorized administrators can restore it.
* **Google Drive Cloud Vault**: Seamless OAuth2 integration to back up and restore database snapshots directly to/from Google Drive.
* **Automated & Manual Sync**: Configurable auto-backup frequencies (Daily, Weekly, Monthly) and network constraints (Wi-Fi Only vs Cellular).
* **One-Tap Cloud & Device Restore**: Restore complete database snapshots in seconds with automatic migration, ensuring zero data loss during device replacements or resets.
* **Standalone Encrypted Export**: Export standalone `.enc` database archives for air-gapped storage or archival.

### 🧑‍🎓 Student Self-Service Portal
* **Instant Self-Enrollment**: Scan library entrance posters, complete profiles, and verify identity via email OTP.
* **Anti-Fraud Rolling QR Pass**: Dynamic entrance QR code regenerating every 30 seconds to prevent unauthorized entry via screenshots.
* **Live Desk & Shift Status**: Check allocated desk numbers, view shift timings, and submit seat change or locker requests.
* **Integrated Study Material & NCERT Catalog**: In-app PDF reader featuring night mode, zoom support, page bookmarks, and direct NCERT textbook imports.
* **Digital ID Card**: Tamper-evident digital student ID with library branding, membership status, and QR verification.

---

## 🛠 Tech Stack

* **Language**: [Kotlin](https://kotlinlang.org/) (Coroutines, Flow, StateFlow)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3 with custom semantic theming)
* **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) with SQLite (Full Offline-First capability)
* **Cloud & Auth**: [Supabase](https://supabase.com/) (PostgreSQL, Auth API with secure OTP verification)
* **Cloud Storage**: Google Drive REST API v3 with Google Sign-In OAuth2 tokens
* **Camera & Scanner**: Android CameraX with ZXing Core (30-second rolling TOTP QR generation & scanning)
* **Networking**: OkHttp 4 & Kotlinx Serialization

---

## 📁 Architecture Overview

```text
app/src/main/java/com/example/
├── data/
│   ├── backup/         # Google Drive Cloud API, AES-256 GCM Encrypted Vault, Local Manager
│   ├── local/          # Room Entities, DAOs, TypeConverters, and AppDatabase
│   ├── remote/         # Supabase Client, Auth Service, and Session Manager
│   └── repository/     # Unified Repository coordinating Room and Supabase sync
├── ui/
│   ├── auth/           # Login, Role Selection, Supabase Auth, and Super Admin Portal
│   ├── backup/         # Enterprise Google Drive & Local Backup Settings Screen
│   ├── components/     # TopAppBar, Navigation Drawer, RoleGate, and Status Badges
│   ├── manager/        # Dashboard, Live Supabase Capacity Card, Seat Matrix, Finance
│   ├── student/        # Dynamic Rolling QR Pass, Digital ID, PDF Study Viewer
│   ├── superadmin/     # Full-screen SaaS Subscriptions, Plan Management, Revenue Metrics
│   └── scanner/        # CameraX QR Attendance Scanner
├── util/
│   ├── ImageShareUtils.kt  # Direct WhatsApp message and receipt sharing engine
│   ├── payment/            # UPI QR and Payment Gateway Helpers
│   ├── EmailOtpService.kt  # Supabase 2FA & OTP verification service
│   └── TotpGenerator.kt    # Rolling dynamic QR code security generator
└── viewmodel/              # LibDeskViewModel & BackupViewModel
```

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Jellyfish / Koala (or newer)
* Android SDK 34 (compileSdk: 34, minSdk: 26)
* JDK 17 or JDK 21

### Build & Run
1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-org/libdesk.git
   ```
2. **Open in Android Studio** and synchronize Gradle dependencies.
3. **Verify compilation:**
   ```bash
   gradle assembleDebug
   ```
4. **Execute local JVM unit tests:**
   ```bash
   gradle testDebugUnitTest
   ```

---

## 🛡️ Security & Verification Summary
* **No Authentication Backdoors**: Super Admin and administrative sessions are strictly verified via Supabase Auth and mandatory 2FA email codes.
* **No Hardcoded Credentials**: Initial inputs start completely blank; only cached when user selects "Remember me".
* **AES-256 GCM Cloud Snapshots**: Database backups uploaded to Google Drive are encrypted at the client level before transmission.
* **Direct Targeting WhatsApp Communication**: Alerts and payment receipts are sent directly to the student's validated mobile number, eliminating human routing errors.
* **Ephemeral TOTP Credentials**: Gate check-in QR codes expire every 30 seconds to prohibit credential sharing and screenshot forgery.
* **Multi-Tenant RLS Policies**: Isolated PostgreSQL row-level security ensuring libraries only access their respective data partitions.

# LibDesk - Multi-Tenant Library & Study Space SaaS

LibDesk is an offline-first, enterprise-ready Android application designed for running and scaling library and study-center businesses on a unified platform. It delivers a comprehensive multi-tier architecture:

1. **👑 Super Admin (SaaS Platform Owner)** — Manage subscriptions, plans, payment approvals, system metrics, and institute lifecycles with mandatory 2-Factor Authentication (2FA).
2. **🏢 Library Owner / Manager** — Operate day-to-day library operations including visual seat layouts, multiple shifts, student records, fee collection, dynamic QR attendance, and digital materials.
3. **🧑‍🎓 Student Portal** — Self-enrollment, real-time desk availability, dynamic 30-second rotating entrance pass, PDF reader with NCERT catalog, fee receipts, and complaint tracking.

Built natively with **Kotlin** and **Jetpack Compose (Material 3)**.

---

## ✨ Key Features

### 👑 Super Admin (SaaS Management)
* **Single-Slot Protected Governance**: Exactly one Super Admin account is permitted per deployment to ensure total platform security.
* **Mandatory 2FA Authentication**: Email OTP verification is enforced for Super Admin account access and sensitive operations.
* **Subscription Management**: Oversee library tiers (Free Trial, Standard, Pro, Enterprise), review activation requests, and handle payment proofs.
* **Global Analytics**: High-level telemetry covering active institutions, total seats deployed, system occupancy, and MRR.

### 🏢 Library Owner & Manager Suite
* **Interactive Seat Matrix**: Visual desk allocation with custom zones, real-time occupancy status (Occupied, Available, Reserved, Maintenance), and instant desk assignments.
* **Flexible Shifts**: Full support for morning, evening, night, and full-day shifts with auto-calculated pricing and slot limits.
* **Fee Collection & UPI Integration**: Track dues, collect monthly fees via dynamic UPI QR code intents, record partial payments, and issue downloadable/shareable payment receipts.
* **Dynamic Attendance & Scanner**: Embedded CameraX QR scanner for instantaneous student check-in/check-out with geolocation validation and haptic feedback.
* **Digital Library**: Manage study materials, mock tests, and PDFs with persistent Android Storage Access Framework (SAF) URI support.
* **Notice Board & Complaints**: Broadcast urgent updates to all enrolled students and resolve desk/facility complaints in real time.
* **Data Backup & Restore**: Offline-first Room persistence with one-tap Local Zip export/import and automated cloud backup capabilities.

### 🧑‍🎓 Student Self-Service Portal
* **Easy Self-Enrollment**: Discover libraries, scan entrance QR codes, and sign up with email OTP verification.
* **Anti-Fraud Entrance QR Pass**: Rolling-token dynamic QR code that regenerates every 30 seconds to prevent unauthorized entry via screenshots.
* **Real-time Desk Status**: Check available seats, current shifts, and submit seat change or locker requests.
* **Built-in PDF Viewer & NCERT Catalog**: Offline study material reader with native zooming, night mode, page bookmarking, and official NCERT textbook imports.
* **Fee & ID Card Management**: Digital identity card with membership status, renewal reminders, and payment history.

---

## 🛠 Tech Stack

* **Language**: [Kotlin](https://kotlinlang.org/) (100% Coroutines & Flow)
* **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3 with semantic theming)
* **Architecture**: MVVM with Clean Architecture principles & Repository pattern
* **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) with SQLite (Offline-First)
* **Cloud & Auth**: [Supabase](https://supabase.com/) (PostgreSQL backend, Auth with auto-token refresh, OTP services)
* **Camera & QR**: CameraX & ZXing Core (30-second TOTP dynamic QR generation and scanning)
* **Networking**: OkHttp 4 & Kotlinx Serialization

---

## ⚙️ Configuration & Environment

LibDesk uses modern Android build configuration with fallback defaults to ensure zero-configuration development:

1. Create a `.env` file in the project root (or copy `.env.example`):
   ```properties
   SUPABASE_URL=https://<your-project-id>.supabase.co
   SUPABASE_ANON_KEY=<your-supabase-publishable-or-anon-key>
   ```

2. **Built-in Resilience**:
   * If `.env` contains placeholders (e.g. `your-project.supabase.co`), the app automatically falls back to configured production endpoints via `SupabaseClient.getEffectiveUrl()` and `SupabaseClient.getEffectiveApiKey()`.
   * DNS resolution and network timeouts are guarded with graceful fallback error messages.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Iguana / Jellyfish / Koala (or newer)
* Android SDK 34 (compileSdk: 34, minSdk: 26)
* JDK 17 or JDK 21

### Build & Run
1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-org/libdesk.git
   ```
2. **Open in Android Studio** and let Gradle synchronize dependencies.
3. **Run Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. **Build APK:**
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📁 Architecture Overview

```text
app/src/main/java/com/example/
├── data/
│   ├── backup/         # Local Zip & Cloud Drive backup engines
│   ├── local/          # Room Entities, DAOs, and AppDatabase
│   ├── remote/         # Supabase Client, Auth Service, and Session Manager
│   └── repository/     # Single source of truth data repository
├── ui/
│   ├── auth/           # Login, Signup, OTP, and Super Admin claim gates
│   ├── manager/        # Owner dashboard, seat matrix, fees, students, notices
│   ├── student/        # Student portal, dynamic QR pass, digital library, profile
│   ├── superadmin/     # SaaS subscription management, plan builder, audit log
│   ├── scanner/        # CameraX QR check-in modal with vibration & feedback
│   └── theme/          # M3 color schemes, typography, and semantic LibDeskColors
├── util/               # TOTP generator, PDF helpers, UPI intent handler, OTP service
└── viewmodel/          # LibDeskViewModel and BackupViewModel
```

---

## 🛡️ Security & Privacy
* Passwords and PINs are hashed using cryptographic one-way digests before storage.
* QR access tokens expire every 30 seconds using synchronized timestamp hashes.
* Storage Access Framework permissions are persisted to maintain offline document availability without requesting dangerous storage permissions.


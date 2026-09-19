# LibDesk - Multi-Tenant Library & Study Space SaaS

LibDesk is an offline-first Android app for running one or many library / study-center
businesses on a single platform. A Super Admin manages SaaS subscriptions across every
library on the platform; each Library Owner/Manager runs their own institute (students,
seats, fees, attendance); and Students can discover a library, self-enroll, and manage
their own membership — all from one app, built with **Kotlin** and **Jetpack Compose**.

## ✨ Key Features

* **👑 Super Admin (SaaS) Dashboard** — Overview stats, per-library subscription status
  (active/expired/suspended/pending), manual payment approval queue, full CRUD on SaaS
  plans, and account settings. Exactly **one** Super Admin account can exist at a time,
  and **2-Factor email OTP is mandatory** on every login — it cannot be turned off from
  the signup form or settings.
* **📚 Membership Management** — Student profiles, membership plans, fee collection, and
  outstanding dues, with payment math that only ever runs through a single source of
  truth (no double-counted payments).
* **🪑 Interactive Seat Matrix** — Visual desk allocation, shift management, and booking
  tracking across halls and zones.
* **📱 Dynamic Entrance QR Pass** — Rolling-token QR code for turnstile access and instant
  seat check-ins, refreshing every 30 seconds to prevent screenshot sharing.
* **📖 Digital Study Library** — Publish PDFs/notes/PYQs with a native offline PDF viewer.
  Includes a one-tap **"Embed NCERT"** import that live-verifies and pulls real, official
  NCERT textbook PDFs (Class 6 & 10 so far) directly from ncert.nic.in — no fake or
  placeholder content, and files students pick up (owner uploads) are downloaded with a
  persistent permission grant so they keep working after the app restarts.
* **🧑‍🎓 Student Self-Enrollment** — Students scan a library's QR (or pick it from a list)
  and sign up with a professional, validated registration form: mandatory email OTP
  verification, required-field checks, a Terms of Service checkbox, and no silently
  auto-selected library.
* **🔐 Account Security** — Every registration form (owner and student) requires a real
  password with confirmation — the app never silently assigns a guessable default
  password to an account left blank.
* **📝 First-Time Profile Completion** — A Library Owner/Manager whose library profile is
  missing key business details (address, city, state, UPI ID) is walked through a
  mandatory one-time completion screen before they can use the rest of the app, so real
  payment QR codes never point at blank or placeholder data.
* **📶 Offline-First Architecture** — Powered by **Room Database**; core features work
  without an internet connection and sync when it returns.
* **☁️ Cloud Synchronization** — Supabase-backed sync with real session-token refresh, so
  a signed-in session doesn't silently go stale mid-use.
* **🌓 Modern UI & Theming** — Jetpack Compose with a single Material3 color system
  (Dark/Light) applied consistently across screens.
* **🔔 Notifications & Reminders** — Local alerts for membership expirations, fee dues,
  and check-in statuses.

## 🛠 Tech Stack

* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
* **Local Database:** [Room](https://developer.android.com/training/data-storage/room)
* **Asynchronous Programming:** [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
* **Cloud / Backend Sync:** [Supabase](https://supabase.com/) (Auth with token refresh, Postgres sync)
* **Networking:** OkHttp
* **QR Code Generation:** ZXing Core
* **Camera / Scanning:** CameraX

## 🚀 Getting Started

### Prerequisites
* Android Studio (latest stable recommended)
* Minimum SDK: 26 (Android 8.0)
* Target SDK: 34 (Android 14)
* A Supabase project (URL + anon key) for cloud sync and auth

### Installation
1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/libdesk.git
   ```
2. **Open the project** in Android Studio.
3. **Sync Gradle files** to download all dependencies.
4. **Run the app** on an emulator or a physical device.

### First Run
* The very first person to claim the Super Admin slot does so from the login screen's
  "Claim Admin Access" flow — this account is permanent and unique; a 2FA email OTP is
  required on every subsequent login.
* A Library Owner registers through "Register Your Library", verifies their email with a
  one-time code, and is then required to complete their library's address/city/state/UPI
  details before the dashboard unlocks.
* A Student scans a library's entrance QR (or selects it from the list) to sign up, and
  verifies their email before their account is created.

## 📁 Project Structure

* `data/local/` — Room entities, DAOs, and the database builder (`AppDatabase.kt`).
* `data/remote/` — Supabase auth/session/sync (`SupabaseAuthService.kt`,
  `SupabaseSyncManager.kt`, `SessionManager.kt`) and the live NCERT catalog fetcher
  (`NcertCatalogService.kt`).
* `data/repository/` — Single source of truth for data access (`LibDeskRepository.kt`).
* `ui/` — Jetpack Compose screens (`auth/`, `manager/`, `student/`, `superadmin/`,
  `subscription/`, `profile/`) and reusable components.
* `ui/theme/` — Colors, typography, shapes, and the theme-aware `LibDeskColors` semantic
  palette used app-wide instead of hardcoded hex values.
* `viewmodel/` — UI state management (`LibDeskViewModel.kt`).

## 🛡 License
This project is for educational and portfolio purposes.

---
*Built with LibDesk's own team, with fixes and hardening passes done with Claude.*

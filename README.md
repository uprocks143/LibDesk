# LibDesk - Library & Study Space Management App

LibDesk is a modern, offline-first Android application designed to manage library memberships, study desk allocations, attendance, and facility access. Built entirely with **Kotlin** and **Jetpack Compose**, it provides a seamless experience for both administrators and student members.

## ✨ Key Features

*   **📚 Membership Management:** Easily manage student profiles, membership plans, fee payments, and outstanding dues.
*   **🪑 Interactive Seat Matrix:** Visual desk allocation, shift management, and booking tracking for different halls and zones.
*   **📱 Dynamic Entrance QR Pass:** A highly secure, rolling-token QR code generator for turnstile access control and instant seat check-ins. Features a 30-second refresh cycle to prevent screenshot sharing.
*   **📶 Offline-First Architecture:** Powered by **Room Database**, allowing core features like profile viewing and QR pass generation to work perfectly without an internet connection.
*   **☁️ Cloud Synchronization:** Background sync capabilities powered by **Supabase** to ensure local data is safely backed up and consistent across devices.
*   **🌓 Modern UI & Theming:** Fully responsive Jetpack Compose interface with robust Dark/Light mode support (Classic Oxford Light & Oxford Midnight).
*   **🔔 Notifications & Reminders:** Automated local alerts for membership expirations, fee dues, and check-in statuses.

## 🛠 Tech Stack

*   **Language:** [Kotlin](https://kotlinlang.org/)
*   **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **Local Database:** [Room](https://developer.android.com/training/data-storage/room)
*   **Asynchronous Programming:** [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
*   **Cloud / Backend Sync:** [Supabase](https://supabase.com/)
*   **QR Code Generation:** ZXing Core
*   **Camera / Scanning:** CameraX

## 🚀 Getting Started

### Prerequisites
*   Android Studio (Latest stable version recommended)
*   Minimum SDK: 26 (Android 8.0)
*   Target SDK: 34 (Android 14)

### Installation
1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/libdesk.git
    ```
2.  **Open the project** in Android Studio.
3.  **Sync Gradle files** to download all necessary dependencies.
4.  **Run the application** on an emulator or a physical Android device.

## 📁 Project Structure

*   `data/local/` - Contains Room Database entities, DAOs, and the database builder (`AppDatabase.kt`).
*   `data/remote/` - Contains cloud synchronization logic (e.g., `SupabaseSyncManager.kt`).
*   `data/repository/` - The single source of truth for data access (`LibDeskRepository.kt`).
*   `ui/` - Contains all Jetpack Compose screens and reusable components (e.g., `ProfileScreen`, `DynamicEntranceQrCard`).
*   `ui/theme/` - Contains color palettes, typography, and shape definitions.
*   `viewmodel/` - Contains the UI state management logic (`LibDeskViewModel.kt`).

## 🛡 License
This project is for educational and portfolio purposes. 

---
*Built with ❤️ using Google AI Studio.*

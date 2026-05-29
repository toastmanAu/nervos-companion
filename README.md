# Nervos Network Companion App

An all-in-one unofficial Android companion application for the **Nervos Network (CKB)** built with Kotlin, Jetpack Compose, and Material 3.

---

## Features Implemented

*   **Persistent Bottom Navigation**: Type-safe navigation utilizing the modern `androidx.navigation3` API.
*   **Home Dashboard**: Displays real-time chain statistics (Tip block, Epoch index/progress), live CKB pricing data (price, 24h change, volume, and market cap from CoinGecko), and latest news highlights.
*   **News Feed Aggregator**: Dynamic news feed compiling forum posts from `talk.nervos.org` and curated articles. Features tags, custom search filters, and source chips.
*   **Ecosystem Directory**: Clean showcase of popular CKB applications (JoyID, YokaiSwap, .bit, and Nervos DAO) styled in custom 8:3 ratio banner cards.
*   **Dynamic Remote Configurations**: Remote config URL setting allows dynamically loading app cards (`apps.json`) and links from a GitHub repository.
*   **Developer Tools & Console**:
    *   **Nervos DAO Web View**: Embedded viewport pointing to `daoview.org`.
    *   **CKB RPC Terminal Console**: Interactive terminal allowing raw JSON-RPC commands with auto-completion, parameter parsing, and pretty-printed JSON results.
*   **Visual Polish & Themes**: Toggles instantly between four visual palettes: *Emerald Forest* (Nervos Green), *Cyberpunk Neon*, *Midnight Ocean*, and *Obsidian Stealth*.

---

## Tech Stack

*   **UI Framework**: Jetpack Compose (Material 3)
*   **Language**: Kotlin
*   **Navigation**: AndroidX Navigation 3
*   **JSON-RPC Client**: Custom lightweight `HttpURLConnection` wrapper
*   **Storage**: Android SharedPreferences

---

## Roadmap & Next Milestones

*   [ ] **Room Database Caching**: Cache fetched news items, apps directory, and chain stats locally for offline-first support.
*   [ ] **WorkManager Polling**: Implement automated background workers to query chain tip status and send notifications on block events.
*   [ ] **Push Notifications**: Integrate Firebase Cloud Messaging (FCM) or local alerts for chain events or curated announcements.
*   [ ] **Transaction Viewer & Calculator**: Add tools for parsing CKB transaction hashes and calculating DAO yields.
*   [ ] **Fiber Network Integration**: Next-generation lightning network channel monitoring features.

---

## Getting Started

### Prerequisites

*   JDK 17 or higher (configured using gradle properties)
*   Android SDK command-line tools

### Build and Run

1.  **Build the Debug APK**:
    ```bash
    ./gradlew assembleDebug
    ```
2.  **Deploy and Run on Emulator/Device**:
    ```bash
    android run --debug
    ```

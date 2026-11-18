# Vault Spend — Smart Android SMS Expense Tracker

A privacy-first native Android expense tracker built with **Kotlin**, **Jetpack Compose**, and **Room**, featuring a **CRED-inspired AMOLED Black & Light Mode UI** and an on-device multi-pass financial SMS classification engine.

## Features

- **Dual-Mode CRED Aesthetic (AMOLED Black + Light Mode)**
  - True `#000000` pitch-black AMOLED mode by default with an instant top-bar Dark/Light mode toggle.
  - Rolling odometer spend counters, directional horizontal tab transitions, interactive 3D-flipping credit cards, scrollable monthly spend velocity bar charts, and animated category donut charts.
- **Autonomous Multi-Pass Expense AI (`OfflineExpenseAiEngine` + `IterativeLedgerAnalyzer`)**
  - Parses Indian bank, Credit Card, UPI, NEFT/IMPS, and Wallet SMS alerts directly on-device.
  - Curated merchant intelligence database (`750+` merchant codes, UPI VPA prefixes, QR aggregators, and ISO 18245 MCC codes) with fuzzy bigram & semantic keyword classification.
  - Automatically separates **Credit Card Bill Settlements** (e.g., `CRED Club`, `Kiwi`, `CheQ`, card bill payments) and **Self Transfers** between own accounts so monthly spends and credits are never double-counted.
  - Filters out OTPs, promotional coupons, e-mandate pre-debit reminders, and duplicate card statement summaries in memory.
- **Complete Spend Control**
  - Dedicated **Credit Cards**, **UPI**, and **Wallets** views.
  - Instant auto-save for custom merchant rules, category overrides, personal searchable notes, and per-transaction spend inclusion toggles, plus a 5-second undo bar on delete.
  - Full-text search across raw SMS bodies, personal notes, merchant names, banks, UPI apps, categories, and amounts.
- **Biometric & Passcode Security**
  - Salted SHA-256 4-digit passcode + Android `BiometricPrompt` (Fingerprint / Face) with 3-question personal passcode recovery and `FLAG_SECURE` task-switcher protection.
  - Zero network permissions (`android.permission.INTERNET` omitted from `AndroidManifest.xml`).

## Tech Stack

- **Language**: Kotlin 1.9 (`jvmTarget = 17`)
- **UI**: Jetpack Compose (Material 3, Custom Canvas Charts, Custom Variable Fonts)
- **Persistence**: Room SQLite (`v6` schema with migrations)
- **Security**: `androidx.biometric`, Salted SHA-256 Auth & Recovery

## Installing the APK (Android / Play Protect Guide)

Because Vault Spend reads bank/UPI SMS alerts on-device (`READ_SMS` / `RECEIVE_SMS`) without a Play Store listing, Google Play Protect's Enhanced Fraud Protection may pause sideloaded APK installs downloaded from a browser or GitHub.

### Method 1: Install Directly on Your Phone (No PC Required)
1. Download **`app-release.apk`** from the **Releases** section.
2. Open **Google Play Store** -> tap your **Profile icon** (top-right) -> **Play Protect** -> **Settings** (gear icon) -> temporarily pause **Scan apps with Play Protect**.
3. Tap the downloaded **`app-release.apk`** to install it (you can re-enable Play Protect afterward).
4. **Android 13+ Restricted Settings**: Before granting SMS permission, open **Settings -> Apps -> Vault Spend -> `⋮` (top-right menu) -> Allow restricted settings**, then open Vault Spend and grant SMS access.

### Method 2: Install via PC (`adb`) — No Play Protect Prompt
Enable **Developer Options -> USB Debugging** on your Android phone, connect via USB, and run:

```bash
adb install -r -g app-release.apk
```

## Build from Source

```bash
# Run JVM unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Install onto a connected Android device via ADB
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
```

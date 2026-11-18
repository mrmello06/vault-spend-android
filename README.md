# Raizen — Smart Offline SMS Money Tracker

A privacy-first, 100% offline native Android expense tracker built with **Kotlin**, **Jetpack Compose**, and **Room SQLite**, featuring a **CRED-inspired Liquid Glass AMOLED UI** and an intelligent on-device financial SMS classification engine.

> **Zero Internet Access**: Raizen does not declare the `android.permission.INTERNET` permission in its Android manifest. It is physically impossible for your financial data or SMS to leave your phone.

---

## Key Features

- **Liquid Glass AMOLED Aesthetics (Pure `#000000` Black + Light Mode)**
  - True AMOLED pitch-black theme with floating translucent frosted liquid glass navigation dock.
  - Smooth, stutter-free animations calibrated for 60Hz, 120Hz, and 144Hz high-refresh displays.
  - Rolling odometer spend counters, interactive 3D-flipping credit cards, monthly spend velocity bar charts, and category breakdown charts.
  - Small one-tap toggle on top for switching between AMOLED Dark and Light modes.

- **Autonomous On-Device AI Ledger Engine**
  - Parses Indian bank, Credit Card, UPI, NEFT/IMPS, and Wallet SMS alerts locally in real-time.
  - Recognizes bank alerts across **HDFC, ICICI, SBI, Axis, Kotak, PNB**, and all major Indian financial institutions.
  - **Corporate Salary Detection**: Automatically identifies corporate salary credits (NEFT, ACH, BULK POSTING, payroll narrations) without misclassifying casual P2P transfers or refunds, highlighted in distinct Emerald Green (`#00E676`).
  - Separates **Credit Card Bill Settlements** (e.g., `CRED Club`, `Kiwi`, `CheQ`) and **Self Transfers** between own accounts so expenses are never double-counted.
  - Smart firewall filters out OTPs, promotional coupons, e-mandates, and marketing spam.

- **Card & Bill Manager**
  - Dedicated cards view with visual representations of your Credit and Debit cards.
  - 3D card flip with complete dark underlay to view card details and statements without white-glare artifacts.
  - Track bill cycles and payment due dates.

- **Biometric & Passcode Security**
  - Hardware-backed AES encryption + salted SHA-256 4-digit passcode + `BiometricPrompt` (Fingerprint/Face).
  - **Passcode is Optional**: Toggle *"Require Passcode to Open"* on or off based on your preference.
  - Secure Passcode Change: Requires entering the current passcode before allowing updates.
  - Instant *"Lock App Now"* button and 3-question recovery system.

---

## Download

Get the latest release from the [GitHub Releases](https://github.com/mrmello06/vault-spend-android/releases/latest) page:
- **`Raizen.apk`** — Production Release (Recommended)
- **`Raizen-debug.apk`** — Debug Build

---

## How to Install via ADB (Recommended)

Installing via ADB (**Android Debug Bridge**) installs the app directly onto your device with all permissions pre-granted, completely bypassing Google Play Protect's third-party sideload warnings.

### Prerequisites: Enable USB Debugging on Your Android Phone

1. Open your phone's **Settings** app.
2. Scroll to the bottom and tap **About phone** -> **Software information**.
3. Tap **Build number** repeatedly **7 times** until you see the prompt: *"You are now a developer!"*.
4. Return to the main **Settings** menu and open the new **Developer options** menu (usually at the very bottom, or under *System*).
5. Find **USB debugging** and toggle it **ON**.
6. Connect your phone to your computer using a USB cable.
7. A prompt will appear on your phone asking *"Allow USB debugging?"* — check **"Always allow from this computer"** and tap **Allow**.

---

### Step 1: Install ADB on Your Computer

If you don't already have ADB installed:

- **Windows**:
  - Run via PowerShell:
    ```powershell
    winget install Google.PlatformTools
    ```
  - *Or download the standalone [Android SDK Platform-Tools for Windows](https://developer.android.com/tools/releases/platform-tools) and extract the folder.*

- **macOS**:
  - Using Homebrew:
    ```bash
    brew install android-platform-tools
    ```

- **Linux (Ubuntu/Debian)**:
  ```bash
  sudo apt update && sudo apt install adb
  ```

---

### Step 2: Verify Device Connection

Open your terminal (PowerShell, Command Prompt, or Terminal) and run:

```bash
adb devices
```

You should see your connected phone listed:
```text
List of devices attached
RZCY11PVP0X    device
```
*(If it says `unauthorized`, unlock your phone screen and accept the USB Debugging prompt).*

---

### Step 3: Install Raizen

Open your terminal in the folder where you downloaded `Raizen.apk` and run:

```bash
adb install -r -g Raizen.apk
```

> **What these flags mean**:
> - `-r`: Replaces/updates an existing installation while preserving your local database and settings.
> - `-g`: Automatically grants all requested permissions (`READ_SMS`, `RECEIVE_SMS`, `POST_NOTIFICATIONS`) so you can start immediately without extra permission dialogs.

---

### Step 4 (Optional): Launch Raizen from Terminal

```bash
adb shell am start -n com.moneytracker.vault/.MainActivity
```

---

## Alternative: Installing Directly on Your Phone (No PC Required)

1. Download **`Raizen.apk`** on your phone from the [Releases](https://github.com/mrmello06/vault-spend-android/releases/latest) section.
2. **Play Protect Warning**: Because Raizen reads SMS alerts on-device without being published on Google Play, Google Play Protect may block or pause the installation:
   - Open **Google Play Store** -> tap your **Profile icon** (top-right) -> **Play Protect** -> tap the **Settings gear icon** (top-right).
   - Temporarily pause **Scan apps with Play Protect**.
3. Open your Downloads folder and tap **`Raizen.apk`** to install.
4. **Android 13+ Restricted Settings**:
   - If Android restricts the SMS permission for sideloaded apps:
   - Go to your phone's **Settings -> Apps -> Raizen -> tap the `⋮` (3 dots in top-right corner) -> Allow restricted settings**.
   - Open Raizen and allow SMS access when prompted.
5. Re-enable Play Protect in the Play Store settings.

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/mrmello06/vault-spend-android.git
cd vault-spend-android

# Run JVM Unit Tests
./gradlew testDebugUnitTest

# Assemble Release APK (outputs to app/build/outputs/apk/release/Raizen.apk)
./gradlew assembleRelease

# Install directly to your connected device
adb install -r -g app/build/outputs/apk/release/Raizen.apk
```

---

## License

This project is licensed under the Apache 2.0 License. Free, open source, and privacy-respecting forever.

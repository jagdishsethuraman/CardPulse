# CardPulse 💳

> *The pulse of your card spending* — A standalone, 100% local, privacy-first Android app to parse PDF credit card statements and monitor spending across cards in one unified dashboard.

CardPulse is built for users who make all their expenditures through credit cards and want to monitor where their money goes without compromising their financial privacy. The app requires **no internet permission** (`android.permission.INTERNET` is omitted from the manifest), providing an OS-level guarantee that your data never leaves your device.

---

## Key Features

- **Local PDF Parsing**: Extract transaction data page-by-page from bank statement PDFs using `pdfbox-android`.
- **Encryption Decryption Support**: Prompt for password to decrypt encrypted PDFs locally in-memory.
- **Bank Auto-Detection**: Auto-detects statement formats from major Indian banks: **SBI, HDFC, and Federal Bank**.
- **Interactive Dashboard**: View combined total spending, credit limit usage progress bars, top merchants, and category distributions.
- **Custom Canvas Visualizations**: Modern, animated Donut and Bar charts drawn from scratch without heavy third-party dependencies.
- **Activity & Search**: Full transaction history with search, scrollable category filter chips, and credit/debit tab filtering.
- **Settings & Backups**: Import and export database contents via copy-paste JSON clipboard backups. Zero storage permission required.

---

## Tech Stack

- **UI**: Jetpack Compose, Material 3
- **Navigation**: Jetpack Navigation 3 (runtime + UI)
- **Database**: Room Database (SQLite) with KSP compilation
- **Preferences**: Jetpack DataStore Preferences
- **PDF Extraction**: PDFBox-Android (Apache 2.0 license)
- **Concurrency**: Kotlin Coroutines & Flow (MVVM Architecture)
- **JDK Requirement**: Java 17

---

## Project Structure

```
CardPulse/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/cardpulse/
│   │   │   │   ├── data/
│   │   │   │   │   ├── db/          # Room Database, entities, DAOs, Repo
│   │   │   │   │   ├── parser/      # PDF text extractor & regex engines
│   │   │   │   │   └── prefs/       # DataStore Preferences wrapper
│   │   │   │   ├── theme/           # Premium Slate HSL colors and styles
│   │   │   │   ├── ui/
│   │   │   │   │   ├── analytics/   # Trend screens & monthly bar charts
│   │   │   │   │   ├── cards/       # Card carousel & Add Card sheets
│   │   │   │   │   ├── components/  # Reusable charts, selectors, rows
│   │   │   │   │   ├── dashboard/   # Summary dashboard, gauges, donuts
│   │   │   │   │   ├── import_flow/ # Document pickers, decrypters, previews
│   │   │   │   │   ├── onboarding/  # First-run horizontal pager setup
│   │   │   │   │   ├── settings/    # Profile, backups, data wipes
│   │   │   │   │   └── transactions/# Searchable activity & tabs
│   │   │   │   ├── CardPulseApp.kt  # Application entry, manual DI setup
│   │   │   │   ├── MainActivity.kt  # ComponentActivity launcher
│   │   │   │   └── Navigation.kt    # Navigation3 host & activity ViewModels
│   │   │   └── res/                 # Layout XML strings, launcher icons
│   │   └── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml           # Version Catalog with KSP2 & PDFBox
├── build.gradle.kts                 # Root Gradle Configuration
└── settings.gradle.kts
```

---

## How to Build & Run

### Prerequisites
1. Installed **Android SDK** and platform tools.
2. Installed **Java 17** (e.g. Homebrew openjdk@17 on macOS).

### Build Instructions
Open your terminal in the `CardPulse` directory and run:

```bash
# Point to Java 17 home directory (Mac Apple Silicon example)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# Build the debug APK
./gradlew assembleDebug
```

The compiled APK will be available under `app/build/outputs/apk/debug/app-debug.apk`. You can install it on your device using:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Security & Privacy
CardPulse implements a Zero-Trust privacy model. For detailed information on security principles, local file handling, and sandboxing, read [SECURITY.md](SECURITY.md).

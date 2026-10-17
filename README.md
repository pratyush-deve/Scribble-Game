# Scribble — Multiplayer Drawing & Guessing Game

**Scribble** is a real-time multiplayer drawing and word-guessing Android application built with **Kotlin**, **Jetpack Compose**, and **Firebase**. Players can create or join private game rooms using 6-character alphanumeric room codes, take turns drawing words on a live synchronized canvas, and compete to guess words as quickly as possible.

---

## Features

### Implemented Features
- **Authentication & Guest Access**
  - **Google Sign-In:** One-tap authentication via AndroidX Credential Manager and Google Identity Services (`GetGoogleIdOption`) backed by Firebase Authentication.
  - **Anonymous Guest Access:** Instant guest sign-in with custom display names. Guests can play up to **2 completed multiplayer matches** before being prompted to link a Google account.
  - **Seamless Account Linking:** Guests can upgrade to a permanent account by linking their Google credentials while preserving their existing `uid`, profile customization, and progress, with automatic collision handling if the Google account already exists.
- **Multiplayer Rooms & Lobby**
  - **Room Creation & 6-Character Codes:** Hosts generate unique 6-character room codes (`RoomCodeGenerator`) claimed atomically via Firebase Realtime Database transactions.
  - **Configurable Room Settings:** Hosts can customize maximum players (up to 8), total rounds (default 3), drawing time per turn (default 60s), and word difficulty (`EASY`, `MEDIUM`, `HARD`).
  - **Real-Time Presence Tracking:** Live player connection status using Firebase `.info/connected` and `onDisconnect` handlers, plus automatic host migration or room closure when players leave.
- **Live Drawing & Synchronized Canvas**
  - **Interactive Drawing Tools:** Freehand pen tool, segment/stroke eraser, undo last action, and full canvas clear.
  - **Customizable Brush:** Multiple color palette options and adjustable stroke widths.
  - **Normalized Coordinate Streaming:** Strokes are normalized across screen aspect ratios (`StrokeGeometry`, `DrawingStroke`) and streamed in real time to all room participants.
- **Turn-Based Gameplay & Security**
  - **Word Selection & Secret Isolation:** At the start of each turn, the active drawer chooses from 3 word candidates stored in an isolated `/gameSecrets/{roomId}/{drawerId}` node protected by Firebase Security Rules.
  - **Zero-Trust Word Masking:** Public game state stores only the masked word hint (`MaskedWord`) and a SHA-256 hash (`wordHash`) of the target word so client applications never receive the unmasked answer unless they are the active drawer.
  - **Live Chat & Guess Evaluation:** Integrated chat feed displaying player messages, system events, and correct-guess notifications. Close guesses (Levenshtein edit distance of 1 for words $\ge 4$ characters) are detected locally.
  - **Order-Based Scoring Engine (`GameScoringEngine`):**
    - 1st correct guess: **100 pts**
    - 2nd correct guess: **80 pts**
    - 3rd correct guess: **60 pts**
    - 4th+ correct guesses: **40 pts**
    - Drawer bonus: **50 pts** per player who guesses correctly.
- **Match Results, Replay Voting & Lifetime Statistics**
  - **Winner Podium & Standings:** End-of-match podium and full scoreboard.
  - **Authoritative Replay Voting:** Cloud Functions (`submitPlayAgainVote`, `finalizeReplaySession`, `checkReplayTimeouts`) manage post-match "Play Again" voting with a strict majority threshold ($\lfloor N/2 \rfloor + 1$) and automatic room/code cleanup on timeout.
  - **Authoritative Lifetime Stats:** Cloud Function `finalizeMatchStats` records completed matches to Cloud Firestore (`/matches/{matchId}`) and atomically updates player lifetime stats (`gamesPlayed`, `wins`, `losses`, `draws`, `totalScore`, `bestScore`, `winStreak`, and `winRate`).
- **Profile & Customization**
  - **Avatar Picker:** 12 selectable emoji avatars (`CAT`, `DOG`, `FOX`, `PANDA`, `KOALA`, `LION`, `TIGER`, `MONKEY`, `BEAR`, `RABBIT`, `PENGUIN`, `OWL`).
  - **Display Name Editing:** Real-time profile updates synced to Cloud Firestore and active game rooms.

### Planned / Scaffolding Modules
- **Local Match History Cache (`:core:database`):** Currently provides `GameHistoryRepository` and `MockGameHistoryRepository` interfaces; persistent match history is currently queried directly from Cloud Firestore (`FirestoreProfileRepository.getMatchHistory`).
- **Local Preferences DataStore (`:core:datastore`):** Includes `UserPreferencesRepository` and `MockUserPreferencesRepository` alongside JetPack DataStore dependencies for local caching.

---

## Technology Stack

- **Language:** Kotlin `2.2.10` (Java 11 target)
- **UI Framework:** Jetpack Compose (Compose BOM `2026.02.01`, Material 3, Material Icons Extended)
- **Architecture:** Multi-module Clean Architecture with MVVM (`ViewModel`, `StateFlow`, Unidirectional Data Flow)
- **Navigation:** Jetpack Navigation Compose (`2.8.8`) with type-safe Kotlinx Serialization routes
- **Asynchronous Programming:** Kotlin Coroutines & Flow (`1.10.1`)
- **Authentication:** Firebase Authentication + AndroidX Credentials (`1.3.0`) & Google Identity Services (`googleid 1.1.1`)
- **Backend & Real-Time Sync:**
  - **Firebase Realtime Database:** Low-latency room state, player presence, live drawing strokes, chat, and drawer secrets (`firebase-bom 33.10.0`)
  - **Cloud Firestore:** User profiles, guest match counters, lifetime statistics, and authoritative match history records
  - **Firebase Cloud Functions (v2 / Node.js 20 / TypeScript):** Server-authoritative match stats finalization, replay voting transactions, guest eligibility checks, and scheduled cleanup jobs (`checkReplayTimeouts`, `cleanupAbandonedGuestAccounts`)
- **Build System:** Android Gradle Plugin (AGP `9.4.1`), Gradle Wrapper (`9.4.1`), Version Catalogs (`gradle/libs.versions.toml`), compileSdk `37` / minSdk `26`

---

## Repository Structure

```text
Scribble/
├── app/                        # Main Android application module, MainActivity, SplashScreen, and ScribbleNavHost
├── core/
│   ├── common/                 # Shared utilities and AppResult wrapper (Success, Error, Loading)
│   ├── model/                  # Pure Kotlin domain models, GameScoringEngine, MaskedWord, StrokeGeometry, and WordBank
│   ├── designsystem/           # Material 3 theme (Color, Type, Shapes, Spacing) and reusable Scribble UI components
│   ├── ui/                     # Shared feature UI components (AvatarGridPicker, ScribbleScreenContainer)
│   ├── navigation/             # Type-safe navigation route definitions (ScreenDestination)
│   ├── network/                # Firebase Realtime Database, Firestore, and Cloud Functions repositories & DTOs
│   ├── database/               # GameHistoryRepository interface and mock implementation
│   └── datastore/              # UserPreferencesRepository interface and mock implementation
├── feature/
│   ├── auth/                   # Google Sign-In (Credential Manager), Guest login, account linking, and AuthScreen
│   ├── home/                   # Home dashboard, Host/Join room dialogs, Guest limit dialog, and How-To-Play guide
│   ├── lobby/                  # Pre-game waiting room, room code sharing, host settings, and player roster
│   ├── game/                   # Live drawing canvas, toolbars, word selection modal, countdowns, and chat/guessing
│   ├── results/                # Post-game winner podium, scoreboard, and real-time Play Again majority voting
│   └── profile/                # User profile customization (display name, 12 avatars) and lifetime statistics card
├── functions/                  # Firebase Cloud Functions (TypeScript) for match stats, replay voting, and scheduled cleanup
├── rules-tests/                # Node.js test suites for Firebase Realtime Database security rules and gameplay flows
├── docs/                       # Public Privacy Policy hosted via GitHub Pages (docs/index.html)
├── database.rules.json         # Firebase Realtime Database security and validation rules
├── firestore.rules             # Cloud Firestore security and ownership validation rules
├── firebase.json               # Firebase CLI configuration (RTDB, Firestore, Functions, and local Emulators)
└── gradle/libs.versions.toml   # Centralized Gradle dependency version catalog
```

---

## Prerequisites & Setup

### Prerequisites
- **Android Studio:** Ladybug / Meerkat or newer with Android SDK Platform 37 installed.
- **JDK:** Java 11 or higher (Gradle toolchain resolver is configured in `settings.gradle.kts`).
- **Node.js & Firebase CLI** *(Optional, for Cloud Functions & Security Rules tests)*:
  - Node.js `20+`
  - Firebase CLI (`npm install -g firebase-tools`)

### Cloning & Building the Android Project
1. **Clone the repository:**
   ```bash
   git clone https://github.com/pratyush-deve/Scribble-Game.git
   cd Scribble-Game
   ```
2. **Open in Android Studio:**
   - Select **File > Open** and choose the root `Scribble-Game` directory.
   - Android Studio will automatically generate `local.properties` pointing to your local Android SDK and sync the Gradle wrapper.
3. **Firebase Client Configuration (`app/google-services.json`):**
   - For security and to avoid exposing API keys in public version control, `app/google-services.json` is excluded via `.gitignore` (a safe structure template is provided at `app/google-services.json.example`).
   - To build and run the app locally:
     1. Open your project in the [Firebase Console](https://console.firebase.google.com/).
     2. Register an Android app with package name `com.pratyush.scribble` (or select the existing Android app in Project Settings).
     3. Add your debug and release SHA-1 / SHA-256 signing certificate fingerprints (required for Google Sign-In via Credential Manager).
     4. Enable **Authentication** (Google & Anonymous providers), **Realtime Database**, **Cloud Firestore**, and **Cloud Functions**.
     5. Download `google-services.json` from Firebase Project Settings and place it at `app/google-services.json` (do not commit this file).
     6. Update the Realtime Database URL in `core/network/src/main/java/com/pratyush/scribble/core/network/di/NetworkModule.kt` if using a custom RTDB instance.

---

## Firebase Security Rules & Cloud Functions

### Deploying Security Rules
The repository includes strict security rules in `database.rules.json` and `firestore.rules` that enforce:
- Authenticated access for all rooms, room codes, profiles, and match records.
- Drawer-only write access to strokes during the `DRAWING` phase and isolated read/write access to `/gameSecrets/{roomId}/{playerId}`.
- Server-only writes to `/matches/{matchId}` and user lifetime statistics in Cloud Firestore (preventing client score tampering).

To deploy the security rules to your Firebase project:
```bash
firebase deploy --only database,firestore:rules
```

### Building & Deploying Cloud Functions
Cloud Functions are located in `functions/src/index.ts` (`us-central1`):
```bash
cd functions
npm install
npm run build
firebase deploy --only functions
```

---

## Running Tests & Builds

### Android Unit Tests & Debug Build
Run unit tests across all modules (`:app`, `:core:model`, `:core:network`, `:feature:auth`, `:feature:home`, `:feature:lobby`, `:feature:game`, `:feature:results`, `:feature:profile`):
```bash
# Windows (PowerShell / CMD)
.\gradlew.bat testDebugUnitTest

# macOS / Linux
./gradlew testDebugUnitTest
```

Assemble the debug APK:
```bash
.\gradlew.bat assembleDebug
```

### Firebase Security Rules Tests
With the Firebase Emulator Suite installed:
```bash
npm test
```

---

## Privacy Policy

The official Google Play-compliant Privacy Policy for **Scribble** is hosted via GitHub Pages:
- **Public Privacy Policy URL:** [https://pratyush-deve.github.io/Scribble-Game/](https://pratyush-deve.github.io/Scribble-Game/)
- **Source File:** [`docs/index.html`](docs/index.html)

---

## Security & Credentials Notice

> **IMPORTANT:** Never commit private signing keystores (`*.jks`, `*.keystore`), signing passwords (`keystore.properties`), `local.properties`, or Firebase Admin SDK service-account private keys (`*firebase-adminsdk*.json`, `serviceAccountKey.json`) to version control. The root `.gitignore` is configured to block these files automatically.

---

## Contributing & Reporting Issues

1. Fork the repository and create a feature branch (`git checkout -b feature/your-feature`).
2. Ensure all existing unit tests pass (`./gradlew testDebugUnitTest`).
3. Open a Pull Request with a clear description of your changes, or report bugs via [GitHub Issues](https://github.com/pratyush-deve/Scribble-Game/issues).

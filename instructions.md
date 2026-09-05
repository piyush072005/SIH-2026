# iTantra Developer Instructions & Orchestration Guide
**Offline Multilingual Walkie-Talkie (SIH 2026, Problem Statement 26173)**

---

## 1. Team Scopes & Responsibilities

This repository is cooperatively developed by two independent AI agents / IDEs coordinated by the Antigravity Orchestrator:

| Scope | Tool / Agent | Target File Paths | Primary Responsibilities |
|---|---|---|---|
| **UI Scope** | **VS Code + Copilot** | `app/src/main/java/com/isro/itantra/ui/**`<br>`app/src/main/res/**` | Compose screens, ViewModels, Themes, tactile controls, audio waveform visualizers, pairing sheets. |
| **Backend Scope** | **Cursor** | `app/src/main/java/com/isro/itantra/audio/`<br>`app/src/main/java/com/isro/itantra/transport/`<br>`app/src/main/java/com/isro/itantra/mode/`<br>`app/src/main/java/com/isro/itantra/data/`<br>`app/src/main/java/com/isro/itantra/service/`<br>`app/src/main/java/com/isro/itantra/di/` | STT/TTS offline inference engines, Nearby Connections P2P transport, ModeController state machine, Foreground Service, DI. |
| **Orchestrator** | **Antigravity** | `app/src/main/java/com/isro/itantra/domain/contracts/**`<br>`docs/**`<br>`instructions.md` | Shared contracts definition, documentation consistency, architecture governance, non-destructive integration verification. |

---

## 2. The Shared Contract Rule (CRITICAL)

All inter-scope communication is mediated **strictly** through pure Kotlin interfaces in:
```
app/src/main/java/com/isro/itantra/domain/contracts/
├── AudioEngineManager.kt
├── TransportManager.kt
├── ModeController.kt
└── models/
    ├── OperationalMode.kt
    ├── ConnectionState.kt
    ├── DiscoveredPeer.kt
    ├── IncomingMessage.kt
    └── AudioEngineState.kt
```

### Golden Rules:
1. **Zero Cross-Scope Leaks**: Code in `ui/` **MUST NEVER** import or reference concrete classes from `audio/`, `transport/`, `mode/`, `service/`, or `data/`. It must only inject/consume the interfaces defined in `domain/contracts/`.
2. **Contract Immutability**: No scope may edit files in `domain/contracts/`. If an interface needs expansion or amendment, submit an issue/proposal to the Antigravity Orchestrator for formal review.
3. **No Direct Feature Edits by Orchestrator**: The Orchestrator will never write implementation code inside `ui/` or `audio/`, `transport/`, `mode/`, `data/`, `service/`.

---

## 3. Directory Map & Task Breakdown

```
app/src/main/
├── AndroidManifest.xml                  [Orchestrator baseline]
├── java/com/isro/itantra/
│   ├── domain/contracts/                [ORCHESTRATOR SCOPE]
│   │   ├── AudioEngineManager.kt
│   │   ├── TransportManager.kt
│   │   ├── ModeController.kt
│   │   └── models/
│   ├── ui/                              [UI SCOPE: VS Code + Copilot]
│   │   ├── theme/                       (Color, Type, Theme tokens)
│   │   ├── navigation/                  (Screen routes & backstack)
│   │   ├── screens/
│   │   │   ├── ptt/                     (Push-To-Talk tactile interface)
│   │   │   ├── duplex/                  (Full-duplex hands-free view)
│   │   │   ├── alert/                   (SOS emergency broadcast screen)
│   │   │   └── peers/                   (Peer discovery & pairing dialog)
│   │   ├── components/                  (Common UI widgets, buttons, status bars)
│   │   └── viewmodels/                  (ViewModels consuming contracts)
│   ├── audio/                           [BACKEND SCOPE: Cursor]
│   │   ├── engine/                      (Sherpa-onnx / Vosk / TFLite STT)
│   │   ├── tts/                         (Piper / Android TTS wrapper)
│   │   ├── recorder/                    (AudioRecord mic buffer management)
│   │   └── vad/                         (Voice Activity Detection)
│   ├── transport/                       [BACKEND SCOPE: Cursor]
│   │   ├── nearby/                      (Google Nearby Connections P2P_STAR / P2P_CLUSTER)
│   │   ├── payload/                     (Protocol buffer / JSON packet serialization)
│   │   └── connection/                  (Socket / connection lifecycle management)
│   ├── mode/                            [BACKEND SCOPE: Cursor]
│   │   └── ModeControllerImpl.kt        (State machine & alert override handler)
│   ├── data/                            [BACKEND SCOPE: Cursor]
│   │   ├── preferences/                 (DataStore for callsign, language, channels)
│   │   └── assets/                      (Offline model asset unpacker)
│   ├── service/                         [BACKEND SCOPE: Cursor]
│   │   └── WalkieTalkieService.kt       (Foreground service for continuous audio/P2P)
│   └── di/                              [BACKEND SCOPE: Cursor]
│       └── AppModule.kt                 (Hilt / Koin dependency injection bindings)
└── res/                                 [UI SCOPE: VS Code + Copilot]
    ├── drawable/
    ├── values/
    └── mipmap/
```

---

## 4. Work Checklist for Developers

### UI Scope Checklist (Hand off to VS Code + Copilot)
- [ ] **UI-1: Theme & Design System**: Configure tactical dark walkie-talkie UI with high-contrast military/rugged accents (`ui/theme/`).
- [ ] **UI-2: PTT Screen**: Implement push-and-hold tactile button with haptic feedback, audio recording indicator, and live transcribed text feed.
- [ ] **UI-3: Duplex Screen**: Implement hands-free two-way voice screen with visual VAD activity and channel members.
- [ ] **UI-4: Alert / SOS Screen**: High-visibility emergency override view with siren toggle and broadcast trigger.
- [ ] **UI-5: Peer Discovery Sheet**: Sheet/Dialog allowing operators to scan for nearby devices, view RSSI signal strengths, and pair.
- [ ] **UI-6: ViewModels**: Inject `AudioEngineManager`, `TransportManager`, and `ModeController` to expose clean Compose state.

### Backend Scope Checklist (Hand off to Cursor)
- [ ] **BE-1: Audio Engine**: Implement `AudioEngineManager` using offline STT (Sherpa-onnx / Vosk) and TTS (Piper / local TTS), managing 16kHz PCM audio buffers.
- [ ] **BE-2: P2P Transport**: Implement `TransportManager` using Google Nearby Connections API (`Strategy.P2P_CLUSTER` or `P2P_STAR`).
- [ ] **BE-3: Mode Controller**: Implement `ModeController` enforcing state transitions, alert overrides, and fallback restoration.
- [ ] **BE-4: Foreground Service**: Implement `WalkieTalkieService` to keep audio recording and P2P connection alive when app is backgrounded.
- [ ] **BE-5: DataStore & Assets**: Unpack offline model weights from APK assets / local storage and manage user preferences.
- [ ] **BE-6: DI Configuration**: Provide singleton bindings for `AudioEngineManager`, `TransportManager`, and `ModeController`.

---

## 5. Branching Strategy & Integration Flow

1. **UI Features**: Work on branches named `feature/ui-<feature-name>`.
2. **Backend Features**: Work on branches named `feature/backend-<feature-name>`.
3. **Integration Verification**:
   - The Antigravity Orchestrator will review PRs across branches to ensure zero direct dependencies between `ui/` and `backend/` implementations.
   - Run verification checks before proposing merges into `main`.

---

## 6. Hardware & Physical Device Notes (Human Operator Testing Required)
Certain critical Android capabilities **cannot** be verified in automated JVM tests or headless emulators:
- **Foreground Service with Microphone Type**: On Android 14+ (API 34), foreground services with `microphone` type require special runtime permissions and active notification icons.
- **Wi-Fi Direct / Nearby Connections**: Local peer discovery requires location and nearby device runtime permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES`). Radio behavior must be tested across at least two physical hardware devices.
- **Native Inference Latency**: ONNX / TFLite NDK binaries must be tested on physical ARM64 / ARMv7 processors to measure thermal impact and latency.

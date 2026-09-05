# iTantra Architecture Specification
**Offline Multilingual Walkie-Talkie (SIH 2026, PS 26173)**

---

## 1. System Overview

iTantra is designed for zero-infrastructure, disaster-response, and mission-critical offline communication. It enables voice-driven, multilingual walkie-talkie capability across nearby mobile devices without cellular connectivity or internet access.

The system pipeline bridges **Speech-to-Text (STT) -> Low-Bandwidth Text Transport -> Text-to-Speech (TTS)**:

```
[Local Operator]
       │
       ▼ (Acoustic Audio 16kHz PCM)
┌─────────────────────────────────────────────────┐
│               Local Audio Pipeline              │
│  - AudioRecord Capture                          │
│  - Voice Activity Detection (Silero VAD)        │
│  - Offline STT (Sherpa-onnx / Vosk / Whisper)   │
└──────────────────────┬──────────────────────────┘
                       │ Transcribed Text (Payload < 100 bytes)
                       ▼
┌─────────────────────────────────────────────────┐
│               Transport Layer                   │
│  - Google Nearby Connections / Wi-Fi Direct     │
│  - Offline P2P Mesh Topology                    │
│  - Priority QoS (Alerts > Duplex > PTT)         │
└──────────────────────┬──────────────────────────┘
                       │ (Over-the-Air P2P Broadcast)
                       ▼
┌─────────────────────────────────────────────────┐
│           Remote Peer Audio Pipeline            │
│  - Offline TTS (Piper / Local Android TTS)      │
│  - AudioTrack Playback (Language Localization)  │
└──────────────────────┬──────────────────────────┘
                       ▼ (Acoustic Audio Playback)
[Remote Operator]
```

### Why Transcribe Before Transmission?
Transmitting raw 16kHz PCM audio requires ~256 kbps bandwidth and is prone to packet loss over ad-hoc Wi-Fi/Bluetooth mesh links. Transcribing locally and sending **text payloads** reduces data size to **<100 bytes per sentence** (>99% bandwidth reduction), enabling:
- Multi-hop mesh relaying without congestion.
- Cross-lingual translation / localization on the receiving end.
- Instant searchability and tactical logging.

---

## 2. Layered Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           UI Presentation Layer                         │
│   (Jetpack Compose Screens, ViewModels, Theme Tokens, Audio Visualizer) │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ Consumes Flow / Calls methods
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          Domain Contract Layer                          │
│     app/src/main/java/com/isro/itantra/domain/contracts/                │
│     - AudioEngineManager (STT / TTS)                                    │
│     - TransportManager (P2P Mesh / Nearby)                              │
│     - ModeController (PushToTalk / Duplex / Alert)                      │
└──────────────────▲─────────────────▲──────────────────▲─────────────────┘
                   │                 │                  │
         Implements│       Implements│        Implements│
┌──────────────────┴────────┐ ┌──────┴─────────┐ ┌──────┴─────────────────┐
│       Audio Engine        │ │    Transport   │ │     Mode Controller    │
│  (Sherpa-onnx, Vosk, VAD) │ │(Nearby Conns)  │ │ (State Machine, Alert) │
└──────────────────┬────────┘ └──────┬─────────┘ └──────┬─────────────────┘
                   │                 │                  │
                   └─────────────────┼──────────────────┘
                                     │ Orchestrated by
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                   WalkieTalkieForegroundService                         │
│  - Holds WakeLocks, WifiLocks, and AudioRecord buffer                   │
│  - Keeps P2P mesh alive while device screen is off                      │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Component Details

### 3.1 Domain Contracts (`domain/contracts/`)
The single source of truth connecting UI and Backend.
- **`AudioEngineManager`**:
  - Exposes `transcribedText: Flow<String>` emitting real-time STT segments.
  - Exposes `isListening: Flow<Boolean>` and `audioState: Flow<AudioEngineState>`.
  - Provides `startListening()`, `stopListening()`, `speak(text, languageCode)`.
- **`TransportManager`**:
  - Exposes `connectionState: Flow<ConnectionState>` and `discoveredPeers: Flow<List<DiscoveredPeer>>`.
  - Exposes `receivedText: Flow<String>` and `incomingMessages: Flow<IncomingMessage>`.
  - Provides discovery, advertising, connection, and transmission functions.
- **`ModeController`**:
  - Manages transitions between `PushToTalk`, `Duplex`, and `Alert`.
  - Guarantees immediate priority override for emergency SOS alerts with auto-restoration upon clearance.

### 3.2 Offline Audio Engine (`audio/`)
- Audio captured at 16,000 Hz, 16-bit Mono PCM.
- Voice Activity Detection (VAD) chops audio into natural speech chunks (minimum 300ms pause).
- Offline STT (Sherpa-onnx / Vosk quantized INT8 acoustic models) computes text on-device.
- Output text is synthesized using Piper TTS or Android native offline TTS engines.

### 3.3 Peer-to-Peer Transport (`transport/`)
- Powered by Google Nearby Connections API using `Strategy.P2P_CLUSTER` (multi-peer ad-hoc mesh) or `Strategy.P2P_STAR`.
- Supports fallback to Wi-Fi Direct or Bluetooth Low Energy (BLE) advertisements.
- Packets are serialized as compact binary/JSON payloads with header metadata (sender ID, callsign, message ID, timestamp, priority flag).

### 3.4 Foreground Service (`service/`)
- `WalkieTalkieForegroundService`:
  - Notification channel with persistent status: active mode, connected peers count.
  - Foreground service types: `microphone` and `connectedDevice`.
  - Android PowerManager `PARTIAL_WAKE_LOCK` and WifiManager `WIFI_MODE_FULL_HIGH_PERF` to maintain link integrity when device enters Doze mode.

---

## 4. State Flow & Concurrency

- All asynchronous state streams use Kotlin Coroutines `StateFlow` and `SharedFlow`.
- Threading model:
  - UI state collected on `Dispatchers.Main.immediate`.
  - Audio recording and STT inference dispatched to dedicated high-priority threads (`Dispatchers.Default` / custom single-thread executor).
  - Network I/O and Nearby socket operations on `Dispatchers.IO`.

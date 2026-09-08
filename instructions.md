# iTantra (SIH26173) — technical instructions

Offline Android transceiver radio: on-device STT/TTS for 10 languages, P2P text over Wi-Fi Direct (Bluetooth fallback), walkie-talkie PTT or duplex phone-like audio.

## ML runtime

- Use **sherpa-onnx** (ONNX Runtime Mobile) for streaming/offline STT and TTS. No other inference framework.
- **STT:** AI4Bharat IndicConformer, INT8, per language at `app/src/main/assets/models/stt/<lang-code>/`
- **TTS:** Piper/VITS at `app/src/main/assets/models/tts/<lang-code>/`. MMS-TTS fallback is CC-BY-NC — document in `docs/model-card.md` if used.
- **VAD:** Silero VAD; STT must not run while VAD reports silence.

Languages: `hi`, `gu`, `mr`, `kn`, `ml`, `ta`, `te`, `or`, `bn`, `en`.

## Transport

- Primary: `WifiP2pManager`. Fallback: `BluetoothSocket`.
- ACK/retry on every reliable payload. Design for link drop mid-message and reconnect (replay unacked seqs), not only the happy path.

## Modes

- `PushToTalk`, `Duplex`, `Alert`
- Alert interrupts current playback and forces max volume regardless of mode.

## Service / threading

- Foreground service: `foregroundServiceType="microphone|connectedDevice"` (Android 14+).
- No blocking work on the main thread. Inference and I/O on `Dispatchers.Default` / `Dispatchers.IO`.

## Scope split

Backend implements contracts in `domain/contracts`. UI lives under `ui/` and `res/` and is out of backend scope.

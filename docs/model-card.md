# iTantra model card

## Runtime

All on-device inference uses **sherpa-onnx** (ONNX Runtime). No cloud STT/TTS.

## STT

| Item | Value |
|------|--------|
| Family | AI4Bharat IndicConformer (hybrid CTC), INT8 ONNX |
| Layout | `assets/models/stt/<lang-code>/` (`model.int8.onnx`, `tokens.txt`) |
| Languages | hi, gu, mr, kn, ml, ta, te, or, bn, en |
| License (upstream) | IndicConformer checkpoints are typically MIT — verify per-file LICENSE before redistribution |
| Gating | Silero VAD; recognizer runs only on detected speech segments |

Placeholders only are committed. Binary weights must be copied onto the device or into assets before on-device tests.

## TTS

| Item | Value |
|------|--------|
| Primary | Piper / VITS (`assets/models/tts/<lang-code>/`) |
| MMS-TTS fallback | **Not bundled.** Meta MMS-TTS is **CC-BY-NC**. If a language has no Piper voice and MMS is added later, it cannot be used in commercial/proprietary redistribution without a separate license. |

Current code path loads Piper/VITS-style `model.onnx` + `tokens.txt` (+ `espeak-ng-data` when present). No MMS checkpoint is referenced.

## VAD

Silero VAD ONNX via sherpa-onnx, shared at `assets/models/vad/silero_vad.onnx`.

## Manual verification (not covered by unit tests)

- Word error rate and RTF on a low/mid-range phone
- Idle CPU with VAD open and no speech
- Alert barge-in at max volume
- Wi-Fi Direct + Bluetooth reconnect after mid-message drop

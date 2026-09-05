# iTantra Offline Model Card & Specifications
**Multilingual Offline Speech Models (SIH 2026, PS 26173)**

---

## 1. Overview & Constraints

iTantra operates in isolated disaster environments without external cloud connectivity. All neural models for Speech-to-Text (STT) and Text-to-Speech (TTS) must execute entirely on-device with strictly budgeted RAM, APK size, and battery consumption.

### Target Performance Budgets:
- **Max Model Size on Disk**: < 100 MB per language (quantized INT8).
- **Peak RAM Footprint**: < 150 MB during active STT/TTS execution.
- **Inference Real-Time Factor (RTF)**: < 0.5 (processing 1 second of audio takes < 0.5s on mid-range ARM64 SoCs).
- **Target OS**: Android 8.0 (API 26) through Android 15 (API 35).

---

## 2. Speech-to-Text (STT) Specifications

### Primary Engine: Sherpa-ONNX (Streaming Zipformer / Conformer)
- **Framework**: `k2-fsa/sherpa-onnx`
- **Architecture**: Streaming transducer (Zipformer) quantized to INT8 with ONNX Runtime Mobile.
- **Sample Rate**: 16,000 Hz, 16-bit PCM, single-channel.
- **Feature Extraction**: 80-dimensional log Mel filterbank.
- **Latency**: First partial output within 250ms of speech onset.

### Fallback Engine: Vosk-Android / Whisper Tiny TFLite
- **Vosk**: Kaldi-based small acoustic models (~40MB per language), very low CPU usage on older devices.
- **Whisper Tiny.en / Tiny Multilingual**: Quantized TFLite/ONNX, used for high-accuracy translation tasks where streaming latency is less critical.

### Language Support Matrix (Indian Languages):
| Language | Code | Primary Model Variant | Quantization | Accuracy (WER on Clear Speech) |
|---|---|---|---|---|
| Hindi | `hi` | Sherpa-ONNX Zipformer Hindi | INT8 | < 12% |
| English (Indian Accent) | `en-IN` | Sherpa-ONNX Conformer Small | INT8 | < 8% |
| Tamil | `ta` | Vosk Small Tamil / Sherpa | INT8 | < 15% |
| Telugu | `te` | Vosk Small Telugu / Sherpa | INT8 | < 15% |
| Bengali | `bn` | Sherpa / Vosk Small Bengali | INT8 | < 16% |

---

## 3. Text-to-Speech (TTS) Specifications

### Primary Engine: Piper TTS (ONNX)
- **Framework**: `piper-tts` ported via Sherpa-ONNX.
- **Architecture**: Fast VITS neural vocoder.
- **Size**: ~25-45 MB per voice model.
- **Audio Output**: 22,050 Hz or 16,000 Hz WAV/PCM stream.
- **Performance**: RTF < 0.2 on modern mobile CPUs.

### Fallback Engine: Android System `TextToSpeech`
- Utilizes pre-installed offline voices provided by Android OEM (Google Speech Services / Samsung TTS).
- **Memory Overhead**: 0 MB added to APK.
- **Availability**: System dependent; tested if target language pack is downloaded by the OS.

---

## 4. Voice Activity Detection (VAD)

- **Model**: Silero VAD (v4/v5 ONNX)
- **Size**: ~1.5 MB
- **Operation**: Runs on 30ms audio windows (480 samples @ 16kHz).
- **Purpose**: Eliminates transmission of background static, reduces STT inference power consumption by only passing active speech segments to the acoustic model.

---

## 5. Storage & Asset Packaging

- Model files (`.onnx`, `tokens.txt`, `am.mvn`) reside in `app/src/main/assets/models/` or are dynamically unpacked on first launch to `context.getExternalFilesDir("models")`.
- Memory mapping (`mmap`) is utilized to minimize resident memory consumption.

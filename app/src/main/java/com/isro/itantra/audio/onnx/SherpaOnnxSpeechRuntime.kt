package com.isro.itantra.audio.onnx

import android.content.Context
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * sherpa-onnx wrappers. Native model load must be called off the main thread.
 *
 * IndicConformer INT8 is consumed as NeMo EncDec CTC when `model.int8.onnx` is present.
 * On-device inference must be verified on a low/mid-range phone.
 */
@Singleton
class SherpaOnnxSpeechRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechRuntime {

    @Volatile
    override var isReady: Boolean = false
        private set

    private var vad: Vad? = null
    private var recognizer: OfflineRecognizer? = null
    private var tts: OfflineTts? = null

    @Synchronized
    override fun prepare(paths: ModelPaths) {
        release()
        vad = Vad(config = buildVadConfig(paths.vadPath))
        recognizer = OfflineRecognizer(config = buildAsrConfig(paths))
        tts = OfflineTts(config = buildTtsConfig(paths))
        isReady = true
    }

    override fun resetVad() {
        vad?.reset()
    }

    override fun acceptVad(samples: FloatArray): Boolean {
        val engine = vad ?: return false
        engine.acceptWaveform(samples)
        return engine.isSpeechDetected()
    }

    override fun popUtterance(): FloatArray? {
        val engine = vad ?: return null
        if (engine.empty()) return null
        val segment = engine.front()
        engine.pop()
        return segment.samples
    }

    override fun decodeFinal(samples: FloatArray): String {
        val asr = recognizer ?: return ""
        val stream = asr.createStream()
        stream.acceptWaveform(samples, sampleRate = SAMPLE_RATE)
        asr.decode(stream)
        val text = asr.getResult(stream).text.trim()
        stream.release()
        return text
    }

    override fun decodePartial(samples: FloatArray): String = decodeFinal(samples)

    override fun synthesize(text: String): PcmAudio {
        val engine = tts ?: return PcmAudio(FloatArray(0), SAMPLE_RATE)
        val audio: GeneratedAudio = engine.generate(text)
        return PcmAudio(audio.samples, audio.sampleRate)
    }

    @Synchronized
    override fun release() {
        isReady = false
        vad?.release()
        recognizer?.release()
        tts?.release()
        vad = null
        recognizer = null
        tts = null
    }

    private fun buildVadConfig(modelPath: String): VadModelConfig {
        require(File(modelPath).exists()) { "Missing Silero VAD at $modelPath" }
        return VadModelConfig(
            sileroVadModelConfig = SileroVadModelConfig(
                model = modelPath,
                threshold = 0.5f,
                minSilenceDuration = 0.35f,
                minSpeechDuration = 0.25f,
                windowSize = WINDOW_SIZE,
            ),
            sampleRate = SAMPLE_RATE,
            numThreads = 1,
            provider = "cpu",
        )
    }

    private fun buildAsrConfig(paths: ModelPaths): OfflineRecognizerConfig {
        require(File(paths.sttInt8).exists()) { "Missing IndicConformer INT8 at ${paths.sttInt8}" }
        require(File(paths.sttTokens).exists()) { "Missing STT tokens at ${paths.sttTokens}" }
        val model = OfflineModelConfig(
            nemo = OfflineNemoEncDecCtcModelConfig(model = paths.sttInt8),
            tokens = paths.sttTokens,
            numThreads = 2,
            provider = "cpu",
            modelType = "nemo_ctc",
        )
        return OfflineRecognizerConfig(modelConfig = model)
    }

    private fun buildTtsConfig(paths: ModelPaths): OfflineTtsConfig {
        require(File(paths.ttsModel).exists()) { "Missing Piper/VITS model at ${paths.ttsModel}" }
        require(File(paths.ttsTokens).exists()) { "Missing TTS tokens at ${paths.ttsTokens}" }
        val vits = OfflineTtsVitsModelConfig(
            model = paths.ttsModel,
            tokens = paths.ttsTokens,
            dataDir = paths.ttsDataDir.takeIf { File(it).isDirectory } ?: "",
        )
        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(vits = vits, numThreads = 2, provider = "cpu"),
        )
    }

    companion object {
        const val SAMPLE_RATE: Int = 16_000
        const val WINDOW_SIZE: Int = 512
    }
}

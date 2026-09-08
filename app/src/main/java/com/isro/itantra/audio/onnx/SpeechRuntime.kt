package com.isro.itantra.audio.onnx

interface SpeechRuntime {
    val isReady: Boolean
    fun prepare(paths: ModelPaths)
    fun resetVad()
    /** Returns true if Silero currently reports speech. */
    fun acceptVad(samples: FloatArray): Boolean
    /** Endpointed utterance PCM at 16 kHz. Empty if VAD has no finished segment. */
    fun popUtterance(): FloatArray?
    fun decodeFinal(samples: FloatArray): String
    fun decodePartial(samples: FloatArray): String
    fun synthesize(text: String): PcmAudio
    fun release()
}

data class PcmAudio(
    val samples: FloatArray,
    val sampleRate: Int,
)

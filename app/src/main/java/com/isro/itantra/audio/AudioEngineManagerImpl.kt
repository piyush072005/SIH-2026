package com.isro.itantra.audio

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.isro.itantra.audio.onnx.ModelPaths
import com.isro.itantra.audio.onnx.PcmAudio
import com.isro.itantra.audio.onnx.SherpaOnnxSpeechRuntime
import com.isro.itantra.audio.onnx.SpeechRuntime
import com.isro.itantra.di.IoDispatcher
import com.isro.itantra.domain.contracts.AudioEngineManager
import com.isro.itantra.domain.contracts.models.AudioEngineState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEngineManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtime: SpeechRuntime,
    private val volumeController: VolumeController,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AudioEngineManager {

    private val engineExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "itantra-onnx").apply { priority = Thread.NORM_PRIORITY }
    }
    private val engineDispatcher = engineExecutor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + io)

    private val _transcribedText = MutableSharedFlow<String>(extraBufferCapacity = 16)
    private val _audioState = MutableStateFlow<AudioEngineState>(AudioEngineState.Uninitialized)
    private val _isListening = MutableStateFlow(false)

    override val transcribedText: Flow<String> = _transcribedText.asSharedFlow()
    override val audioState: Flow<AudioEngineState> = _audioState.asStateFlow()
    override val isListening: Flow<Boolean> = _isListening.asStateFlow()

    private val capturing = AtomicBoolean(false)
    private val abortPlayback = AtomicBoolean(false)
    private var captureJob: Job? = null
    @Volatile private var languageCode: String = "hi"
    private var volumeSnapshot: VolumeSnapshot? = null
    @Volatile private var audioTrack: AudioTrack? = null

    override suspend fun initialize(): Result<Unit> {
        _audioState.value = AudioEngineState.Initializing
        return runCatching {
            withContext(engineDispatcher) {
                val root = AssetModelUnpacker.unpackLanguage(context, languageCode)
                runtime.prepare(
                    ModelPaths(
                        languageCode = languageCode,
                        sttDir = root.resolve("stt/$languageCode").absolutePath,
                        ttsDir = root.resolve("tts/$languageCode").absolutePath,
                        vadPath = root.resolve("vad/silero_vad.onnx").absolutePath,
                    ),
                )
            }
            _audioState.value = AudioEngineState.Ready
        }.onFailure { err ->
            _audioState.value = AudioEngineState.Error(err.message ?: "init_failed", err)
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startListening() {
        if (!capturing.compareAndSet(false, true)) return
        _isListening.value = true
        _audioState.value = AudioEngineState.Listening
        captureJob = scope.launch { captureLoop() }
    }

    override fun stopListening() {
        capturing.set(false)
        captureJob?.cancel()
        captureJob = null
        _isListening.value = false
        if (_audioState.value is AudioEngineState.Listening) {
            _audioState.value = AudioEngineState.Ready
        }
    }

    override suspend fun speak(text: String, languageCode: String): Result<Unit> {
        if (text.isBlank()) return Result.success(Unit)
        this.languageCode = languageCode
        abortPlayback.set(false)
        _audioState.value = AudioEngineState.Speaking(text)
        return runCatching {
            val pcm = withContext(engineDispatcher) { runtime.synthesize(text) }
            if (!abortPlayback.get()) {
                withContext(io) { playPcm(pcm, alert = false) }
            }
            if (_audioState.value is AudioEngineState.Speaking) {
                _audioState.value = AudioEngineState.Ready
            }
        }.onFailure { err ->
            _audioState.value = AudioEngineState.Error(err.message ?: "tts_failed", err)
        }
    }

    override fun stopSpeaking() {
        abortPlayback.set(true)
        audioTrack?.let {
            runCatching {
                it.pause()
                it.flush()
                it.stop()
            }
        }
        if (_audioState.value is AudioEngineState.Speaking) {
            _audioState.value = AudioEngineState.Ready
        }
    }

    fun setOutputVolumeMax() {
        if (volumeSnapshot == null) {
            volumeSnapshot = volumeController.snapshot()
        }
        volumeController.forceMaxAlert()
    }

    fun restoreOutputVolume() {
        volumeSnapshot?.let { volumeController.restore(it) }
        volumeSnapshot = null
    }

    suspend fun speakAlert(text: String): Result<Unit> {
        if (text.isBlank()) return Result.success(Unit)
        abortPlayback.set(false)
        _audioState.value = AudioEngineState.Speaking(text)
        return runCatching {
            val pcm = withContext(engineDispatcher) { runtime.synthesize(text) }
            if (!abortPlayback.get()) {
                withContext(io) { playPcm(pcm, alert = true) }
            }
            if (_audioState.value is AudioEngineState.Speaking) {
                _audioState.value = AudioEngineState.Ready
            }
        }
    }

    override fun release() {
        stopListening()
        stopSpeaking()
        runtime.release()
        engineDispatcher.close()
        engineExecutor.shutdown()
        _audioState.value = AudioEngineState.Uninitialized
    }

    private suspend fun captureLoop() {
        val minBuf = AudioRecord.getMinBufferSize(
            SherpaOnnxSpeechRuntime.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SherpaOnnxSpeechRuntime.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 2,
        )
        val shorts = ShortArray(SherpaOnnxSpeechRuntime.WINDOW_SIZE)
        val floats = FloatArray(SherpaOnnxSpeechRuntime.WINDOW_SIZE)
        recorder.startRecording()
        try {
            withContext(engineDispatcher) { runtime.resetVad() }
            while (capturing.get()) {
                val read = recorder.read(shorts, 0, shorts.size)
                if (read <= 0) continue
                for (i in 0 until read) {
                    floats[i] = shorts[i] / 32768.0f
                }
                val window = if (read == floats.size) floats else floats.copyOf(read)
                withContext(engineDispatcher) { runtime.acceptVad(window) }
                val utterance = withContext(engineDispatcher) { runtime.popUtterance() }
                if (utterance != null && utterance.isNotEmpty()) {
                    val text = withContext(engineDispatcher) { runtime.decodeFinal(utterance) }
                    if (text.isNotBlank()) {
                        _transcribedText.emit(text)
                    }
                }
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }

    private fun playPcm(pcm: PcmAudio, alert: Boolean) {
        if (pcm.samples.isEmpty()) return
        val channel = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioTrack.getMinBufferSize(pcm.sampleRate, channel, encoding)
        val usage = if (alert) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_VOICE_COMMUNICATION
        val content = if (alert) AudioAttributes.CONTENT_TYPE_SONIFICATION else AudioAttributes.CONTENT_TYPE_SPEECH
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(content)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(pcm.sampleRate)
                    .setEncoding(encoding)
                    .setChannelMask(channel)
                    .build(),
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack = track
        val shorts = ShortArray(pcm.samples.size) { i ->
            (pcm.samples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        }
        track.play()
        var offset = 0
        val chunk = 1024
        while (offset < shorts.size && !abortPlayback.get()) {
            val n = minOf(chunk, shorts.size - offset)
            track.write(shorts, offset, n)
            offset += n
        }
        if (!abortPlayback.get()) {
            track.stop()
        }
        track.release()
        audioTrack = null
    }
}

package com.isro.itantra.mode

import com.isro.itantra.audio.AudioEngineManagerImpl
import com.isro.itantra.audio.VolumeController
import com.isro.itantra.audio.VolumeSnapshot
import com.isro.itantra.di.ApplicationScope
import com.isro.itantra.domain.contracts.AudioEngineManager
import com.isro.itantra.domain.contracts.ModeController
import com.isro.itantra.domain.contracts.TransportManager
import com.isro.itantra.domain.contracts.models.OperationalMode
import com.isro.itantra.mode.machine.ModeEvent
import com.isro.itantra.mode.machine.ModeMachineState
import com.isro.itantra.mode.machine.ModeReducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModeControllerImpl @Inject constructor(
    private val audio: AudioEngineManager,
    private val transport: TransportManager,
    private val volume: VolumeController,
    @ApplicationScope private val scope: CoroutineScope,
) : ModeController {

    private val mutex = Mutex()
    private var machine = ModeMachineState()
    private var volumeSnapshot: VolumeSnapshot? = null
    private var volumeForced = false

    private val _currentMode = MutableStateFlow(OperationalMode.PushToTalk)
    private val _preAlertMode = MutableStateFlow<OperationalMode?>(null)

    override val currentMode: Flow<OperationalMode> = _currentMode.asStateFlow()
    override val preAlertMode: Flow<OperationalMode?> = _preAlertMode.asStateFlow()

    init {
        scope.launch {
            audio.transcribedText.collectLatest { text ->
                if (machine.mode == OperationalMode.Alert) return@collectLatest
                if (text.isBlank()) return@collectLatest
                runCatching { transport.sendText(text) }
            }
        }
        scope.launch {
            transport.incomingMessages.collectLatest { inbound ->
                if (inbound.isAlert) {
                    applyRemoteAlert(inbound.text)
                } else if (machine.mode != OperationalMode.Alert) {
                    audio.speak(inbound.text, inbound.languageCode)
                }
            }
        }
    }

    override fun setMode(mode: OperationalMode) {
        scope.launch {
            dispatch(ModeEvent.SetMode(mode))
            applyListeningForMode(machine.mode)
        }
    }

    override fun triggerAlert(alertMessage: String?) {
        scope.launch {
            dispatch(ModeEvent.RaiseAlert(alertMessage))
            audio.stopListening()
            audio.stopSpeaking()
            forceMaxVolume()
            val text = alertMessage.orEmpty().ifBlank { "Alert" }
            runCatching { transport.broadcastAlert(text) }
            val engine = audio as? AudioEngineManagerImpl
            if (engine != null) engine.speakAlert(text) else audio.speak(text, "en")
        }
    }

    override fun clearAlert() {
        scope.launch {
            audio.stopSpeaking()
            restoreVolume()
            dispatch(ModeEvent.ClearAlert)
            applyListeningForMode(machine.mode)
        }
    }

    private suspend fun applyRemoteAlert(text: String) {
        dispatch(ModeEvent.RaiseAlert(text))
        audio.stopListening()
        audio.stopSpeaking()
        forceMaxVolume()
        val engine = audio as? AudioEngineManagerImpl
        if (engine != null) engine.speakAlert(text) else audio.speak(text, "en")
    }

    private suspend fun dispatch(event: ModeEvent) {
        mutex.withLock {
            machine = ModeReducer.reduce(machine, event)
            _currentMode.value = machine.mode
            _preAlertMode.value = machine.preAlertMode
        }
    }

    private fun forceMaxVolume() {
        if (!volumeForced) {
            volumeSnapshot = volume.snapshot()
            volumeForced = true
        }
        volume.forceMaxAlert()
    }

    private fun restoreVolume() {
        if (volumeForced) {
            volumeSnapshot?.let { volume.restore(it) }
            volumeForced = false
        }
    }

    private fun applyListeningForMode(mode: OperationalMode) {
        when (mode) {
            OperationalMode.Duplex -> audio.startListening()
            OperationalMode.PushToTalk, OperationalMode.Alert -> audio.stopListening()
        }
    }
}

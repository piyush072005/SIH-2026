package com.isro.itantra.mode

import com.google.common.truth.Truth.assertThat
import com.isro.itantra.audio.VolumeController
import com.isro.itantra.audio.VolumeSnapshot
import com.isro.itantra.domain.contracts.AudioEngineManager
import com.isro.itantra.domain.contracts.TransportManager
import com.isro.itantra.domain.contracts.models.AudioEngineState
import com.isro.itantra.domain.contracts.models.ConnectionState
import com.isro.itantra.domain.contracts.models.DiscoveredPeer
import com.isro.itantra.domain.contracts.models.IncomingMessage
import com.isro.itantra.domain.contracts.models.OperationalMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModeControllerImplTest {
    @Test
    fun duplexStartsListening() = runTest(UnconfinedTestDispatcher()) {
        val audio = FakeAudio()
        val transport = FakeTransport()
        val controller = ModeControllerImpl(audio, transport, FakeVolume(), backgroundScope)
        controller.setMode(OperationalMode.Duplex)
        advanceUntilIdle()
        assertThat(audio.listenStarts).isEqualTo(1)
        assertThat(controller.currentMode.first()).isEqualTo(OperationalMode.Duplex)
    }

    @Test
    fun alertStopsAudioForcesVolumeAndBroadcasts() = runTest(UnconfinedTestDispatcher()) {
        val audio = FakeAudio()
        val transport = FakeTransport()
        val volume = FakeVolume()
        val controller = ModeControllerImpl(audio, transport, volume, backgroundScope)
        controller.setMode(OperationalMode.Duplex)
        advanceUntilIdle()
        controller.triggerAlert("SOS")
        advanceUntilIdle()
        assertThat(audio.stopSpeakCount).isAtLeast(1)
        assertThat(audio.listenStops).isAtLeast(1)
        assertThat(volume.maxCount).isEqualTo(1)
        assertThat(transport.alerts).contains("SOS")
        assertThat(controller.currentMode.first()).isEqualTo(OperationalMode.Alert)
        assertThat(controller.preAlertMode.first()).isEqualTo(OperationalMode.Duplex)
        controller.clearAlert()
        advanceUntilIdle()
        assertThat(controller.currentMode.first()).isEqualTo(OperationalMode.Duplex)
        assertThat(audio.listenStarts).isEqualTo(2)
        assertThat(volume.restoreCount).isEqualTo(1)
    }

    @Test
    fun inboundAlertDoesNotRebroadcast() = runTest(UnconfinedTestDispatcher()) {
        val audio = FakeAudio()
        val transport = FakeTransport()
        ModeControllerImpl(audio, transport, FakeVolume(), backgroundScope)
        transport.incomingMutable.emit(
            IncomingMessage(
                id = "3",
                senderId = "p1",
                senderName = "peer",
                text = "help",
                isAlert = true,
            ),
        )
        advanceUntilIdle()
        assertThat(transport.alerts).isEmpty()
        assertThat(audio.spoken).contains("help")
    }
}

private class FakeAudio : AudioEngineManager {
    val spoken = mutableListOf<String>()
    var listenStarts = 0
    var listenStops = 0
    var stopSpeakCount = 0
    override val transcribedText = MutableSharedFlow<String>()
    override val audioState = MutableStateFlow<AudioEngineState>(AudioEngineState.Ready)
    override val isListening = MutableStateFlow(false)

    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    override fun startListening() {
        listenStarts += 1
        isListening.value = true
    }

    override fun stopListening() {
        listenStops += 1
        isListening.value = false
    }

    override suspend fun speak(text: String, languageCode: String): Result<Unit> {
        spoken += text
        return Result.success(Unit)
    }

    override fun stopSpeaking() {
        stopSpeakCount += 1
    }

    override fun release() = Unit
}

private class FakeTransport : TransportManager {
    val sent = mutableListOf<String>()
    val alerts = mutableListOf<String>()
    val incomingMutable = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 8)
    override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val receivedText = MutableSharedFlow<String>()
    override val incomingMessages: Flow<IncomingMessage> = incomingMutable
    override val discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())

    override fun startAdvertising(deviceName: String) = Unit
    override fun stopAdvertising() = Unit
    override fun startDiscovery() = Unit
    override fun stopDiscovery() = Unit
    override fun connect(peerId: String) = Unit
    override fun disconnect() = Unit
    override suspend fun sendText(text: String): Result<Unit> {
        sent += text
        return Result.success(Unit)
    }

    override suspend fun broadcastAlert(alertMessage: String): Result<Unit> {
        alerts += alertMessage
        return Result.success(Unit)
    }
}

private class FakeVolume : VolumeController {
    var maxCount = 0
    var restoreCount = 0
    override fun snapshot() = VolumeSnapshot(1, 1, 1)
    override fun forceMaxAlert() {
        maxCount += 1
    }

    override fun restore(snapshot: VolumeSnapshot) {
        restoreCount += 1
    }
}

package com.isro.itantra.di

import com.isro.itantra.audio.AndroidVolumeController
import com.isro.itantra.audio.AudioEngineManagerImpl
import com.isro.itantra.audio.VolumeController
import com.isro.itantra.audio.onnx.SherpaOnnxSpeechRuntime
import com.isro.itantra.audio.onnx.SpeechRuntime
import com.isro.itantra.domain.contracts.AudioEngineManager
import com.isro.itantra.domain.contracts.ModeController
import com.isro.itantra.domain.contracts.TransportManager
import com.isro.itantra.mode.ModeControllerImpl
import com.isro.itantra.transport.TransportManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackendBindingsModule {
    @Binds
    @Singleton
    abstract fun audio(impl: AudioEngineManagerImpl): AudioEngineManager

    @Binds
    @Singleton
    abstract fun transport(impl: TransportManagerImpl): TransportManager

    @Binds
    @Singleton
    abstract fun modes(impl: ModeControllerImpl): ModeController

    @Binds
    @Singleton
    abstract fun speechRuntime(impl: SherpaOnnxSpeechRuntime): SpeechRuntime

    @Binds
    @Singleton
    abstract fun volume(impl: AndroidVolumeController): VolumeController
}

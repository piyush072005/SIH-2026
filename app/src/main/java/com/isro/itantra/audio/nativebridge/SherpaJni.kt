package com.isro.itantra.audio.nativebridge

object SherpaJni {
    init {
        runCatching { System.loadLibrary("itantra_sherpa") }
    }

    @JvmStatic
    external fun nativeRuntimeName(): String
}

package com.isro.itantra.audio

interface VolumeController {
    fun snapshot(): VolumeSnapshot
    fun forceMaxAlert()
    fun restore(snapshot: VolumeSnapshot)
}

data class VolumeSnapshot(
    val music: Int,
    val alarm: Int,
    val notification: Int,
)

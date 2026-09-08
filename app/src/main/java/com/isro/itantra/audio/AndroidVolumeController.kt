package com.isro.itantra.audio

import android.content.Context
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidVolumeController @Inject constructor(
    @ApplicationContext context: Context,
) : VolumeController {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun snapshot(): VolumeSnapshot = VolumeSnapshot(
        music = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
        alarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM),
        notification = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION),
    )

    override fun forceMaxAlert() {
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0,
        )
        audioManager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0,
        )
        audioManager.setStreamVolume(
            AudioManager.STREAM_NOTIFICATION,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION),
            0,
        )
    }

    override fun restore(snapshot: VolumeSnapshot) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, snapshot.music, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, snapshot.alarm, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, snapshot.notification, 0)
    }
}

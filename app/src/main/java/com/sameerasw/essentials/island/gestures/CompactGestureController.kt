package com.sameerasw.essentials.island.gestures

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CompactGestureController(
    private val context: Context,
    private val settings: SettingsRepository,
    private val scope: () -> CoroutineScope?,
) : CompactGestures {
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    override val hasLongPress get() = settings.getIslandLongPressAction() != null
    override val hasDoubleTap get() = settings.getIslandDoubleTapAction() != null

    override val slideMode: SlideMode
        get() {
            if (settings.isIslandSlideTrackEnabled() && audioManager.isMusicActive) return SlideMode.Track
            return when (settings.getIslandSlideMode()) {
                "volume" -> SlideMode.Volume
                "brightness" -> SlideMode.Brightness
                "sound_mode" -> SlideMode.SoundMode
                else -> SlideMode.None
            }
        }

    override fun longPress() {
        settings.getIslandLongPressAction()?.let(::execute)
    }

    override fun doubleTap() {
        settings.getIslandDoubleTapAction()?.let(::execute)
    }

    override fun slideStep(forward: Boolean) {
        when (slideMode) {
            SlideMode.Volume -> audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (forward) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI,
            )
            SlideMode.Brightness -> try {
                val current = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
                val next = (current + if (forward) 15 else -15).coerceIn(1, 255)
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, next)
            } catch (_: Exception) {
            }
            else -> {}
        }
    }

    override fun slideCommit(dx: Float) {
        val inverted = settings.isIslandSlideInvertDirectionEnabled()
        val forward = if (inverted) dx < 0f else dx > 0f
        when (slideMode) {
            SlideMode.Track -> mediaKey(if (forward) KeyEvent.KEYCODE_MEDIA_NEXT else KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            SlideMode.SoundMode -> cycleSoundMode(forward)
            else -> {}
        }
    }

    private fun cycleSoundMode(forward: Boolean) {
        try {
            val order = listOf(AudioManager.RINGER_MODE_NORMAL, AudioManager.RINGER_MODE_VIBRATE, AudioManager.RINGER_MODE_SILENT)
            val index = order.indexOf(audioManager.ringerMode).coerceAtLeast(0)
            audioManager.ringerMode = order[(index + if (forward) 1 else order.size - 1) % order.size]
        } catch (_: Exception) {
        }
    }

    private fun mediaKey(code: Int) {
        try {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        } catch (_: Exception) {
        }
    }

    private fun execute(action: Action) {
        scope()?.launch {
            try {
                CombinedActionExecutor.execute(context, action)
            } catch (_: Exception) {
            }
        }
    }
}

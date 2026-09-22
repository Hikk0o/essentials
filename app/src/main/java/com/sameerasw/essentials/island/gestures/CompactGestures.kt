package com.sameerasw.essentials.island.gestures

enum class SlideMode { None, Volume, Brightness, SoundMode, Track }

interface CompactGestures {
    val hasLongPress: Boolean
    val hasDoubleTap: Boolean
    val slideMode: SlideMode

    fun longPress()
    fun doubleTap()

    fun slideStep(forward: Boolean)

    fun slideCommit(dx: Float)

    val hasAny: Boolean get() = hasLongPress || hasDoubleTap || slideMode != SlideMode.None

    object None : CompactGestures {
        override val hasLongPress = false
        override val hasDoubleTap = false
        override val slideMode = SlideMode.None
        override fun longPress() {}
        override fun doubleTap() {}
        override fun slideStep(forward: Boolean) {}
        override fun slideCommit(dx: Float) {}
    }
}

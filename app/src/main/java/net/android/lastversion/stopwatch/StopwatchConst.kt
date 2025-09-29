package net.android.lastversion.stopwatch

object StopwatchConst {
    const val CHANNEL_ID = "stopwatch_channel"
    const val CHANNEL_NAME = "Stopwatch"
    const val NOTI_ID = 12345

    const val ACTION_START = "stopwatch.ACTION_START"
    const val ACTION_PAUSE = "stopwatch.ACTION_PAUSE"
    const val ACTION_RESUME = "stopwatch.ACTION_RESUME"
    const val ACTION_STOP = "stopwatch.ACTION_STOP"
    const val ACTION_GET_STATE = "stopwatch.ACTION_GET_STATE" // Thêm này

    const val ACTION_PROMOTE_FOREGROUND = "stopwatch.ACTION_PROMOTE_FOREGROUND"
    const val ACTION_DEMOTE_FOREGROUND = "stopwatch.ACTION_DEMOTE_FOREGROUND"

    const val ACTION_TICK = "stopwatch.ACTION_TICK"
    const val EXTRA_ELAPSED = "elapsed"
}
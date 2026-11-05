package com.firetv.streamdeck.data

import com.firetv.streamdeck.network.AdbTcpClient
import kotlinx.coroutines.delay

object FireTvKeycodes {
    const val KEYCODE_DPAD_UP = 19
    const val KEYCODE_DPAD_DOWN = 20
    const val KEYCODE_DPAD_LEFT = 21
    const val KEYCODE_DPAD_RIGHT = 22
    const val KEYCODE_DPAD_CENTER = 23
    const val KEYCODE_BACK = 4
    const val KEYCODE_HOME = 3
    const val KEYCODE_MENU = 82
    const val KEYCODE_PLAY_PAUSE = 85
    const val KEYCODE_REWIND = 89
    const val KEYCODE_FAST_FORWARD = 90
    const val KEYCODE_VOLUME_UP = 24
    const val KEYCODE_VOLUME_DOWN = 25
    const val KEYCODE_VOLUME_MUTE = 164
    const val KEYCODE_POWER = 26
}

enum class VideoPreset4K(
    val label: String,
    val refreshRate: Float,
    val description: String,
    val adbCommand: String
) {
    UHD_24HZ(
        label = "2160p @ 24 Hz",
        refreshRate = 24.0f,
        description = "Cinema e contenuti nativi a 23.976 / 24 fps.",
        adbCommand = "cmd display set-user-preferred-display-mode 3840 2160 24.0"
    ),
    UHD_50HZ(
        label = "2160p @ 50 Hz",
        refreshRate = 50.0f,
        description = "Standard europei, broadcast TV e sport.",
        adbCommand = "cmd display set-user-preferred-display-mode 3840 2160 50.0"
    ),
    UHD_60HZ(
        label = "2160p @ 60 Hz",
        refreshRate = 60.0f,
        description = "Standard digitali, YouTube e interfaccia.",
        adbCommand = "cmd display set-user-preferred-display-mode 3840 2160 60.0"
    )
}

class FireTvDeckRepository {
    suspend fun pingSocket(host: String, port: Int = 5555): Result<Long> {
        return AdbTcpClient(host, port).testConnection()
    }

    suspend fun sendKey(host: String, port: Int = 5555, keycode: Int): Result<String> {
        return AdbTcpClient(host, port).executeShellCommand("input keyevent ")
    }

    suspend fun executeStreamInfoMacro(
        host: String,
        port: Int = 5555,
        onProgress: (remainingMs: Long) -> Unit = {}
    ): Result<String> {
        val client = AdbTcpClient(host, port)
        client.executeShellCommand("input keyevent 23 20")

        val totalPauseMs = 1200L
        val stepMs = 100L
        var elapsed = 0L
        while (elapsed < totalPauseMs) {
            onProgress(totalPauseMs - elapsed)
            delay(stepMs)
            elapsed += stepMs
        }
        onProgress(0L)

        return client.executeShellCommand(
            "input keyevent 82 || am start -n com.amazon.ssm/com.amazon.ssm.ControlPanelActivity"
        )
    }

    suspend fun applyVideoPreset(
        host: String,
        port: Int = 5555,
        preset: VideoPreset4K
    ): Result<String> {
        val client = AdbTcpClient(host, port)
        val command = " && settings put global match_content_frame_rate 1"
        return client.executeShellCommand(command)
    }
}

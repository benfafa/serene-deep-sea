package com.fanqieyu.deepfocus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class DanmakuMessage(
    val id: Long,
    val appName: String,
    val title: String,
    val text: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * 内存级通知弹幕事件总线，仅实时派发，不落盘隐私通知内容。
 */
object DanmakuNotificationBus {
    private val _events = MutableSharedFlow<DanmakuMessage>(
        replay = 0,
        extraBufferCapacity = 32,
    )
    val events: SharedFlow<DanmakuMessage> = _events.asSharedFlow()

    fun emit(message: DanmakuMessage) {
        _events.tryEmit(message)
    }
}

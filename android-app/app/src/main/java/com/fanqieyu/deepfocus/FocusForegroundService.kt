package com.fanqieyu.deepfocus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: FocusRepository
    private var commandJob: Job? = null
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = FocusRepository.getInstance(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialState = repository.state.value
        startForegroundCompat(buildProgressNotification(initialState))
        commandJob?.cancel()
        commandJob = serviceScope.launch {
            when (intent?.action) {
                ACTION_PAUSE -> repository.pause()
                ACTION_RESUME -> repository.resume()
                ACTION_ABORT -> repository.abort()
                ACTION_COMPLETE -> repository.complete()
            }
            val restored = repository.restoreIfNeeded()
            if (restored.status == FocusStatus.IDLE ||
                restored.status == FocusStatus.COMPLETED ||
                restored.status == FocusStatus.ABORTED
            ) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                startTimerIfNeeded()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        timerJob?.cancel()
        commandJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTimerIfNeeded() {
        if (timerJob?.isActive == true) return
        timerJob = serviceScope.launch {
            while (isActive) {
                val current = repository.tick()
                updateProgressNotification(current)
                if (current.status == FocusStatus.RUNNING && current.remainingSeconds <= 0) {
                    val finished = repository.complete()
                    postCompletionNotification(finished)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                if (current.status == FocusStatus.IDLE ||
                    current.status == FocusStatus.COMPLETED ||
                    current.status == FocusStatus.ABORTED
                ) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                delay(1000L)
            }
        }
    }

    private fun updateProgressNotification(state: FocusSessionState) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            buildProgressNotification(state),
        )
    }

    private fun postCompletionNotification(state: FocusSessionState) {
        getSystemService(NotificationManager::class.java).notify(
            COMPLETION_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("专注完成")
                .setContentText("${state.sceneName} 已完成本轮专注，愿你保持这份平静。")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build(),
        )
    }

    private fun buildProgressNotification(state: FocusSessionState): Notification {
        val minutes = (state.remainingSeconds.coerceAtLeast(0) / 60).toString().padStart(2, '0')
        val seconds = (state.remainingSeconds.coerceAtLeast(0) % 60).toString().padStart(2, '0')
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("深海禅定 · ${state.sceneName}")
            .setContentText("剩余 $minutes:$seconds")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(mainActivityPendingIntent())

        when (state.status) {
            FocusStatus.RUNNING -> builder.addAction(action("暂停", ACTION_PAUSE))
            FocusStatus.PAUSED -> builder.addAction(action("继续", ACTION_RESUME))
            else -> Unit
        }
        if (state.status == FocusStatus.RUNNING || state.status == FocusStatus.PAUSED) {
            builder.addAction(action("结束", ACTION_ABORT))
        }
        return builder.build()
    }

    private fun action(label: String, action: String): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_pause,
            label,
            servicePendingIntent(action),
        ).build()

    private fun servicePendingIntent(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, FocusForegroundService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun mainActivityPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        MAIN_ACTIVITY_REQUEST_CODE,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "专注倒计时",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "显示正在进行的深海禅定倒计时"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_PAUSE = "com.fanqieyu.deepfocus.action.PAUSE"
        const val ACTION_RESUME = "com.fanqieyu.deepfocus.action.RESUME"
        const val ACTION_ABORT = "com.fanqieyu.deepfocus.action.ABORT"
        const val ACTION_STOP = ACTION_ABORT
        const val ACTION_COMPLETE = "com.fanqieyu.deepfocus.action.COMPLETE"
        private const val CHANNEL_ID = "focus_countdown"
        private const val NOTIFICATION_ID = 1001
        private const val COMPLETION_NOTIFICATION_ID = 1002
        private const val MAIN_ACTIVITY_REQUEST_CODE = 1003

        fun start(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

package com.fanqieyu.deepfocus

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class FocusNotificationListenerService : NotificationListenerService() {
    private var database: LocalAquariumDatabase? = null

    override fun onCreate() {
        super.onCreate()
        database = LocalAquariumDatabase(applicationContext)
    }

    override fun onDestroy() {
        database?.close()
        database = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName == packageName || sbn.isOngoing) return
        val db = database ?: LocalAquariumDatabase(applicationContext).also { database = it }
        val matched = db.loadWhitelistApps().firstOrNull {
            it.enabled && it.packageName == sbn.packageName
        } ?: return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()?.take(48).orEmpty()
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString()
            ?.trim()
            ?.take(48)
            .orEmpty()
        if (title.isEmpty() && body.isEmpty()) return

        DanmakuNotificationBus.emit(
            DanmakuMessage(
                id = System.nanoTime(),
                appName = matched.appName,
                title = title,
                text = body,
                packageName = sbn.packageName,
            ),
        )
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ).orEmpty()
            val componentName = ComponentName(context, FocusNotificationListenerService::class.java).flattenToString()
            return flat.contains(componentName) || flat.contains(FocusNotificationListenerService::class.java.name)
        }

        fun openSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

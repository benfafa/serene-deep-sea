package com.fanqieyu.deepfocus

import android.app.Activity
import android.app.ActivityManager

interface LockTaskController {
    fun start(activity: Activity): Boolean
    fun stop(activity: Activity): Boolean
    fun isLocked(activity: Activity): Boolean
}

class AndroidLockTaskController : LockTaskController {
    override fun start(activity: Activity): Boolean = runCatching {
        activity.startLockTask()
    }.isSuccess

    override fun stop(activity: Activity): Boolean = runCatching {
        activity.stopLockTask()
    }.isSuccess

    override fun isLocked(activity: Activity): Boolean =
        activity.getSystemService(ActivityManager::class.java)?.lockTaskModeState !=
            ActivityManager.LOCK_TASK_MODE_NONE
}

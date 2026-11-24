package com.fanqieyu.deepfocus

enum class FocusStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    ABORTED,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class FocusSessionState(
    val status: FocusStatus = FocusStatus.IDLE,
    val sessionId: Long? = null,
    val sceneName: String = DEFAULT_SCENE_NAME,
    val plannedMinutes: Int = 0,
    val remainingSeconds: Int = 0,
    val startedAt: Long? = null,
    val updatedAt: Long? = null,
)

data class FocusRecord(
    val id: Long,
    val sceneName: String,
    val plannedMinutes: Int,
    val actualSeconds: Int,
    val rewardCoins: Int,
    val status: FocusStatus,
    val startedAt: Long,
    val endedAt: Long,
)

data class FocusStats(
    val totalMinutes: Int,
    val todayMinutes: Int,
    val successCount: Int,
    val failCount: Int,
    val completionRate: Int,
    val totalEarnedCoins: Int,
)

data class WhitelistApp(
    val packageName: String,
    val appName: String,
    val enabled: Boolean = true,
)

data class AppState(
    val selectedSceneName: String = DEFAULT_SCENE_NAME,
    val todayFocusMinutes: Int = 0,
    val todayDate: String = "",
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

/** Completed sessions earn coins scaled by focused minutes; aborted sessions earn zero. */
fun calculateRewardCoins(
    actualSeconds: Int,
    plannedMinutes: Int,
    status: FocusStatus,
): Int {
    if (status != FocusStatus.COMPLETED || plannedMinutes <= 0) return 0
    val safeSeconds = actualSeconds.coerceAtLeast(0)
    val mins = (safeSeconds / 60).coerceAtLeast(
        if (safeSeconds >= plannedMinutes * 60) plannedMinutes else 0,
    )
    if (mins <= 0) return 0
    return when {
        mins >= 90 -> 260 + (mins - 90) * 3
        mins >= 50 -> 140 + (mins - 50) * 3
        mins >= 25 -> 80 + (mins - 25) * 2
        else -> (mins * 3).coerceAtLeast(5)
    }
}

fun rewardCoinsForCompletedMinutes(minutes: Int): Int =
    calculateRewardCoins(minutes.coerceAtLeast(0) * 60, minutes, FocusStatus.COMPLETED)

fun computeFocusStats(records: List<FocusRecord>, todayMinutes: Int): FocusStats {
    var totalSeconds = 0
    var success = 0
    var fail = 0
    var coins = 0
    records.forEach { rec ->
        totalSeconds += rec.actualSeconds.coerceAtLeast(0)
        if (rec.status == FocusStatus.COMPLETED) {
            success += 1
        } else if (rec.status == FocusStatus.ABORTED) {
            fail += 1
        }
        coins += rec.rewardCoins.coerceAtLeast(0)
    }
    val totalCount = success + fail
    val rate = if (totalCount == 0) 0 else (success * 100) / totalCount
    return FocusStats(
        totalMinutes = totalSeconds / 60,
        todayMinutes = todayMinutes.coerceAtLeast(0),
        successCount = success,
        failCount = fail,
        completionRate = rate,
        totalEarnedCoins = coins,
    )
}

const val DEFAULT_SCENE_NAME = "月光海沟"

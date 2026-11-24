package com.fanqieyu.deepfocus

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class FocusRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = LocalAquariumDatabase(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _sessionState = MutableStateFlow(FocusSessionState())
    private val _coins = MutableStateFlow(LocalAquariumDatabase.INITIAL_COINS)
    private val _selectedSceneName = MutableStateFlow(DEFAULT_SCENE_NAME)
    private val _todayFocusMinutes = MutableStateFlow(0)
    private val _userName = MutableStateFlow("")
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val _userNameLoaded = MutableStateFlow(false)

    val sessionState: StateFlow<FocusSessionState> = _sessionState.asStateFlow()
    val state: StateFlow<FocusSessionState> = sessionState
    val coins: StateFlow<Int> = _coins.asStateFlow()
    val selectedSceneName: StateFlow<String> = _selectedSceneName.asStateFlow()
    val todayFocusMinutes: StateFlow<Int> = _todayFocusMinutes.asStateFlow()
    val userName: StateFlow<String> = _userName.asStateFlow()
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    val userNameLoaded: StateFlow<Boolean> = _userNameLoaded.asStateFlow()

    init {
        scope.launch {
            mutex.withLock { refreshPersistentStateLocked() }
        }
    }

    suspend fun start(minutes: Int, sceneName: String = selectedSceneName.value): FocusSessionState {
        require(minutes > 0) { "Focus duration must be positive" }
        val result = withContext(Dispatchers.IO) {
            mutex.withLock {
                val existing = restoreLocked(System.currentTimeMillis())
                if (existing.status == FocusStatus.RUNNING || existing.status == FocusStatus.PAUSED) {
                    return@withLock existing
                }
                val now = System.currentTimeMillis()
                val newState = FocusSessionState(
                    status = FocusStatus.RUNNING,
                    sessionId = now,
                    sceneName = sceneName.ifBlank { DEFAULT_SCENE_NAME },
                    plannedMinutes = minutes,
                    remainingSeconds = minutes * 60,
                    startedAt = now,
                    updatedAt = now,
                )
                database.saveActiveSession(newState)
                _sessionState.value = newState
                newState
            }
        }
        FocusForegroundService.start(appContext)
        return result
    }

    suspend fun pause(): FocusSessionState = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = restoreLocked(System.currentTimeMillis())
            if (current.status != FocusStatus.RUNNING || current.sessionId == null) return@withLock current
            val paused = current.copy(status = FocusStatus.PAUSED, updatedAt = System.currentTimeMillis())
            database.saveActiveSession(paused)
            _sessionState.value = paused
            paused
        }
    }

    suspend fun resume(): FocusSessionState = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = restoreLocked(System.currentTimeMillis())
            if (current.status != FocusStatus.PAUSED || current.sessionId == null) return@withLock current
            val resumed = current.copy(status = FocusStatus.RUNNING, updatedAt = System.currentTimeMillis())
            database.saveActiveSession(resumed)
            _sessionState.value = resumed
            resumed
        }
    }

    suspend fun abort(): FocusSessionState = withContext(Dispatchers.IO) {
        val result = mutex.withLock { finishLocked(FocusStatus.ABORTED, System.currentTimeMillis()) }
        if (result.status == FocusStatus.ABORTED) stopService()
        result
    }

    suspend fun complete(): FocusSessionState = withContext(Dispatchers.IO) {
        val result = mutex.withLock { finishLocked(FocusStatus.COMPLETED, System.currentTimeMillis()) }
        if (result.status == FocusStatus.COMPLETED) stopService()
        result
    }

    suspend fun restoreIfNeeded(): FocusSessionState = withContext(Dispatchers.IO) {
        mutex.withLock { restoreLocked(System.currentTimeMillis()) }
    }

    suspend fun tick(): FocusSessionState = restoreIfNeeded()

    suspend fun purchaseFish(costCoins: Int): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val success = database.spendCoins(costCoins)
            if (success) _coins.value = database.loadCoins()
            success
        }
    }

    suspend fun setSelectedScene(sceneName: String) {
        val normalized = sceneName.ifBlank { DEFAULT_SCENE_NAME }
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val appState = database.loadAppState().copy(selectedSceneName = normalized)
                database.saveAppState(appState)
                _selectedSceneName.value = normalized
                _todayFocusMinutes.value = appState.todayFocusMinutes
            }
        }
    }

    suspend fun setUserName(name: String) {
        val trimmed = name.trim().take(16)
        if (trimmed.isEmpty()) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val appState = database.loadAppState().copy(userName = trimmed)
                database.saveAppState(appState)
                _userName.value = trimmed
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val appState = database.loadAppState().copy(themeMode = mode)
                database.saveAppState(appState)
                _themeMode.value = mode
            }
        }
    }

    suspend fun getFocusHistory(): List<FocusRecord> = withContext(Dispatchers.IO) {
        mutex.withLock { database.loadFocusRecords() }
    }

    suspend fun getFocusStats(): FocusStats = withContext(Dispatchers.IO) {
        mutex.withLock {
            val appState = database.loadAppState()
            computeFocusStats(database.loadFocusRecords(), appState.todayFocusMinutes)
        }
    }

    suspend fun getWhitelistApps(): List<WhitelistApp> = withContext(Dispatchers.IO) {
        mutex.withLock { database.loadWhitelistApps() }
    }

    suspend fun saveWhitelistApp(app: WhitelistApp) {
        withContext(Dispatchers.IO) { mutex.withLock { database.saveWhitelistApp(app) } }
    }

    suspend fun removeWhitelistApp(packageName: String) {
        withContext(Dispatchers.IO) { mutex.withLock { database.removeWhitelistApp(packageName) } }
    }

    suspend fun setWhitelistEnabled(packageName: String, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val app = database.loadWhitelistApps().firstOrNull { it.packageName == packageName }
                    ?: return@withLock
                database.saveWhitelistApp(app.copy(enabled = enabled))
            }
        }
    }

    private fun refreshPersistentStateLocked() {
        _coins.value = database.loadCoins()
        val appState = database.loadAppState()
        _selectedSceneName.value = appState.selectedSceneName
        _todayFocusMinutes.value = appState.todayFocusMinutes
        _userName.value = appState.userName
        _themeMode.value = appState.themeMode
        _userNameLoaded.value = true
        restoreLocked(System.currentTimeMillis())
    }

    private fun restoreLocked(now: Long): FocusSessionState {
        val persisted = database.loadActiveSession()
        if (persisted == null) {
            val idle = FocusSessionState()
            _sessionState.value = idle
            return idle
        }
        if (persisted.status != FocusStatus.RUNNING) {
            _sessionState.value = persisted
            return persisted
        }
        val elapsedSeconds = ((now - (persisted.updatedAt ?: now)) / 1000L)
            .coerceAtLeast(0L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        if (elapsedSeconds == 0) {
            _sessionState.value = persisted
            return persisted
        }
        val restored = persisted.copy(
            remainingSeconds = (persisted.remainingSeconds - elapsedSeconds).coerceAtLeast(0),
            updatedAt = now,
        )
        database.saveActiveSession(restored)
        _sessionState.value = restored
        return restored
    }

    private fun finishLocked(status: FocusStatus, endedAt: Long): FocusSessionState {
        val current = restoreLocked(endedAt)
        if (current.sessionId == null || current.status == FocusStatus.IDLE ||
            current.status == FocusStatus.COMPLETED || current.status == FocusStatus.ABORTED
        ) {
            return current
        }
        val actualSeconds = (current.plannedMinutes * 60 - current.remainingSeconds)
            .coerceIn(0, current.plannedMinutes * 60)
        val rewardCoins = calculateRewardCoins(actualSeconds, current.plannedMinutes, status)
        val record = FocusRecord(
            id = current.sessionId,
            sceneName = current.sceneName,
            plannedMinutes = current.plannedMinutes,
            actualSeconds = actualSeconds,
            rewardCoins = rewardCoins,
            status = status,
            startedAt = requireNotNull(current.startedAt),
            endedAt = endedAt,
        )
        val appState = database.loadAppState().let {
            if (status == FocusStatus.COMPLETED) {
                it.copy(todayFocusMinutes = it.todayFocusMinutes + actualSeconds / 60)
            } else {
                it
            }
        }
        database.persistFinishedSession(record, database.loadCoins() + rewardCoins, appState)
        val finished = current.copy(status = status, remainingSeconds = 0, updatedAt = endedAt)
        _coins.value = database.loadCoins()
        _todayFocusMinutes.value = appState.todayFocusMinutes
        _sessionState.value = finished
        return finished
    }

    private fun stopService() {
        appContext.stopService(Intent(appContext, FocusForegroundService::class.java))
    }

    companion object {
        @Volatile
        private var instance: FocusRepository? = null

        fun getInstance(context: Context): FocusRepository =
            instance ?: synchronized(this) {
                instance ?: FocusRepository(context).also { instance = it }
            }
    }
}

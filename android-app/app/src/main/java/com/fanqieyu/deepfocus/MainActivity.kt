package com.fanqieyu.deepfocus

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class TouchInteractionMode {
    ATTRACT,
    SCATTER,
}

data class OceanTouchState(
    val pointDp: Offset,
    val mode: TouchInteractionMode,
    val id: Long,
)

class MainActivity : ComponentActivity() {
    private val lockTaskController = AndroidLockTaskController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val repository = remember { FocusRepository.getInstance(applicationContext) }
            val themeMode by repository.themeMode.collectAsState()
            DeepFocusLiquidTheme(themeMode = themeMode) {
                DeepFocusApp(
                    activity = this,
                    repository = repository,
                    lockTaskController = lockTaskController,
                )
            }
        }
    }
}

@Composable
private fun DeepFocusApp(
    activity: Activity,
    repository: FocusRepository,
    lockTaskController: LockTaskController,
) {
    val palette = LocalDeepFocusPalette.current
    val database = remember { LocalAquariumDatabase(activity.applicationContext) }
    DisposableEffect(database) {
        onDispose { database.close() }
    }
    val scope = rememberCoroutineScope()
    val sessionState by repository.sessionState.collectAsState()
    val selectedSceneName by repository.selectedSceneName.collectAsState()
    val todayFocusMinutes by repository.todayFocusMinutes.collectAsState()
    val userName by repository.userName.collectAsState()
    val themeMode by repository.themeMode.collectAsState()
    val userNameLoaded by repository.userNameLoaded.collectAsState()

    var destination by remember { mutableStateOf(Destination.Aquarium) }
    var showFocusDialog by remember { mutableStateOf(false) }
    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var showFocusFailedDialog by remember { mutableStateOf(false) }
    var showTodoDialog by remember { mutableStateOf(false) }
    var showEditNicknameDialog by remember { mutableStateOf(false) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    var todoDraft by remember { mutableStateOf("") }
    var nextTodoId by remember(database) {
        mutableStateOf((database.loadTodosOrNull()?.maxOfOrNull { it.id } ?: 3) + 1)
    }
    var focusMinutes by remember(database) { mutableStateOf(database.loadFocusMinutes()) }
    var hudVisible by remember { mutableStateOf(false) }
    var hudToken by remember { mutableStateOf(0) }
    var focusHistory by remember { mutableStateOf<List<FocusRecord>>(emptyList()) }
    var focusStats by remember { mutableStateOf(FocusStats(0, 0, 0, 0, 0, 0)) }
    var whitelistApps by remember { mutableStateOf<List<WhitelistApp>>(emptyList()) }
    var listenerEnabled by remember { mutableStateOf(FocusNotificationListenerService.isEnabled(activity)) }
    var weatherState by remember { mutableStateOf(WeatherUiState()) }
    var lockTaskEngaged by remember { mutableStateOf(false) }
    val activeDanmaku = remember { mutableStateListOf<DanmakuMessage>() }

    val deployedFish = remember(database) {
        val storedFish = database.loadDeployedFish()
        mutableStateListOf<Fish>().apply {
            addAll(storedFish.ifEmpty { AquariumCatalog.defaultDeployedFish })
        }
    }
    val todos = remember(database) {
        val storedTodos = database.loadTodosOrNull()
        mutableStateListOf<TodoItem>().apply {
            addAll(
                storedTodos ?: listOf(
                    TodoItem(1, "回复重要邮件", completed = true),
                    TodoItem(2, "整理产品灵感"),
                    TodoItem(3, "给妈妈打电话"),
                ),
            )
        }
    }

    val currentScene = AquariumCatalog.findSceneByName(selectedSceneName) ?: AquariumCatalog.scenes.first()

    fun refreshWeather() {
        scope.launch {
            weatherState = weatherState.copy(isLoading = true)
            weatherState = LocationWeatherManager.fetchWeather(activity.applicationContext)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshWeather()
    }

    LaunchedEffect(Unit) {
        DanmakuNotificationBus.events.collect { msg ->
            if (activeDanmaku.size >= 8) {
                activeDanmaku.removeAt(0)
            }
            activeDanmaku.add(msg)
        }
    }

    LaunchedEffect(repository) {
        val restored = repository.restoreIfNeeded()
        if (restored.status == FocusStatus.RUNNING) {
            FocusForegroundService.start(activity)
        }
        focusHistory = repository.getFocusHistory()
        focusStats = repository.getFocusStats()
        var currentWhitelist = repository.getWhitelistApps()
        if (currentWhitelist.isEmpty()) {
            listOf(
                WhitelistApp("com.android.notes", "备忘录", true),
                WhitelistApp("com.android.weather", "天气", true),
                WhitelistApp("com.tencent.mm", "微信", true),
            ).forEach { repository.saveWhitelistApp(it) }
            currentWhitelist = repository.getWhitelistApps()
        }
        whitelistApps = currentWhitelist
        if (!LocationWeatherManager.hasLocationPermission(activity)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        } else {
            refreshWeather()
        }
    }

    LaunchedEffect(sessionState.status) {
        if (sessionState.status == FocusStatus.COMPLETED || sessionState.status == FocusStatus.ABORTED) {
            lockTaskEngaged = false
            focusHistory = repository.getFocusHistory()
            focusStats = repository.getFocusStats()
            if (lockTaskController.isLocked(activity)) {
                lockTaskController.stop(activity)
            }
        } else if (sessionState.status == FocusStatus.RUNNING || sessionState.status == FocusStatus.PAUSED) {
            while (true) {
                val lockedNow = lockTaskController.isLocked(activity)
                if (lockedNow) {
                    lockTaskEngaged = true
                } else if (lockTaskEngaged) {
                    lockTaskEngaged = false
                    repository.abort()
                    focusHistory = repository.getFocusHistory()
                    focusStats = repository.getFocusStats()
                    showFocusFailedDialog = true
                    break
                }
                delay(800)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sessionState.status) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = FocusNotificationListenerService.isEnabled(activity)
            } else if (event == Lifecycle.Event.ON_STOP) {
                val active = sessionState.status == FocusStatus.RUNNING || sessionState.status == FocusStatus.PAUSED
                val powerManager = activity.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val screenOn = powerManager?.isInteractive ?: true
                if (active && screenOn && !activity.isChangingConfigurations) {
                    scope.launch {
                        lockTaskEngaged = false
                        repository.abort()
                        focusHistory = repository.getFocusHistory()
                        focusStats = repository.getFocusStats()
                        showFocusFailedDialog = true
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(destination) {
        listenerEnabled = FocusNotificationListenerService.isEnabled(activity)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        @Suppress("DEPRECATION")
        if (destination == Destination.Aquarium) {
            activity.window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else {
            activity.window.decorView.systemUiVisibility = 0
            hudVisible = false
        }
    }

    LaunchedEffect(hudToken) {
        if (hudToken == 0) return@LaunchedEffect
        delay(5000)
        hudVisible = false
    }

    fun revealHud() {
        hudVisible = true
        hudToken += 1
    }

    fun toggleHudFromBackground() {
        if (hudVisible) {
            hudVisible = false
            hudToken += 1
        } else {
            revealHud()
        }
    }

    Surface(color = palette.background, modifier = Modifier.fillMaxSize()) {
        if (destination == Destination.Aquarium) {
            AquariumRoomScreen(
                currentScene = currentScene,
                deployedFish = deployedFish,
                todos = todos,
                focusMinutes = focusMinutes,
                sessionState = sessionState,
                todayFocusMinutes = todayFocusMinutes,
                weatherState = weatherState,
                hudVisible = hudVisible,
                danmakuMessages = activeDanmaku,
                onDanmakuExpired = { id -> activeDanmaku.removeAll { it.id == id } },
                onRefreshWeather = { refreshWeather() },
                onBackgroundTap = ::toggleHudFromBackground,
                onDestinationSelected = { destination = it },
                onStartFocus = { showFocusDialog = true },
                onPauseFocus = { scope.launch { repository.pause(); revealHud() } },
                onResumeFocus = { scope.launch { repository.resume(); revealHud() } },
                onAbortFocus = {
                    scope.launch {
                        lockTaskEngaged = false
                        repository.abort()
                        lockTaskController.stop(activity)
                        focusHistory = repository.getFocusHistory()
                        focusStats = repository.getFocusStats()
                        showFocusFailedDialog = true
                        revealHud()
                    }
                },
                onFocusMinutesChange = { focusMinutes = it; database.saveFocusMinutes(it); revealHud() },
                onOpenCustomDuration = { showCustomDurationDialog = true; revealHud() },
                onAddTodo = { revealHud(); todoDraft = ""; showTodoDialog = true },
                onToggleTodo = { id ->
                    val index = todos.indexOfFirst { it.id == id }
                    if (index >= 0) todos[index] = todos[index].copy(completed = !todos[index].completed)
                    database.replaceTodos(todos)
                    revealHud()
                },
                onDeleteTodo = { id ->
                    todos.removeAll { it.id == id }
                    database.replaceTodos(todos)
                    revealHud()
                },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(palette.background, palette.backgroundAccent)),
                    )
                    .safeDrawingPadding(),
            ) {
                TopHeader(
                    destination = destination,
                    userName = userName.ifBlank { "深海旅人" },
                )
                Box(modifier = Modifier.weight(1f)) {
                    when (destination) {
                        Destination.Scenes -> ScenesScreen(
                            selectedSceneName = currentScene.name,
                            onSelectScene = { scene ->
                                scope.launch {
                                    repository.setSelectedScene(scene.name)
                                    Toast.makeText(activity, "已切换到「${scene.name}」", Toast.LENGTH_SHORT).show()
                                    destination = Destination.Aquarium
                                }
                            },
                        )
                        Destination.Nursery -> NurseryScreen(
                            deployedFishNames = deployedFish.map { it.name }.toSet(),
                            onToggleDeployFish = { fish ->
                                val exists = deployedFish.any { it.name == fish.name }
                                if (exists) {
                                    if (deployedFish.size <= 1) {
                                        Toast.makeText(activity, "水族箱里至少保留一位住客", Toast.LENGTH_SHORT).show()
                                    } else {
                                        deployedFish.removeAll { it.name == fish.name }
                                        database.replaceDeployedFish(deployedFish)
                                        Toast.makeText(activity, "已将 ${fish.name} 移出水族箱", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    deployedFish.add(fish)
                                    database.replaceDeployedFish(deployedFish)
                                    Toast.makeText(activity, "已将 ${fish.name} 投放到水族箱", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        Destination.Settings -> SettingsScreen(
                            userName = userName.ifBlank { "深海旅人" },
                            themeMode = themeMode,
                            focusMinutes = focusMinutes,
                            isLocked = lockTaskController.isLocked(activity),
                            listenerEnabled = listenerEnabled,
                            whitelistApps = whitelistApps,
                            focusHistory = focusHistory,
                            focusStats = focusStats,
                            onEditNickname = { showEditNicknameDialog = true },
                            onThemeModeChange = { mode -> scope.launch { repository.setThemeMode(mode) } },
                            onFocusMinutesChange = { focusMinutes = it; database.saveFocusMinutes(it) },
                            onOpenCustomDuration = { showCustomDurationDialog = true },
                            onOpenNotificationSettings = { FocusNotificationListenerService.openSettings(activity) },
                            onToggleWhitelist = { app ->
                                scope.launch {
                                    repository.setWhitelistEnabled(app.packageName, !app.enabled)
                                    whitelistApps = repository.getWhitelistApps()
                                }
                            },
                            onOpenAppPicker = { showAppPickerDialog = true },
                            onSendTestDanmaku = {
                                DanmakuNotificationBus.emit(
                                    DanmakuMessage(
                                        id = System.nanoTime(),
                                        appName = "微信",
                                        title = "深海测试消息",
                                        text = "白名单通知已转为水族箱弹幕",
                                        packageName = "com.tencent.mm",
                                    ),
                                )
                                destination = Destination.Aquarium
                            },
                            onStartLock = {
                                val started = lockTaskController.start(activity)
                                if (!started) {
                                    Toast.makeText(activity, "当前设备未允许锁定任务模式", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        Destination.Aquarium -> Unit
                    }
                }
                BottomNavigation(destination, onDestinationSelected = { destination = it })
            }
        }
    }

    if (userNameLoaded && userName.isBlank()) {
        NicknameDialog(
            isInitialSetup = true,
            initialName = "",
            onDismiss = null,
            onConfirm = { name ->
                scope.launch { repository.setUserName(name) }
            },
        )
    }

    if (showEditNicknameDialog) {
        NicknameDialog(
            isInitialSetup = false,
            initialName = userName,
            onDismiss = { showEditNicknameDialog = false },
            onConfirm = { name ->
                scope.launch {
                    repository.setUserName(name)
                    showEditNicknameDialog = false
                }
            },
        )
    }

    if (showFocusDialog) {
        AlertDialog(
            onDismissRequest = { showFocusDialog = false },
            title = { Text("准备潜入${currentScene.name}？") },
            text = {
                Text(
                    "本轮时长 ${focusMinutes} 分钟。\n" +
                        "进入禅定后将锁定当前屏幕，中途退出锁定或离开应用将直接结束本次禅定并记录为失败。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFocusDialog = false
                    scope.launch {
                        lockTaskEngaged = false
                        repository.start(focusMinutes, currentScene.name)
                        val started = lockTaskController.start(activity)
                        if (started) {
                            lockTaskEngaged = true
                        } else {
                            Toast.makeText(activity, "已开启专注倒计时，请勿离开应用", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("锁定并开始") }
            },
            dismissButton = {
                TextButton(onClick = { showFocusDialog = false }) { Text("稍后") }
            },
        )
    }

    if (showFocusFailedDialog) {
        AlertDialog(
            onDismissRequest = { showFocusFailedDialog = false },
            title = { Text("禅定已中断 · 记录失败") },
            text = { Text("检测到退出锁定或离开专注界面，本轮禅定已结束并记录为失败。") },
            confirmButton = {
                TextButton(onClick = { showFocusFailedDialog = false }) { Text("知道了") }
            },
        )
    }

    if (showCustomDurationDialog) {
        CustomDurationDialog(
            initialMinutes = focusMinutes,
            onDismiss = { showCustomDurationDialog = false },
            onConfirm = { minutes ->
                focusMinutes = minutes
                database.saveFocusMinutes(minutes)
                showCustomDurationDialog = false
            },
        )
    }

    if (showTodoDialog) {
        AlertDialog(
            onDismissRequest = { showTodoDialog = false },
            title = { Text("新增 Todo") },
            text = {
                OutlinedTextField(
                    value = todoDraft,
                    onValueChange = { todoDraft = it },
                    singleLine = true,
                    label = { Text("任务内容") },
                    placeholder = { Text("例如：整理水族箱灵感") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = todoDraft.trim()
                        if (title.isNotEmpty()) {
                            todos.add(TodoItem(nextTodoId, title))
                            database.replaceTodos(todos)
                            nextTodoId += 1
                            todoDraft = ""
                            showTodoDialog = false
                        }
                    },
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showTodoDialog = false }) { Text("取消") }
            },
        )
    }

    if (showAppPickerDialog) {
        InstalledAppPickerDialog(
            activity = activity,
            existingWhitelist = whitelistApps,
            onDismiss = { showAppPickerDialog = false },
            onToggleApp = { appInfo, shouldEnable ->
                scope.launch {
                    if (shouldEnable) {
                        repository.saveWhitelistApp(WhitelistApp(appInfo.packageName, appInfo.appName, true))
                    } else {
                        repository.setWhitelistEnabled(appInfo.packageName, false)
                    }
                    whitelistApps = repository.getWhitelistApps()
                }
            },
        )
    }
}

@Composable
private fun NicknameDialog(
    isInitialSetup: Boolean,
    initialName: String,
    onDismiss: (() -> Unit)?,
    onConfirm: (String) -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    var draft by remember { mutableStateOf(initialName) }
    val presets = listOf("深海旅人", "珊瑚汐", "蓝鲸鱼", "星海潜行")

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrandLogoMark(size = 38.dp)
                Column {
                    Text(
                        if (isInitialSetup) "欢迎潜入深海禅定" else "修改潜水员昵称",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("设定你的专属名称", color = p.muted, fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 16) draft = it },
                    singleLine = true,
                    label = { Text("用户名称") },
                    placeholder = { Text("输入你的昵称（1~16 字）") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { preset ->
                        Text(
                            text = preset,
                            color = p.accent,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(p.accent.copy(alpha = 0.14f))
                                .clickable { draft = preset }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = draft.trim()
                    if (finalName.isNotEmpty()) onConfirm(finalName)
                },
                enabled = draft.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.accentOn),
            ) {
                Text(if (isInitialSetup) "潜入深海" else "保存", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = if (onDismiss != null) {
            {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        } else {
            null
        },
    )
}

@Composable
private fun CustomDurationDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    var minutes by remember { mutableStateOf(initialMinutes.coerceIn(1, 180)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义禅定时长", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$minutes 分钟", color = p.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { minutes = it.roundToInt().coerceIn(1, 180) },
                    valueRange = 1f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = p.accent,
                        activeTrackColor = p.accent,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { minutes = (minutes - 5).coerceAtLeast(1) }) { Text("-5m") }
                    OutlinedButton(onClick = { minutes = (minutes + 5).coerceAtMost(180) }) { Text("+5m") }
                    OutlinedButton(onClick = { minutes = 45 }) { Text("45m") }
                    OutlinedButton(onClick = { minutes = 120 }) { Text("120m") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(minutes) },
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.accentOn),
            ) { Text("确定", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun InstalledAppPickerDialog(
    activity: Activity,
    existingWhitelist: List<WhitelistApp>,
    onDismiss: () -> Unit,
    onToggleApp: (InstalledAppInfo, Boolean) -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = InstalledAppsProvider.loadLaunchableApps(activity.applicationContext)
        loading = false
    }

    val enabledPackages = existingWhitelist.filter { it.enabled }.map { it.packageName }.toSet()
    val filteredApps = remember(apps, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) apps else apps.filter { it.appName.lowercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DanmakuBubbleIcon(color = p.accent, size = 20.dp)
                Text("选择白名单应用", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("搜索本机应用名称...") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (loading) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text("正在读取已安装应用...", color = p.muted, fontSize = 12.sp)
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text("未找到匹配的应用", color = p.muted, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isSelected = app.packageName in enabledPackages
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) p.accent.copy(alpha = 0.16f) else Color.Transparent)
                                    .clickable { onToggleApp(app, !isSelected) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = app.appName,
                                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(p.accent.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(app.appName.take(1), color = p.ink, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = app.appName,
                                    color = p.ink,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = if (isSelected) "✓ 已选" else "添加",
                                    color = if (isSelected) p.accent else p.muted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}

@Composable
private fun TopHeader(destination: Destination, userName: String) {
    val p = LocalDeepFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandLogoMark(size = 40.dp)
            Column {
                Text("DEEP FOCUS · LIQUID GLASS", color = p.muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                Text(destination.label, color = p.ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(p.coral),
                contentAlignment = Alignment.Center,
            ) {
                Text(userName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(userName, color = p.ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BottomNavigation(
    selected: Destination,
    onDestinationSelected: (Destination) -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(p.surfaceGlassStrong)
            .border(1.dp, p.borderTopHighlight, RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Destination.values().forEach { destination ->
            val active = destination == selected
            val contentColor = if (active) p.accent else p.muted
            TextButton(
                onClick = { onDestinationSelected(destination) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (active) p.accent.copy(alpha = 0.14f) else Color.Transparent),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    when (destination) {
                        Destination.Aquarium -> NavIconAquarium(color = contentColor, size = 19.dp)
                        Destination.Scenes -> NavIconScenes(color = contentColor, size = 19.dp)
                        Destination.Nursery -> NavIconNursery(color = contentColor, size = 19.dp)
                        Destination.Settings -> NavIconSettings(color = contentColor, size = 19.dp)
                    }
                    Text(destination.label, color = contentColor, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun InfiniteAquariumBackdrop(@DrawableRes imageRes: Int) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val transition = rememberInfiniteTransition(label = "ken-burns-aquarium")
        val panX by transition.animateFloat(
            initialValue = -0.042f,
            targetValue = 0.042f,
            animationSpec = infiniteRepeatable(
                tween(32000, easing = EaseInOutSine),
                RepeatMode.Reverse,
            ),
            label = "ken-burns-pan-x",
        )
        val panY by transition.animateFloat(
            initialValue = -0.012f,
            targetValue = 0.012f,
            animationSpec = infiniteRepeatable(
                tween(22000, easing = EaseInOutSine),
                RepeatMode.Reverse,
            ),
            label = "ken-burns-pan-y",
        )
        val zoom by transition.animateFloat(
            initialValue = 1.12f,
            targetValue = 1.16f,
            animationSpec = infiniteRepeatable(
                tween(26000, easing = EaseInOutSine),
                RepeatMode.Reverse,
            ),
            label = "ken-burns-zoom",
        )
        Crossfade(
            targetState = imageRes,
            animationSpec = tween(700),
            label = "scene-crossfade",
        ) { targetRes ->
            Image(
                painter = painterResource(targetRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = panX * widthPx
                        translationY = panY * heightPx
                    },
            )
        }
    }
}

private fun formatCountdown(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = (safe / 60).toString().padStart(2, '0')
    val secs = (safe % 60).toString().padStart(2, '0')
    return "$mins:$secs"
}

@Composable
private fun AquariumRoomScreen(
    currentScene: AquariumScene,
    deployedFish: List<Fish>,
    todos: List<TodoItem>,
    focusMinutes: Int,
    sessionState: FocusSessionState,
    todayFocusMinutes: Int,
    weatherState: WeatherUiState,
    hudVisible: Boolean,
    danmakuMessages: List<DanmakuMessage>,
    onDanmakuExpired: (Long) -> Unit,
    onRefreshWeather: () -> Unit,
    onBackgroundTap: () -> Unit,
    onDestinationSelected: (Destination) -> Unit,
    onStartFocus: () -> Unit,
    onPauseFocus: () -> Unit,
    onResumeFocus: () -> Unit,
    onAbortFocus: () -> Unit,
    onFocusMinutesChange: (Int) -> Unit,
    onOpenCustomDuration: () -> Unit,
    onAddTodo: () -> Unit,
    onToggleTodo: (Int) -> Unit,
    onDeleteTodo: (Int) -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    val sceneMeta = AquariumCatalog.sceneVisualMeta[currentScene.name]
    val sceneTemp = sceneMeta?.temperature ?: "24°C"
    val isFocusing = sessionState.status == FocusStatus.RUNNING || sessionState.status == FocusStatus.PAUSED
    var touchState by remember { mutableStateOf<OceanTouchState?>(null) }
    var ripplePoint by remember { mutableStateOf<Pair<Offset, Long>?>(null) }
    val density = LocalDensity.current.density

    LaunchedEffect(ripplePoint) {
        if (ripplePoint != null) {
            delay(900)
            ripplePoint = null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val startPx = down.position
                    var isOceanInteract = false
                    val randomMode = if (Random.nextBoolean()) TouchInteractionMode.ATTRACT else TouchInteractionMode.SCATTER

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed) break

                        val posDp = Offset(change.position.x / density, change.position.y / density)
                        val elapsed = System.currentTimeMillis() - downTime
                        val moved = (change.position - startPx).getDistance()

                        if (!change.pressed) {
                            if (!isOceanInteract && elapsed < 260 && moved < 20f) {
                                ripplePoint = posDp to System.currentTimeMillis()
                                onBackgroundTap()
                            }
                            touchState = null
                            break
                        }

                        if (isOceanInteract || moved > 18f || elapsed > 160) {
                            isOceanInteract = true
                            touchState = OceanTouchState(
                                pointDp = posDp,
                                mode = randomMode,
                                id = downTime,
                            )
                        }
                    }
                }
            },
    ) {
        InfiniteAquariumBackdrop(currentScene.imageRes)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (p.isDark) {
                            listOf(Color(0x5507131D), Color.Transparent, Color(0x8807131D))
                        } else {
                            listOf(Color(0x33D8F3F8), Color.Transparent, Color(0x440D2C3A))
                        },
                    ),
                ),
        )
        AnimatedWaterLight()
        AnimatedBubbles(sceneWidth = maxWidth)

        TouchRippleCanvas(touchState = touchState, tapRipple = ripplePoint)

        deployedFish.forEachIndexed { index, fish ->
            AquariumCreature(
                fish = fish,
                index = index,
                sceneWidth = maxWidth,
                sceneHeight = maxHeight,
                touchState = touchState,
                onPoke = { pokePoint ->
                    ripplePoint = pokePoint to System.currentTimeMillis()
                },
            )
        }

        DanmakuOverlay(
            messages = danmakuMessages,
            sceneWidth = maxWidth,
            onExpired = onDanmakuExpired,
        )

        AnimatedVisibility(
            visible = touchState != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
        ) {
            val modeText = if (touchState?.mode == TouchInteractionMode.ATTRACT) {
                "指尖轻抚 · 鱼群正在聚拢跟随"
            } else {
                "指尖轻扰 · 鱼群正在四散游开"
            }
            Text(
                text = modeText,
                color = p.hudInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(p.hudGlass)
                    .border(1.dp, p.borderTopHighlight, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }

        if (!hudVisible && isFocusing) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(p.hudGlass)
                    .border(1.dp, p.borderTopHighlight, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LockShieldIcon(color = p.accent, size = 14.dp)
                Text(
                    formatCountdown(sessionState.remainingSeconds),
                    color = p.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (sessionState.status == FocusStatus.PAUSED) "已暂停" else "禅定锁定中",
                    color = p.hudInk,
                    fontSize = 11.sp,
                )
            }
        }

        AnimatedVisibility(visible = hudVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                LiquidGlassSurface(
                    isHud = true,
                    fillMaxWidth = false,
                    cornerRadius = 22.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BrandLogoMark(size = 42.dp)
                        Column {
                            Text("FLOW FOCUS · ${currentScene.name}", color = p.accent, fontSize = 10.sp, letterSpacing = 1.4.sp)
                            Text(currentScene.name, color = p.hudInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("已入住 ${deployedFish.size} 位住客 · $sceneTemp · 今日 ${todayFocusMinutes}m", color = p.hudMuted, fontSize = 11.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoomNavButtonWithIcon(label = "场景", icon = { NavIconScenes(p.hudInk, 15.dp) }) { onDestinationSelected(Destination.Scenes) }
                    RoomNavButtonWithIcon(label = "鱼苗", icon = { NavIconNursery(p.hudInk, 15.dp) }) { onDestinationSelected(Destination.Nursery) }
                    RoomNavButtonWithIcon(label = "设置", icon = { NavIconSettings(p.hudInk, 15.dp) }) { onDestinationSelected(Destination.Settings) }
                }
            }
        }

        AnimatedVisibility(
            visible = hudVisible,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Box(
                modifier = Modifier.padding(top = 74.dp, bottom = 16.dp, end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                RoomInfoPanel(
                    todos = todos,
                    weatherState = weatherState,
                    onRefreshWeather = onRefreshWeather,
                    onAddTodo = onAddTodo,
                    onToggleTodo = onToggleTodo,
                    onDeleteTodo = onDeleteTodo,
                )
            }
        }

        AnimatedVisibility(
            visible = hudVisible,
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            LiquidGlassSurface(
                modifier = Modifier.padding(24.dp).width(365.dp),
                isHud = true,
                fillMaxWidth = false,
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (isFocusing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(
                                    if (sessionState.status == FocusStatus.PAUSED) "PAUSED" else "DIVING NOW",
                                    color = if (sessionState.status == FocusStatus.PAUSED) p.gold else p.accent,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.2.sp,
                                )
                                Text(
                                    formatCountdown(sessionState.remainingSeconds),
                                    color = p.hudInk,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = if (sessionState.status == FocusStatus.PAUSED) onResumeFocus else onPauseFocus,
                                    colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.accentOn),
                                ) {
                                    Text(if (sessionState.status == FocusStatus.PAUSED) "继续" else "暂停", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onAbortFocus,
                                    border = BorderStroke(1.dp, p.coral),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = p.coral),
                                ) {
                                    Text("结束")
                                }
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("READY TO DIVE", color = p.accent, fontSize = 10.sp, letterSpacing = 1.2.sp)
                                Text("${focusMinutes.toString().padStart(2, '0')}:00", color = p.hudInk, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.weight(1f))
                            Button(onClick = onStartFocus, colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.accentOn)) {
                                Text("开始禅定", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        DurationSelector(
                            selectedMinutes = focusMinutes,
                            onSelect = onFocusMinutesChange,
                            onCustomClick = onOpenCustomDuration,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TouchRippleCanvas(
    touchState: OceanTouchState?,
    tapRipple: Pair<Offset, Long>?,
) {
    val activePoint = touchState?.pointDp ?: tapRipple?.first ?: return
    val p = LocalDeepFocusPalette.current
    val isScatter = touchState?.mode == TouchInteractionMode.SCATTER
    val isAttract = touchState?.mode == TouchInteractionMode.ATTRACT
    val transition = rememberInfiniteTransition(label = "touch-ripple")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(820, easing = LinearEasing), RepeatMode.Restart),
        label = "ripple-r",
    )
    val rippleColor = if (isScatter) p.coral else p.accent
    Canvas(Modifier.fillMaxSize()) {
        val cx = activePoint.x.dp.toPx()
        val cy = activePoint.y.dp.toPx()
        val maxR = (if (isScatter) 110 else 86).dp.toPx()

        for (ring in 0 until 3) {
            val t = ((phase + ring * 0.33f) % 1f)
            val rFactor = if (isAttract) (1f - t) else t
            val alpha = (1f - t) * 0.65f
            drawCircle(
                color = rippleColor.copy(alpha = alpha),
                radius = maxR * rFactor.coerceAtLeast(0.08f),
                center = Offset(cx, cy),
                style = Stroke(width = (2.6f - ring * 0.5f).dp.toPx()),
            )
        }

        for (i in 0 until 8) {
            val angle = (i * 45.0) * PI / 180.0
            val pFrac = if (isAttract) (1f - phase) else phase
            val dist = maxR * (0.22f + 0.78f * pFrac)
            val px = cx + (cos(angle) * dist).toFloat()
            val py = cy + (sin(angle) * dist).toFloat()
            drawCircle(
                color = rippleColor.copy(alpha = (1f - phase) * 0.75f),
                radius = 3.2.dp.toPx(),
                center = Offset(px, py),
            )
        }
    }
}

@Composable
private fun RoomNavButtonWithIcon(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(p.hudGlass)
            .border(1.dp, p.borderTopHighlight, RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.textButtonColors(contentColor = p.hudInk),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            icon()
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DurationSelector(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit,
    onCustomClick: () -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    val presets = listOf(25, 50, 90)
    val isCustom = selectedMinutes !in presets

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("时长", color = p.hudMuted, fontSize = 10.sp)
        presets.forEach { minutes ->
            val selected = selectedMinutes == minutes
            TextButton(
                onClick = { onSelect(minutes) },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) p.accent else p.hudGlass.copy(alpha = 0.55f)),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (selected) p.accentOn else p.hudInk,
                ),
            ) {
                Text("${minutes}m", fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
        TextButton(
            onClick = onCustomClick,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCustom) p.accent else p.hudGlass.copy(alpha = 0.55f)),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isCustom) p.accentOn else p.hudInk,
            ),
        ) {
            Text(
                if (isCustom) "${selectedMinutes}m" else "自定义",
                fontSize = 10.sp,
                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun RoomInfoPanel(
    todos: List<TodoItem>,
    weatherState: WeatherUiState,
    onRefreshWeather: () -> Unit,
    onAddTodo: () -> Unit,
    onToggleTodo: (Int) -> Unit,
    onDeleteTodo: (Int) -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    LiquidGlassSurface(modifier = Modifier.width(305.dp), isHud = true, fillMaxWidth = false) {
        Column(
            Modifier
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("TIDE LIST", color = p.hudMuted, fontSize = 10.sp, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("今日潮汐清单", color = p.hudInk, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onAddTodo) {
                    Text("＋ Todo", color = p.accent, fontSize = 10.sp)
                }
            }
            if (todos.isEmpty()) {
                Text("还没有待办，点击右上角添加一条。", color = p.hudMuted, fontSize = 11.sp)
            } else {
                todos.forEach { todo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTodo(todo.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (todo.completed) "✓" else "□",
                            color = if (todo.completed) p.accent else p.hudMuted,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            todo.title,
                            color = if (todo.completed) p.hudMuted else p.hudInk,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(onClick = { onDeleteTodo(todo.id) }) {
                            Text("删除", color = p.coral, fontSize = 9.sp)
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.accent.copy(alpha = 0.08f))
                    .clickable { onRefreshWeather() }
                    .padding(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LocationPinIcon(color = p.accent, size = 12.dp)
                    Text(
                        "${weatherState.cityName}${if (weatherState.locationSource.isNotEmpty()) " · ${weatherState.locationSource}" else ""}",
                        color = p.hudInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    WeatherGlyphIcon(weatherType = weatherState.weatherType, size = 22.dp)
                }
                Spacer(Modifier.height(4.dp))
                val tempText = weatherState.temperatureC?.let { "${it}°  ${weatherState.condition}" } ?: weatherState.condition
                Text(tempText, color = p.gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val detailParts = listOfNotNull(
                    weatherState.feelsLikeC?.let { "体感 ${it}°" },
                    weatherState.humidity?.let { "湿度 ${it}%" },
                    weatherState.windDesc.takeIf { it.isNotBlank() },
                )
                Text(
                    if (detailParts.isEmpty()) "点击刷新实时定位天气" else detailParts.joinToString(" · "),
                    color = p.hudMuted,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun AnimatedWaterLight() {
    val p = LocalDeepFocusPalette.current
    val transition = rememberInfiniteTransition(label = "water-light")
    val alpha by transition.animateFloat(
        initialValue = .08f,
        targetValue = .22f,
        animationSpec = infiniteRepeatable(tween(6500), RepeatMode.Reverse),
        label = "water-light-alpha",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(
                Brush.radialGradient(
                    colors = listOf(p.accent, Color.Transparent),
                    radius = 420f,
                ),
            ),
    )
}

@Composable
private fun AnimatedBubbles(sceneWidth: Dp) {
    val widthDp = sceneWidth.value.coerceAtLeast(320f)
    Box(Modifier.fillMaxSize()) {
        (0 until 10).forEach { index ->
            val transition = rememberInfiniteTransition(label = "bubble-$index")
            val rise by transition.animateFloat(
                initialValue = 12f,
                targetValue = -320f,
                animationSpec = infiniteRepeatable(
                    tween(7200 + index * 950, delayMillis = index * 520, easing = LinearEasing),
                    RepeatMode.Restart,
                ),
                label = "bubble-rise-$index",
            )
            val alpha by transition.animateFloat(
                initialValue = .08f,
                targetValue = .42f,
                animationSpec = infiniteRepeatable(
                    tween(2600 + index * 280, delayMillis = index * 200),
                    RepeatMode.Reverse,
                ),
                label = "bubble-alpha-$index",
            )
            val xPos = (widthDp * (0.06f + (index * 0.092f) % 0.88f)).dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = xPos, y = (rise - (index % 4) * 24).dp)
                    .size((3 + index % 4).dp)
                    .graphicsLayer { this.alpha = alpha }
                    .border(0.8.dp, Color(0x77CFFFF2), CircleShape),
            )
        }
    }
}

@Composable
private fun DanmakuOverlay(
    messages: List<DanmakuMessage>,
    sceneWidth: Dp,
    onExpired: (Long) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp),
    ) {
        messages.forEachIndexed { idx, msg ->
            key(msg.id) {
                DanmakuItem(
                    message = msg,
                    trackIndex = idx % 3,
                    sceneWidth = sceneWidth,
                    onExpired = { onExpired(msg.id) },
                )
            }
        }
    }
}

@Composable
private fun DanmakuItem(
    message: DanmakuMessage,
    trackIndex: Int,
    sceneWidth: Dp,
    onExpired: () -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    val startX = sceneWidth.value * 0.95f
    val endX = -460f
    val offsetX = remember { Animatable(startX) }
    val trackTop = (trackIndex * 46).dp

    LaunchedEffect(message.id) {
        offsetX.animateTo(
            targetValue = endX,
            animationSpec = tween(durationMillis = 8500, easing = LinearEasing),
        )
        onExpired()
    }

    val summary = listOf(message.title, message.text)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { "收到新消息" }

    Row(
        modifier = Modifier
            .offset(x = offsetX.value.dp, y = trackTop)
            .clip(RoundedCornerShape(50))
            .background(p.hudGlass)
            .border(1.dp, p.borderTopHighlight, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message.appName,
            color = p.accentOn,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(p.accent)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Text(
            text = summary,
            color = p.hudInk,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun normalizeAngle(angle: Float): Float {
    var a = angle
    val twoPi = (2.0 * PI).toFloat()
    val pi = PI.toFloat()
    while (a > pi) a -= twoPi
    while (a < -pi) a += twoPi
    return a
}

@Composable
private fun AquariumCreature(
    fish: Fish,
    index: Int,
    sceneWidth: Dp,
    sceneHeight: Dp,
    touchState: OceanTouchState?,
    onPoke: (Offset) -> Unit,
) {
    val style = AquariumCatalog.motionStyleOf(fish)
    val sizeDp = AquariumCatalog.displaySizeDp(fish).toFloat()
    val baseRight = AquariumCatalog.isDefaultFacingRight(fish.name)

    val minX = 28f
    val maxX = (sceneWidth.value - sizeDp - 28f).coerceAtLeast(minX + 40f)
    val minY = 64f
    val maxY = (sceneHeight.value - sizeDp - 72f).coerceAtLeast(minY + 40f)
    val bottomYBase = (sceneHeight.value - sizeDp - 18f - (index % 3) * 10f).coerceAtLeast(minY + 40f)

    val initX = (minX + (maxX - minX) * (((index * 37 + 19) % 100) / 100f)).coerceIn(minX, maxX)
    val initY = when (style) {
        AquariumCatalog.CreatureMotionStyle.BOTTOM_STILL,
        AquariumCatalog.CreatureMotionStyle.BOTTOM_CRAWL -> bottomYBase
        else -> (minY + (maxY - minY) * (((index * 53 + 11) % 100) / 100f)).coerceIn(minY, maxY)
    }

    var posX by remember(fish.name) { mutableStateOf(initX) }
    var posY by remember(fish.name) { mutableStateOf(initY) }
    var renderScaleX by remember(fish.name) { mutableStateOf(1f) }
    var renderScaleY by remember(fish.name) { mutableStateOf(1f) }
    var renderRotZ by remember(fish.name) { mutableStateOf(0f) }

    var activeTouch by remember(fish.name) { mutableStateOf<OceanTouchState?>(null) }
    var pokeBurstToken by remember(fish.name) { mutableStateOf(0) }
    LaunchedEffect(touchState) {
        activeTouch = touchState
    }

    LaunchedEffect(fish.name, style, maxX, maxY) {
        var px = posX.coerceIn(minX, maxX)
        var py = if (style == AquariumCatalog.CreatureMotionStyle.BOTTOM_STILL ||
            style == AquariumCatalog.CreatureMotionStyle.BOTTOM_CRAWL
        ) {
            bottomYBase
        } else {
            posY.coerceIn(minY, maxY)
        }
        var tx = (minX + Random.nextFloat() * (maxX - minX)).coerceIn(minX, maxX)
        var ty = (minY + Random.nextFloat() * (maxY - minY)).coerceIn(minY, maxY)
        var heading = atan2(ty - py, tx - px)
        var movingRight = cos(heading) >= 0f
        var flipX = if (baseRight == movingRight) 1f else -1f
        val baseCruise = 26f + (index % 3) * 5f
        var speed = baseCruise
        var phase = Random.nextFloat() * 6.28f
        var legTimer = 0f
        var pauseTimer = 0f
        var lastNanos = 0L
        var lastScatterTouchId = -1L
        var lastPokeSeen = 0

        while (true) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) 0.016f else ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = frameNanos
                phase += dt

                when (style) {
                    AquariumCatalog.CreatureMotionStyle.SWIM -> {
                        legTimer += dt
                        val touch = activeTouch
                        var targetSpeed = baseCruise
                        var accelRate = 1.4f
                        var turnRate = 1.7f

                        if (pokeBurstToken != lastPokeSeen) {
                            lastPokeSeen = pokeBurstToken
                            speed = 240f
                            val escapeAngle = heading + (if (Random.nextBoolean()) 0.65f else -0.65f)
                            tx = (px + cos(escapeAngle) * 240f).coerceIn(minX, maxX)
                            ty = (py + sin(escapeAngle) * 180f).coerceIn(minY, maxY)
                            heading = atan2(ty - py, tx - px)
                        }

                        if (touch != null) {
                            val cx = px + sizeDp * 0.5f
                            val cy = py + sizeDp * 0.5f
                            val dxTouch = cx - touch.pointDp.x
                            val dyTouch = cy - touch.pointDp.y
                            val dTouch = sqrt(dxTouch * dxTouch + dyTouch * dyTouch)

                            if (touch.mode == TouchInteractionMode.ATTRACT) {
                                val orbitAngle = phase * 1.35f + index * 1.15f
                                val orbitR = 44f + (index % 3) * 16f
                                tx = (touch.pointDp.x + cos(orbitAngle) * orbitR - sizeDp * 0.5f).coerceIn(minX, maxX)
                                ty = (touch.pointDp.y + sin(orbitAngle) * orbitR - sizeDp * 0.5f).coerceIn(minY, maxY)
                                targetSpeed = if (dTouch > 110f) 220f else 82f
                                accelRate = 6.5f
                                turnRate = 6.0f
                            } else {
                                if (dTouch < 460f) {
                                    val safeD = dTouch.coerceAtLeast(1f)
                                    val prox = (1f - (dTouch / 460f)).coerceIn(0.25f, 1f)
                                    val burstSpeed = 240f + 130f * prox
                                    tx = (cx + (dxTouch / safeD) * 320f - sizeDp * 0.5f).coerceIn(minX, maxX)
                                    ty = (cy + (dyTouch / safeD) * 240f - sizeDp * 0.5f).coerceIn(minY, maxY)
                                    if (lastScatterTouchId != touch.id) {
                                        lastScatterTouchId = touch.id
                                        speed = maxOf(speed, burstSpeed)
                                        heading = atan2(ty - py, tx - px)
                                    }
                                    targetSpeed = burstSpeed * 0.85f
                                    accelRate = 8.0f
                                    turnRate = 10.5f
                                }
                            }
                        } else {
                            val dx = tx - px
                            val dy = ty - py
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist < 42f || legTimer > 8f) {
                                tx = (minX + Random.nextFloat() * (maxX - minX)).coerceIn(minX, maxX)
                                ty = (minY + Random.nextFloat() * (maxY - minY)).coerceIn(minY, maxY)
                                legTimer = 0f
                            }
                        }

                        speed += (targetSpeed - speed) * (dt * accelRate).coerceAtMost(1f)
                        val desiredHeading = atan2(ty - py, tx - px)
                        val delta = normalizeAngle(desiredHeading - heading)
                        val maxStep = turnRate * dt
                        heading = normalizeAngle(heading + delta.coerceIn(-maxStep, maxStep))

                        px += cos(heading) * speed * dt
                        py += sin(heading) * speed * dt

                        if (px <= minX || px >= maxX || py <= minY || py >= maxY) {
                            px = px.coerceIn(minX, maxX)
                            py = py.coerceIn(minY, maxY)
                            val centerX = (minX + maxX) * 0.5f
                            val centerY = (minY + maxY) * 0.5f
                            tx = centerX + (Random.nextFloat() - 0.5f) * (maxX - minX) * 0.45f
                            ty = centerY + (Random.nextFloat() - 0.5f) * (maxY - minY) * 0.45f
                            heading = atan2(ty - py, tx - px)
                        }

                        val cosH = cos(heading)
                        if (abs(cosH) > 0.14f) {
                            movingRight = cosH > 0f
                        }
                        val targetFlip = if (baseRight == movingRight) 1f else -1f
                        flipX += (targetFlip - flipX) * (dt * 12f).coerceAtMost(1f)
                        val safeFlip = if (abs(flipX) < 0.18f) (if (flipX >= 0f) 0.18f else -0.18f) else flipX

                        val tiltDeg = (sin(heading) * 16f).coerceIn(-16f, 16f) * (if (movingRight) 1f else -1f)
                        val speedFactor = (speed / 28f).coerceIn(1f, 3.6f)
                        val pulse = 1f + sin(phase * 3.2f * speedFactor) * (0.022f * speedFactor.coerceAtMost(2.2f))

                        posX = px
                        posY = py + sin(phase * 1.7f) * 2.0f
                        renderScaleX = safeFlip * (1f + (speedFactor - 1f) * 0.03f) * pulse
                        renderScaleY = pulse
                        renderRotZ = tiltDeg + sin(phase * 2.2f * speedFactor) * 1.3f
                    }

                    AquariumCatalog.CreatureMotionStyle.JELLYFISH -> {
                        legTimer += dt
                        val dx = tx - px
                        val dy = ty - py
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist < 28f || legTimer > 10f) {
                            tx = (minX + Random.nextFloat() * (maxX - minX)).coerceIn(minX, maxX)
                            ty = (minY + Random.nextFloat() * (maxY - minY)).coerceIn(minY, maxY)
                            legTimer = 0f
                        }
                        val pulseCycle = sin(phase * 1.85f)
                        val lift = if (pulseCycle > 0f) pulseCycle * 11f else pulseCycle * 4f
                        val safeDist = dist.coerceAtLeast(1f)
                        px = (px + (dx / safeDist) * 11f * dt).coerceIn(minX, maxX)
                        py = (py + ((dy / safeDist) * 10f - lift * 0.35f) * dt).coerceIn(minY, maxY)

                        posX = px
                        posY = py
                        renderScaleX = 1f - pulseCycle * 0.065f
                        renderScaleY = 1f + pulseCycle * 0.055f
                        renderRotZ = sin(phase * 0.9f) * 5.5f
                    }

                    AquariumCatalog.CreatureMotionStyle.SEAHORSE -> {
                        legTimer += dt
                        val dx = tx - px
                        val dy = ty - py
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist < 24f || legTimer > 9f) {
                            tx = (minX + Random.nextFloat() * (maxX - minX)).coerceIn(minX, maxX)
                            ty = (minY + Random.nextFloat() * (maxY - minY)).coerceIn(minY, maxY)
                            legTimer = 0f
                        }
                        val safeDist = dist.coerceAtLeast(1f)
                        val vx = (dx / safeDist) * 12f
                        val vy = (dy / safeDist) * 9f
                        px = (px + vx * dt).coerceIn(minX, maxX)
                        py = (py + vy * dt).coerceIn(minY, maxY)
                        if (abs(vx) > 1.5f) movingRight = vx > 0f
                        val targetFlip = if (baseRight == movingRight) 1f else -1f
                        flipX += (targetFlip - flipX) * (dt * 6f).coerceAtMost(1f)
                        val safeFlip = if (abs(flipX) < 0.18f) (if (flipX >= 0f) 0.18f else -0.18f) else flipX

                        posX = px
                        posY = py + sin(phase * 1.5f) * 3.5f
                        renderScaleX = safeFlip
                        renderScaleY = 1f + sin(phase * 2.0f) * 0.02f
                        renderRotZ = sin(phase * 1.3f) * 6f
                    }

                    AquariumCatalog.CreatureMotionStyle.BOTTOM_STILL -> {
                        val breathe = 1f + sin(phase * 1.4f) * 0.016f
                        posX = px
                        posY = bottomYBase
                        renderScaleX = breathe
                        renderScaleY = breathe
                        renderRotZ = sin(phase * 0.8f) * 1.5f
                    }

                    AquariumCatalog.CreatureMotionStyle.BOTTOM_CRAWL -> {
                        if (pauseTimer > 0f) {
                            pauseTimer -= dt
                        } else {
                            val dx = tx - px
                            if (abs(dx) < 16f) {
                                tx = (minX + Random.nextFloat() * (maxX - minX)).coerceIn(minX, maxX)
                                pauseTimer = 2.5f + Random.nextFloat() * 3.0f
                            } else {
                                val stepDir = if (dx > 0f) 1f else -1f
                                px = (px + stepDir * 11f * dt).coerceIn(minX, maxX)
                                movingRight = stepDir > 0f
                            }
                        }
                        val targetFlip = if (baseRight == movingRight) 1f else -1f
                        flipX += (targetFlip - flipX) * (dt * 6f).coerceAtMost(1f)
                        val safeFlip = if (abs(flipX) < 0.18f) (if (flipX >= 0f) 0.18f else -0.18f) else flipX
                        val breathe = 1f + sin(phase * 1.8f) * 0.018f
                        posX = px
                        posY = bottomYBase + cos(phase * 2.2f) * 0.8f
                        renderScaleX = safeFlip * breathe
                        renderScaleY = breathe
                        renderRotZ = if (pauseTimer > 0f) 0f else sin(phase * 4.0f) * 2.2f
                    }
                }
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .offset(posX.dp, posY.dp)
            .graphicsLayer {
                scaleX = renderScaleX
                scaleY = renderScaleY
                rotationZ = renderRotZ
            }
            .size(sizeDp.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                pokeBurstToken += 1
                onPoke(Offset(posX + sizeDp * 0.5f, posY + sizeDp * 0.5f))
            },
    ) {
        Image(
            painter = painterResource(fish.imageRes),
            contentDescription = fish.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ScenesScreen(
    selectedSceneName: String,
    onSelectScene: (AquariumScene) -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    val scenes = AquariumCatalog.scenes
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(scenes) { scene ->
            val isSelected = scene.name == selectedSceneName
            val meta = AquariumCatalog.sceneVisualMeta[scene.name]
            LiquidGlassSurface(
                modifier = Modifier
                    .clickable(enabled = scene.unlocked) { onSelectScene(scene) }
                    .let { if (isSelected) it.border(1.5.dp, p.accent, RoundedCornerShape(24.dp)) else it },
            ) {
                Column {
                    Image(
                        painterResource(scene.imageRes),
                        scene.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                    )
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(scene.name, color = p.ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            meta?.let {
                                Text(it.temperature, color = p.gold, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Text(
                            if (isSelected) "✓ 使用中" else if (scene.unlocked) "✓ 已开放 · 点击切换" else "未解锁",
                            color = if (isSelected) p.accent else p.muted,
                            fontSize = 10.sp,
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(scene.description, color = p.muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private val newResidentNames = setOf(
    "魔鬼鱼", "幼年鲸鲨", "花斑石斑鱼", "海天使鱼", "蓝纹神仙鱼", "斑马倒吊鱼",
    "绿海龟", "章鱼", "海星", "灯塔水母",
    "大砗磲贝", "珍珠贝", "鹦鹉螺", "火焰贝",
)

@Composable
private fun NurseryScreen(
    deployedFishNames: Set<String>,
    onToggleDeployFish: (Fish) -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    var category by remember { mutableStateOf("全部") }
    val residents = AquariumCatalog.residents
    val visibleResidents = when (category) {
        "全部" -> residents
        "新增精选" -> residents.filter { it.name in newResidentNames }
        else -> residents.filter { it.category == category }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("全部", "新增精选", "鱼类", "贝类", "其他").forEach { item ->
                TextButton(
                    onClick = { category = item },
                    colors = ButtonDefaults.textButtonColors(contentColor = if (category == item) p.accentOn else p.muted),
                    modifier = Modifier.background(if (category == item) p.accent else Color.Transparent, RoundedCornerShape(50)),
                ) { Text(item, fontSize = 11.sp) }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "已入住 ${deployedFishNames.size} / 共 ${residents.size} 位 · 全部免费开放",
                color = p.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        LazyVerticalGrid(
            modifier = Modifier.weight(1f),
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(visibleResidents) { item ->
                val isDeployed = item.name in deployedFishNames
                LiquidGlassSurface(
                    modifier = if (isDeployed) Modifier.border(1.2.dp, p.accent, RoundedCornerShape(24.dp)) else Modifier,
                ) {
                    Column {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                                .background(if (p.isDark) Color(0xFF102E3D) else Color(0xFFD5ECF2)),
                        ) {
                            Image(painterResource(item.imageRes), item.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(14.dp))
                        }
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, color = p.ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                if (item.name in newResidentNames) {
                                    Text(
                                        "NEW",
                                        color = p.accentOn,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(p.accent)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text("${item.category} · ${item.temperament} · ${item.water}", color = p.muted, fontSize = 10.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(item.description, color = p.muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { onToggleDeployFish(item) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDeployed) p.coral.copy(alpha = 0.18f) else p.accent,
                                    contentColor = if (isDeployed) p.coral else p.accentOn,
                                ),
                            ) {
                                Text(
                                    if (isDeployed) "移出水族箱" else "投放进水箱",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    userName: String,
    themeMode: ThemeMode,
    focusMinutes: Int,
    isLocked: Boolean,
    listenerEnabled: Boolean,
    whitelistApps: List<WhitelistApp>,
    focusHistory: List<FocusRecord>,
    focusStats: FocusStats,
    onEditNickname: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onFocusMinutesChange: (Int) -> Unit,
    onOpenCustomDuration: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onToggleWhitelist: (WhitelistApp) -> Unit,
    onOpenAppPicker: () -> Unit,
    onSendTestDanmaku: () -> Unit,
    onStartLock: () -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    val timeFormatter = remember { java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()) }
    var historyFilter by remember { mutableStateOf("全部") }

    val filteredHistory = remember(focusHistory, historyFilter) {
        when (historyFilter) {
            "成功" -> focusHistory.filter { it.status == FocusStatus.COMPLETED }
            "失败" -> focusHistory.filter { it.status == FocusStatus.ABORTED }
            else -> focusHistory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. 个人档案与外观主题（跟随系统 / 白天 / 暗黑）
        LiquidGlassSurface {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandLogoMark(size = 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("PROFILE & LIQUID GLASS THEME", color = p.muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
                        Text(userName, color = p.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onEditNickname,
                        border = BorderStroke(1.dp, p.accent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = p.accent),
                    ) { Text("修改昵称", fontSize = 11.sp) }
                }
                Text("主题配色", color = p.ink, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(p.accent.copy(alpha = 0.08f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        ThemeMode.SYSTEM to "跟随系统",
                        ThemeMode.LIGHT to "白天",
                        ThemeMode.DARK to "暗黑",
                    ).forEach { (mode, label) ->
                        val active = themeMode == mode
                        TextButton(
                            onClick = { onThemeModeChange(mode) },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) p.accent else Color.Transparent),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (active) p.accentOn else p.muted,
                            ),
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }

        // 2. 禅定设置与锁定策略
        LiquidGlassSurface {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LockShieldIcon(color = p.accent, size = 18.dp)
                    Text("深海专注锁定规则", color = p.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text("默认禅定时长", color = p.ink, fontWeight = FontWeight.Medium)
                Text("${focusMinutes} 分钟", color = p.muted, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                DurationSelector(
                    selectedMinutes = focusMinutes,
                    onSelect = onFocusMinutesChange,
                    onCustomClick = onOpenCustomDuration,
                )
                SettingRow("锁定失败机制", "退出锁定或离开应用将直接结束本次禅定并记为失败")
                SettingRow("当前锁定状态", if (isLocked) "已进入系统锁定任务模式" else "未锁定（开始禅定时自动触发）")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onStartLock,
                    border = BorderStroke(1.dp, p.accent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = p.accent),
                ) {
                    Text("开始锁定态预览")
                }
            }
        }

        // 3. 禅定统计与记录
        LiquidGlassSurface {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatsChartIcon(color = p.gold, size = 18.dp)
                    Text("禅定统计与记录", color = p.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMiniTile(Modifier.weight(1f), "总专注", "${focusStats.totalMinutes}m", p.ink)
                    StatMiniTile(Modifier.weight(1f), "今日专注", "${focusStats.todayMinutes}m", p.accent)
                    StatMiniTile(Modifier.weight(1f), "完成率", "${focusStats.completionRate}%", p.gold)
                    StatMiniTile(Modifier.weight(1f), "成功 / 失败", "${focusStats.successCount} / ${focusStats.failCount}", p.coral)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("全部", "成功", "失败").forEach { tab ->
                        val active = historyFilter == tab
                        Text(
                            text = tab,
                            color = if (active) p.accentOn else p.muted,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) p.accent else p.accent.copy(alpha = 0.1f))
                                .clickable { historyFilter = tab }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
                if (filteredHistory.isEmpty()) {
                    Text("暂无符合条件的禅定记录。", color = p.muted, fontSize = 12.sp)
                } else {
                    filteredHistory.take(8).forEach { record ->
                        val isSuccess = record.status == FocusStatus.COMPLETED
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(p.accent.copy(alpha = 0.06f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(record.sceneName, color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "${timeFormatter.format(java.util.Date(record.startedAt))} · 计划 ${record.plannedMinutes}m / 实际 ${record.actualSeconds / 60}m",
                                    color = p.muted,
                                    fontSize = 10.sp,
                                )
                            }
                            Text(
                                text = if (isSuccess) "成功 ✓" else "失败",
                                color = if (isSuccess) p.accent else p.coral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        // 4. 通知白名单与弹幕
        LiquidGlassSurface {
            Column(Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DanmakuBubbleIcon(color = p.accent, size = 18.dp)
                            Text("通知白名单与弹幕", color = p.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (listenerEnabled) "通知读取权限：已开启" else "通知读取权限：未开启（开启后自动转为弹幕）",
                            color = if (listenerEnabled) p.accent else p.gold,
                            fontSize = 11.sp,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenAppPicker,
                            border = BorderStroke(1.dp, p.accent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = p.accent),
                        ) { Text("选择白名单 App", fontSize = 11.sp) }
                        OutlinedButton(
                            onClick = onSendTestDanmaku,
                            border = BorderStroke(1.dp, p.gold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = p.gold),
                        ) { Text("测试弹幕", fontSize = 11.sp) }
                    }
                }
                if (!listenerEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onOpenNotificationSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.accentOn),
                    ) { Text("去开启通知读取权限", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))
                whitelistApps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleWhitelist(app) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (app.enabled) "✓" else "□", color = if (app.enabled) p.accent else p.muted, fontSize = 14.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(app.appName, color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(if (app.enabled) "转为弹幕" else "已静音", color = if (app.enabled) p.accent else p.muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniTile(
    modifier: Modifier,
    label: String,
    value: String,
    valueColor: Color,
) {
    val p = LocalDeepFocusPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(p.accent.copy(alpha = 0.08f))
            .border(1.dp, p.borderBottom, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(label, color = p.muted, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingRow(title: String, detail: String) {
    val p = LocalDeepFocusPalette.current
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(title, color = p.ink, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Text(detail, color = p.muted, fontSize = 11.sp)
    }
}

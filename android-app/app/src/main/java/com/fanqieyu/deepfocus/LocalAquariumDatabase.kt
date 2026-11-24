package com.fanqieyu.deepfocus

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

class LocalAquariumDatabase(context: Context) :
    SQLiteOpenHelper(context, "deep_focus.db", null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createBaseTables(db)
        createFocusTables(db)
        seedSingletonRows(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            if (!hasColumn(db, "deployed_fish", "image_key")) {
                db.execSQL("ALTER TABLE deployed_fish ADD COLUMN image_key TEXT NOT NULL DEFAULT ''")
            }
            db.execSQL("UPDATE deployed_fish SET image_key = name WHERE image_key = ''")
            createFocusTables(db)
            seedSingletonRows(db)
        }
        if (oldVersion < 3) {
            if (!hasColumn(db, "app_state", "user_name")) {
                db.execSQL("ALTER TABLE app_state ADD COLUMN user_name TEXT NOT NULL DEFAULT ''")
            }
            if (!hasColumn(db, "app_state", "theme_mode")) {
                db.execSQL("ALTER TABLE app_state ADD COLUMN theme_mode TEXT NOT NULL DEFAULT 'SYSTEM'")
            }
        }
    }

    fun loadDeployedFish(): List<Fish> {
        val result = mutableListOf<Fish>()
        readableDatabase.query(
            "deployed_fish",
            null,
            null,
            null,
            null,
            null,
            "rowid ASC",
        ).use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            val description = cursor.getColumnIndexOrThrow("description")
            val temperament = cursor.getColumnIndexOrThrow("temperament")
            val rarity = cursor.getColumnIndexOrThrow("rarity")
            val water = cursor.getColumnIndexOrThrow("water")
            val bond = cursor.getColumnIndexOrThrow("bond")
            val imageRes = cursor.getColumnIndexOrThrow("image_res")
            val category = cursor.getColumnIndexOrThrow("category")
            while (cursor.moveToNext()) {
                val fishName = cursor.getString(name)
                result += Fish(
                    name = fishName,
                    description = cursor.getString(description),
                    temperament = cursor.getString(temperament),
                    rarity = cursor.getString(rarity),
                    water = cursor.getString(water),
                    bond = cursor.getInt(bond),
                    imageRes = AquariumCatalog.findFishByName(fishName)?.imageRes ?: cursor.getInt(imageRes),
                    category = cursor.getString(category),
                )
            }
        }
        return result
    }

    fun replaceDeployedFish(fish: List<Fish>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("deployed_fish", null, null)
            fish.forEach { item ->
                db.insertOrThrow(
                    "deployed_fish",
                    null,
                    ContentValues().apply {
                        put("name", item.name)
                        put("description", item.description)
                        put("temperament", item.temperament)
                        put("rarity", item.rarity)
                        put("water", item.water)
                        put("bond", item.bond)
                        put("image_res", item.imageRes)
                        put("image_key", item.name)
                        put("category", item.category)
                    },
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadTodosOrNull(): List<TodoItem>? {
        val rows = mutableListOf<TodoItem>()
        var hasState = false
        readableDatabase.query(
            "todos",
            null,
            null,
            null,
            null,
            null,
            "id ASC",
        ).use { cursor ->
            val id = cursor.getColumnIndexOrThrow("id")
            val title = cursor.getColumnIndexOrThrow("title")
            val completed = cursor.getColumnIndexOrThrow("completed")
            while (cursor.moveToNext()) {
                hasState = true
                if (cursor.getInt(id) >= 0) {
                    rows += TodoItem(
                        id = cursor.getInt(id),
                        title = cursor.getString(title),
                        completed = cursor.getInt(completed) == 1,
                    )
                }
            }
        }
        return if (hasState) rows else null
    }

    fun replaceTodos(todos: List<TodoItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("todos", null, null)
            if (todos.isEmpty()) {
                db.insertOrThrow(
                    "todos",
                    null,
                    ContentValues().apply {
                        put("id", -1)
                        put("title", "")
                        put("completed", 1)
                    },
                )
            } else {
                todos.forEach { item ->
                    db.insertOrThrow(
                        "todos",
                        null,
                        ContentValues().apply {
                            put("id", item.id)
                            put("title", item.title)
                            put("completed", if (item.completed) 1 else 0)
                        },
                    )
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadFocusMinutes(): Int {
        readableDatabase.query(
            "focus_settings",
            arrayOf("minutes"),
            "id = 1",
            null,
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getInt(0)
        }
        return 25
    }

    fun saveFocusMinutes(minutes: Int) {
        writableDatabase.insertWithOnConflict(
            "focus_settings",
            null,
            ContentValues().apply {
                put("id", 1)
                put("minutes", minutes)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun loadCoins(): Int = readableDatabase.query(
        "wallet",
        arrayOf("coins"),
        "id = 1",
        null,
        null,
        null,
        null,
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getInt(0) else INITIAL_COINS
    }

    fun setCoins(coins: Int) {
        writableDatabase.insertWithOnConflict(
            "wallet",
            null,
            ContentValues().apply {
                put("id", 1)
                put("coins", coins.coerceAtLeast(0))
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun spendCoins(cost: Int): Boolean {
        if (cost <= 0) return false
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val cursor = db.rawQuery(
                "SELECT coins FROM wallet WHERE id = 1 AND coins >= ?",
                arrayOf(cost.toString()),
            )
            val canSpend = cursor.use { it.moveToFirst() }
            if (canSpend) {
                db.execSQL("UPDATE wallet SET coins = coins - ? WHERE id = 1", arrayOf(cost))
            }
            if (canSpend) db.setTransactionSuccessful()
            canSpend
        } finally {
            db.endTransaction()
        }
    }

    fun loadAppState(): AppState {
        val today = LocalDate.now().toString()
        val values = readableDatabase.query(
            "app_state",
            arrayOf("selected_scene_name", "today_focus_minutes", "today_date", "user_name", "theme_mode"),
            "id = 1",
            null,
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                AppState(
                    selectedSceneName = cursor.getString(0),
                    todayFocusMinutes = cursor.getInt(1),
                    todayDate = cursor.getString(2),
                    userName = cursor.getString(3).orEmpty(),
                    themeMode = runCatching { ThemeMode.valueOf(cursor.getString(4)) }.getOrDefault(ThemeMode.SYSTEM),
                )
            } else {
                AppState(todayDate = today)
            }
        }
        if (values.todayDate == today) return values
        val reset = values.copy(todayFocusMinutes = 0, todayDate = today)
        saveAppState(reset)
        return reset
    }

    fun saveAppState(state: AppState) {
        writableDatabase.insertWithOnConflict(
            "app_state",
            null,
            ContentValues().apply {
                put("id", 1)
                put("selected_scene_name", state.selectedSceneName)
                put("today_focus_minutes", state.todayFocusMinutes.coerceAtLeast(0))
                put("today_date", state.todayDate)
                put("user_name", state.userName)
                put("theme_mode", state.themeMode.name)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun loadActiveSession(): FocusSessionState? {
        return readableDatabase.query(
            "active_session",
            null,
            null,
            null,
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            FocusSessionState(
                status = cursor.getString(cursor.getColumnIndexOrThrow("state"))
                    .toFocusStatusOrNull() ?: FocusStatus.PAUSED,
                sessionId = cursor.getLong(cursor.getColumnIndexOrThrow("session_id")),
                sceneName = cursor.getString(cursor.getColumnIndexOrThrow("scene_name")),
                plannedMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("planned_minutes")),
                remainingSeconds = cursor.getInt(cursor.getColumnIndexOrThrow("remaining_seconds")),
                startedAt = cursor.getLong(cursor.getColumnIndexOrThrow("started_at")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
            )
        }
    }

    fun saveActiveSession(state: FocusSessionState) {
        val sessionId = requireNotNull(state.sessionId) { "An active session needs an id" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("active_session", null, null)
            db.insertOrThrow(
                "active_session",
                null,
                ContentValues().apply {
                    put("session_id", sessionId)
                    put("scene_name", state.sceneName)
                    put("planned_minutes", state.plannedMinutes)
                    put("remaining_seconds", state.remainingSeconds.coerceAtLeast(0))
                    put("state", state.status.name)
                    put("started_at", requireNotNull(state.startedAt))
                    put("updated_at", requireNotNull(state.updatedAt))
                },
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearActiveSession() {
        writableDatabase.delete("active_session", null, null)
    }

    fun persistFinishedSession(record: FocusRecord, coins: Int, appState: AppState) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            insertFocusRecord(record)
            setCoins(coins)
            saveAppState(appState)
            clearActiveSession()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertFocusRecord(record: FocusRecord) {
        writableDatabase.insertOrThrow(
            "focus_sessions",
            null,
            ContentValues().apply {
                put("id", record.id)
                put("scene_name", record.sceneName)
                put("planned_minutes", record.plannedMinutes)
                put("actual_seconds", record.actualSeconds)
                put("reward_coins", record.rewardCoins)
                put("status", record.status.name)
                put("started_at", record.startedAt)
                put("ended_at", record.endedAt)
            },
        )
    }

    fun loadFocusRecords(): List<FocusRecord> {
        val result = mutableListOf<FocusRecord>()
        readableDatabase.query(
            "focus_sessions",
            null,
            null,
            null,
            null,
            null,
            "started_at DESC, id DESC",
        ).use { cursor ->
            val id = cursor.getColumnIndexOrThrow("id")
            val sceneName = cursor.getColumnIndexOrThrow("scene_name")
            val plannedMinutes = cursor.getColumnIndexOrThrow("planned_minutes")
            val actualSeconds = cursor.getColumnIndexOrThrow("actual_seconds")
            val rewardCoins = cursor.getColumnIndexOrThrow("reward_coins")
            val status = cursor.getColumnIndexOrThrow("status")
            val startedAt = cursor.getColumnIndexOrThrow("started_at")
            val endedAt = cursor.getColumnIndexOrThrow("ended_at")
            while (cursor.moveToNext()) {
                result += FocusRecord(
                    id = cursor.getLong(id),
                    sceneName = cursor.getString(sceneName),
                    plannedMinutes = cursor.getInt(plannedMinutes),
                    actualSeconds = cursor.getInt(actualSeconds),
                    rewardCoins = cursor.getInt(rewardCoins),
                    status = cursor.getString(status).toFocusStatusOrNull() ?: FocusStatus.ABORTED,
                    startedAt = cursor.getLong(startedAt),
                    endedAt = cursor.getLong(endedAt),
                )
            }
        }
        return result
    }

    fun loadWhitelistApps(): List<WhitelistApp> {
        val result = mutableListOf<WhitelistApp>()
        readableDatabase.query(
            "notification_whitelist",
            null,
            null,
            null,
            null,
            null,
            "app_name COLLATE NOCASE ASC",
        ).use { cursor ->
            val packageName = cursor.getColumnIndexOrThrow("package_name")
            val appName = cursor.getColumnIndexOrThrow("app_name")
            val enabled = cursor.getColumnIndexOrThrow("enabled")
            while (cursor.moveToNext()) {
                result += WhitelistApp(
                    packageName = cursor.getString(packageName),
                    appName = cursor.getString(appName),
                    enabled = cursor.getInt(enabled) == 1,
                )
            }
        }
        return result
    }

    fun saveWhitelistApp(app: WhitelistApp) {
        writableDatabase.insertWithOnConflict(
            "notification_whitelist",
            null,
            ContentValues().apply {
                put("package_name", app.packageName)
                put("app_name", app.appName)
                put("enabled", if (app.enabled) 1 else 0)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun removeWhitelistApp(packageName: String) {
        writableDatabase.delete("notification_whitelist", "package_name = ?", arrayOf(packageName))
    }

    private fun createBaseTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE deployed_fish (
                name TEXT PRIMARY KEY NOT NULL,
                description TEXT NOT NULL,
                temperament TEXT NOT NULL,
                rarity TEXT NOT NULL,
                water TEXT NOT NULL,
                bond INTEGER NOT NULL,
                image_res INTEGER NOT NULL,
                image_key TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE todos (
                id INTEGER PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                completed INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE focus_settings (
                id INTEGER PRIMARY KEY NOT NULL,
                minutes INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    private fun createFocusTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS wallet (
                id INTEGER PRIMARY KEY NOT NULL,
                coins INTEGER NOT NULL DEFAULT 1280
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_state (
                id INTEGER PRIMARY KEY NOT NULL,
                selected_scene_name TEXT NOT NULL DEFAULT '月光海沟',
                today_focus_minutes INTEGER NOT NULL DEFAULT 0,
                today_date TEXT NOT NULL DEFAULT '',
                user_name TEXT NOT NULL DEFAULT '',
                theme_mode TEXT NOT NULL DEFAULT 'SYSTEM'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS focus_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                scene_name TEXT NOT NULL,
                planned_minutes INTEGER NOT NULL,
                actual_seconds INTEGER NOT NULL,
                reward_coins INTEGER NOT NULL,
                status TEXT NOT NULL CHECK(status IN ('COMPLETED', 'ABORTED')),
                started_at INTEGER NOT NULL,
                ended_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS active_session (
                session_id INTEGER PRIMARY KEY NOT NULL,
                scene_name TEXT NOT NULL,
                planned_minutes INTEGER NOT NULL,
                remaining_seconds INTEGER NOT NULL,
                state TEXT NOT NULL,
                started_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS notification_whitelist (
                package_name TEXT PRIMARY KEY NOT NULL,
                app_name TEXT NOT NULL,
                enabled INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent(),
        )
    }

    private fun seedSingletonRows(db: SQLiteDatabase) {
        db.execSQL("INSERT OR IGNORE INTO wallet (id, coins) VALUES (1, 1280)")
        db.execSQL(
            "INSERT OR IGNORE INTO app_state (id, selected_scene_name, today_focus_minutes, today_date, user_name, theme_mode) " +
                "VALUES (1, '月光海沟', 0, '${LocalDate.now()}', '', 'SYSTEM')",
        )
    }

    private fun hasColumn(db: SQLiteDatabase, table: String, column: String): Boolean {
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return true
            }
        }
        return false
    }

    private fun String.toFocusStatusOrNull(): FocusStatus? =
        runCatching { FocusStatus.valueOf(this) }.getOrNull()

    companion object {
        const val DATABASE_VERSION = 3
        const val INITIAL_COINS = 1280
    }
}

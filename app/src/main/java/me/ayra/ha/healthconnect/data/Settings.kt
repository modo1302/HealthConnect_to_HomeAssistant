package me.ayra.ha.healthconnect.data

import android.content.Context
import me.ayra.ha.healthconnect.utils.DataStore.getKey
import me.ayra.ha.healthconnect.utils.DataStore.removeKey
import me.ayra.ha.healthconnect.utils.DataStore.setKey
import me.ayra.ha.healthconnect.utils.DataStore.toJson
import me.ayra.ha.healthconnect.utils.DataStore.tryParseJson

const val DATA = "DATA"
const val MIN_SYNC_DAYS = 1L
const val MAX_SYNC_DAYS = 30L
const val DEFAULT_SYNC_DAYS = 7L
const val MIN_STEPS_GOAL = 1_000
const val MAX_STEPS_GOAL = 100_000
const val DEFAULT_STEPS_GOAL = 5_000

object Settings {
    data class SyncError(
        val timestamp: Long,
        val message: String,
    )

    fun Context.getSettings(path: String): String? = getKey<String>(DATA, path)

    fun Context.setSettings(
        path: String,
        value: String,
    ) = setKey(DATA, path, value)

    fun Context.getSettings(
        path: String,
        default: Boolean,
    ): Boolean? = getKey<Boolean>(DATA, path, default)

    fun Context.setSettings(
        path: String,
        value: Boolean,
    ) = setKey(DATA, path, value)

    fun Context.getForegroundServiceEnabled(): Boolean = getSettings("foregroundService", false) ?: false

    fun Context.setForegroundServiceEnabled(value: Boolean) {
        setSettings("foregroundService", value)
    }

    fun Context.isNotificationPromptDisabled(): Boolean = getSettings("notificationPermissionPromptDisabled", false) ?: false

    fun Context.setNotificationPromptDisabled(value: Boolean) {
        setSettings("notificationPermissionPromptDisabled", value)
    }

    fun Context.getIgnoredUpdateVersion(): Int? = getKey<Int>(DATA, "ignoredUpdateVersion")

    fun Context.setIgnoredUpdateVersion(value: Int) {
        setKey(DATA, "ignoredUpdateVersion", value)
    }

    fun Context.getLastSync(): Long? = getKey<Long>(DATA, "lastSync")

    fun Context.setLastSync(value: Long) = setKey(DATA, "lastSync", value)

    fun Context.getLastError(): SyncError? = tryParseJson(getKey<String>(DATA, "lastError"))

    fun Context.setLastError(value: SyncError) = setKey(DATA, "lastError", value.toJson())

    fun Context.removeLastError() {
        removeKey(DATA, "lastError")
    }

    fun Context.getAutoSync(): Boolean? = getKey<Boolean>(DATA, "AutoSync", true)

    fun Context.setAutoSync(value: Boolean) = setKey(DATA, "AutoSync", value)

    fun Context.getSyncDays(): Long {
        val storedValue = getSettings("syncDays")?.toLongOrNull()
        return storedValue?.coerceIn(MIN_SYNC_DAYS, MAX_SYNC_DAYS) ?: DEFAULT_SYNC_DAYS
    }

    fun Context.setSyncDays(value: Long) {
        val sanitizedValue = value.coerceIn(MIN_SYNC_DAYS, MAX_SYNC_DAYS)
        setSettings("syncDays", sanitizedValue.toString())
    }

    fun Context.getStepsGoal(): Int {
        val storedValue = getSettings("stepsGoal")?.toIntOrNull()
        return storedValue?.coerceIn(MIN_STEPS_GOAL, MAX_STEPS_GOAL) ?: DEFAULT_STEPS_GOAL
    }

    fun Context.setStepsGoal(value: Int) {
        val sanitizedValue = value.coerceIn(MIN_STEPS_GOAL, MAX_STEPS_GOAL)
        setSettings("stepsGoal", sanitizedValue.toString())
    }
}

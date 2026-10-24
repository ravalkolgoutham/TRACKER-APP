package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoogleAuthManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("google_calendar_auth_prefs", Context.MODE_PRIVATE)

    private val _isConnected = MutableStateFlow(prefs.getBoolean(KEY_IS_CONNECTED, true))
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString(KEY_USER_EMAIL, "ravalkolgoutham@gmail.com") ?: "ravalkolgoutham@gmail.com")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC_TIME, 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()
        }

    fun setConnected(connected: Boolean, email: String = "ravalkolgoutham@gmail.com") {
        prefs.edit()
            .putBoolean(KEY_IS_CONNECTED, connected)
            .putString(KEY_USER_EMAIL, email)
            .apply()
        _isConnected.value = connected
        _userEmail.value = email
    }

    fun updateLastSyncTime(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
        _lastSyncTime.value = timestamp
    }

    companion object {
        private const val KEY_IS_CONNECTED = "is_connected"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"

        @Volatile
        private var INSTANCE: GoogleAuthManager? = null

        fun getInstance(context: Context): GoogleAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

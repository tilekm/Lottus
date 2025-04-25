// ./app/src/main/java/kz/tilek/lottus/data/TokenManager.kt
package kz.tilek.lottus.data

import android.content.Context
import android.content.SharedPreferences
import kz.tilek.lottus.App // Убедись, что App.kt создан и зарегистрирован

object TokenManager {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_TOKEN = "auth_token" // Переименовал для ясности
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_USERNAME = "user_username" // Добавим username
    private const val KEY_IS_LOGGED_IN = "is_logged_in" // Переименовал для ясности

    // Получаем SharedPreferences через Application Context
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // --- Сохранение ---

    fun saveAuthToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun saveUserDetails(context: Context, userId: String, email: String, username: String) {
        getPrefs(context).edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_USERNAME, username)
            apply()
        }
    }

    fun setLoggedInStatus(context: Context, isLoggedIn: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    // --- Получение ---

    fun getAuthToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }

    fun getUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ID, null)
    }

    fun getUserEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_EMAIL, null)
    }

    fun getUsername(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_USERNAME, null)
    }

    fun isLoggedIn(context: Context): Boolean {
        // Пользователь считается вошедшим, если есть токен и статус установлен
        return getAuthToken(context) != null && getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // --- Очистка ---

    fun clearUserData(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    // --- Удобные методы для использования с App.appContext ---
    // (Опционально, но может упростить вызовы из других мест)

    val token: String?
        get() = getAuthToken(App.appContext)

    val userId: String?
        get() = getUserId(App.appContext)

    val email: String?
        get() = getUserEmail(App.appContext)

    val username: String?
        get() = getUsername(App.appContext)

    val isUserLoggedIn: Boolean
        get() = isLoggedIn(App.appContext)

    fun clear() {
        clearUserData(App.appContext)
    }
}
